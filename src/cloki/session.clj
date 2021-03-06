(in-ns 'cloki)

(defprotocol Session
  "An interaction with a Mediawiki instance"

  (logout [session] "Logs out of the mediawiki session, erasing login cookie and edit token.")

  (get-page [session title] "Returns a cloki/PageData record the satisifies the cloki/Page protocol.
    Use to create/update/delete the page with the given title.")

  (request [session params method] "Executes HTTP request to mediawiki.

    Returns zip of xml response.  Updates session state as required. Throws
    java.io.IOException if errors are found in the response.  Response format
    defaults to 'xml'; use the :format parameter to specify a different format.
     
    This function is provided for Mediawiki API functionality that cloki has not (yet) 
    wrapped and the caller needs to make a lower level request using the current session."))

(defn- query [session params] (request session (merge params {"action" "query"}) "get"))

(defn- post [session params] (request session (merge {"bot" "true"} params) "post"))

(defn- edit-token [session]
  (let [state (:state session)]
    (when-not (:edit-token @state)
      (let [xml (query session {"prop" "info", "intoken" "edit", "titles" "dummy page"})
            edit-token (xml1-> xml :query :pages :page (attr :edittoken))]
        (dosync
         (alter state assoc :edit-token edit-token)))))
  (:edit-token @(:state session)))

(defrecord SessionState [cookies edit-token])

(defrecord SessionData [url state]
  Session

  (request [session params method]
           (let [params (stringify-keys (merge {"format" "xml"} params))
                 state (:state session)
                 resp (http/request (:url session) method {} (:cookies @state) params)
                 cookies (:cookies resp)
                 xml (zip-xml resp)]
             (check-response method (:url session) params xml)
             (if (not (nil? cookies))
               (dosync
                (alter state assoc :cookies cookies)))
             xml))

  (logout [session]
          (post session {"action" "logout"})
          (let [state (:state session)]
            (dosync
             (alter state assoc :cookies {})
             (alter state assoc :edit-token nil))))

  (get-page [session title]
            (PageData. session title)))

(defn login [url user password & domain]
  "Logins into wiki, returning a cloki/SessionData record satisfying the Wiki Session protocol. 
   Throws java.io.IOException exception if login fails."
  (let [session (SessionData. url (ref (SessionState. {} nil)))
        params {"lgname" user, "lgpassword" password, "lgdomain" domain, "action" "login"}
        xml (post session params)
        result (xml1-> xml :login (attr :result))]
                                        ; Newer versions of the mediawiki API have a two step login process: first
                                        ; you issue a login request without a token, it returns 'NeedToken' and a
                                        ; login token, and then you reissue the loging request with the token.
                                        ; However older versions had a single step login process that did not
                                        ; require you to obtain a token.  If the first request returns 'Success'
                                        ; we're using an older version, and we're done.
    (if (= "NeedToken" result)
      (let [token (xml1-> xml :login (attr :token))
            params (merge params {"lgtoken" token})
            xml (post session params)
            result (xml1-> xml :login (attr :result))]
        (when-not (= "Success" result)
          (throw-error "login" url params (str "Expected result 'Success', got '" result "'")))
        session)
      (throw-error "login" url params (str "Expected result 'NeedToken', got '" result "'")))))

(defmacro with-login [bindings & body]
  "Calls logut on session binding in finally clause."
  `(let ~(subvec bindings 0 2)
     (try
       (with-open ~(subvec bindings 2) ~@body)
       (finally
        (. ~(bindings 0) logout)))))

