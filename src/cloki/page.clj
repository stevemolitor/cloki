(in-ns 'cloki)

(defprotocol Page
  "Functions for reading, creating, updating and deleting a page."
  
  (get-content [page] "Remotely fetches the content for this page.  Returns nil if the page does not exist.

    The content is *not* lazy loaded or memoized - each call to get-content remotely
    fetches the latest content from the wiki.")
  
  (put [page params] "Updates the content for this page.

    The :text parameter specifies the content of the page.  Boolean options
    include :bot (defaults to true), :summary and :minor (for a minor edit).")
  
  (delete [page] "Deletes this page."))

(declare post)
(declare query)
(declare edit-token)

(defrecord PageData [session title]
  Page

  (get-content [page]
               (let [xml (query (:session page) {"prop" "revisions", "rvprop", "content", "titles" (:title page)})]
                 (first (:content (first (xml1-> xml :query :pages :page :revisions :rev))))))

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

