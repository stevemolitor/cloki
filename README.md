# Cloki

Clojure library for access mediawiki installations.  Cloki is a wrapper around the Mediawiki HTTP API (see http://www.mediawiki.org/wiki/API). At this point it's a very thin wrapper!

## Synopsis

Log on, create / update content of a page, and ensure log out:

    (use 'cloki)

    (with-login [session (login test-wiki test-user test-password)]
      (let [page (get-page session "My Page")]
        (put page {:text "page content"})))

Print the contents of a page:

    (use 'cloki)

    (with-login [session (login test-wiki test-user test-password)]
      (let [page (get-page session "My Page")]
        (println (get-content page))))

## Problems / Bugs
Deleting pages is returning a "permission denied" error from Mediawiki.  
      
## License

Copyright (c) Stephen Molitor and released under an MIT license.
