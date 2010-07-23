(defproject cloki "0.0.1"
  :description "Clojure wrapper around Mediawiki API"
  :dependencies [
                 [org.clojure/clojure "1.2.0-beta1"]
                 [org.clojure/clojure-contrib "1.2.0-beta1"]
;                 [org.clojure/clojure "1.2.0-master-SNAPSHOT"]
                 [clojure-http-client "1.0.1"]]
  :dev-dependencies [[org.clojars.gjahad/debug-repl "0.3.0-SNAPSHOT"]
                     [swank-clojure "1.2.0"]
                     [leiningen/lein-swank "1.1.0"]]
  :repositories {"clojure-releases" "http://build.clojure.org/releases"})
