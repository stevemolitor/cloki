(in-ns 'cloki)

(defprotocol Session
  "An interaction with a Mediawiki instance"

  (request [session params method] "Executes HTTP request to mediawiki.

Returns zip of xml response.  Updates session state as required. Throws
java.io.IOException if errors are found in the response.  Response format
defaults to 'xml'; use the :format parameter to specify a different format.")

  (query [session params] "Executes query action to mediawiki.")
  
  (post [session params] "Executes HTTP POST request.  See request.")
  
  (logout [session] "Logs out of the mediawiki session, erasing login cookie and edit token.")

  (get-page [session title] "Returns a cloki/PageData record the satisifies the cloki/Page protocol.

Use to create/update/delete the page with the given title.")
  
  (edit-token [session] "Lazily retrieves and stores edit token from wiki.

The edit token is used internally by cloki to create and save pages.  Curiously
the mediawiki API only requires one edit token per session that can be reused
multiple times to edit any page.")
    
    )

(defrecord SessionState [cookies edit-token])

(defrecord SessionData [url state]
  Session

  (request [session params method]
           (let [params (stringify-keys (merge {"format" "xml"} params))
                 state (:state session)
                 resp (http/request (:url session) method {} (:cookies @state) params)
                 xml (zip-xml resp)]
             (check-response method (:url session) params xml)
             (dosync
              (alter state assoc :cookies (:cookies resp)))
             xml))

  (query [session params] (request session (merge params {"action" "query"}) "get"))

  (post [session params] (request session (merge {"bot" "true"} params) "post"))

  (logout [session]
          (post session {"action" "logout"})
          (let [state (:state session)]
            (dosync
             (alter state assoc :cookies {})
             (alter state assoc :edit-token nil))))

  (get-page [session title]
            (PageData. session title))

  (edit-token [session]
   (let [state (:state session)]
     (when-not (:edit-token @state)
       (let [xml (query session {"prop" "info", "intoken" "edit", "titles" "dummy page"})
             edit-token (xml1-> xml :query :pages :page (attr :edittoken))]
         (dosync
          (alter state assoc :edit-token edit-token)))))
   (:edit-token @(:state session)))
  
  )

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
  (cond
   (= (count bindings) 0) `(do ~@body)
   (symbol? (bindings 0)) `(let ~(subvec bindings 0 2)
                             (try
                               (with-open ~(subvec bindings 2) ~@body)
                               (finally
                                (. ~(bindings 0) logout))))
   :else (throw (IllegalArgumentException.
                 "with-login only allows Symbols in bindings"))))
