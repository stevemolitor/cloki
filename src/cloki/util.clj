(in-ns 'cloki)

(defn- zip-xml [response]
  "Parses XML body from HTTP response, return zip of xml elements."
  (zip/xml-zip (xml/parse (java.io.ByteArrayInputStream. (.getBytes (first (concat (:body-seq response))))))))

(defn- throw-error [method url params msg]
  (let [error-text (str "Error executing " method
                        " request to " url
                        " with parameters " params
                        " - " msg)]
    (throw (java.io.IOException. error-text))))

(defn- check-for-errors [method url params xml]
  "Checks for errors in xml zip of mediawiki response body.  Raises exception if error(s) are found."
  (let [errors (xml-> xml :error)]
    (when-not (empty? errors)
      (println errors)
      (let [error-msgs (map #(str "error code: " (xml1-> % (attr :code))
                                  ",  description: " (xml1-> % (attr :info)))
                            errors)]
        (throw-error method url params (seq error-msgs))))))

(defn- check-for-warnings [method url params xml]
  "Logs any warnings in zip of xml response."
  (let [warnings (xml-> xml :warnings)]
    (for [w warnings]
      (warn (str "Warning in wiki response while executing " method
                 " request to " url
                 " with parameters " params
                 " - " w)))))

(defn- check-response [method url params xml]
  (check-for-errors method url params xml)
  (check-for-warnings method url params xml))


