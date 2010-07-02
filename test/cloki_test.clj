(ns cloki-test
  (:use clojure.test)
  (:use cloki)
 )

(def test-wiki "http://localhost/~steve/mediawiki/api.php")
(def test-user "Admin")
(def test-password "password")

(deftest test-login
  (let [session (login test-wiki test-user test-password)]
    (is (not (nil? (:cookies @(:state session)))))
    (is (= test-wiki (:url session)))))

(deftest test-logout
  (let [session (login test-wiki test-user test-password)
        state (:state session)]
    (is (not (empty? (:cookies @state))))
    (logout session)
    (is (empty? (:cookies @state)))
    (is (nil? (:edit-token @state)))))

(deftest test-with-login
  (with-login [session (login test-wiki test-user test-password)]
    (is (not (nil? (:cookies @(:state session)))))
    (is (= test-wiki (:url session)))))

(deftest test-pages
  (with-login [session (login test-wiki test-user test-password)]
    (let [page (get-page session "TestPage")]
      (put page {:text "page content"})
      (= "page content" (get-content page))
      (delete page)
      (is (nil? (get-content page)))
      )))

(deftest test-examples
  (with-login [session (login test-wiki test-user test-password)]
    (let [page (get-page session "My Page")]
      (put page {:text "page content"})
      ))

  (with-login [session (login test-wiki test-user test-password)]
    (let [page (get-page session "My Page")]
      (println (get-content page))))
  
  )
