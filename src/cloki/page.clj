(in-ns 'cloki)

(defprotocol Page
  "Functions for reading, creating, updating and deleting a page."

  (content [page] "Returns the content of this page as a string.")

  (put [page params] "Updates the content for this page.

    The :text parameter specifies the content of the page.  Boolean options
    include :bot (defaults to true), :summary and :minor (for a minor edit).")
  
  (delete [page] "Deletes this page."))

(declare post)
(declare query)
(declare edit-token)

(defn- get-content [page]
  (let [xml (query (:session page) {"prop" "revisions", "rvprop", "content", "titles" (:title page)})]
    (first (:content (first (xml1-> xml :query :pages :page :revisions :rev))))))

(def get-content (memoize get-content))

(defrecord PageData [session title]
  Page

  (content [page] (get-content page))
  
  (put [page params]
       (let [session (:session page)
             token (edit-token session)]
         (post session (merge params {"action" "edit", "token" token, "title" (:title page)}))))

  (delete [page]
          (if (get-content page)
            (let [session (:session page)
                  token (edit-token session)]
              (post session {"action" "delete", "token" token, "title" (:title page)})
              ))))

