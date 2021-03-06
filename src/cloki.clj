(ns cloki
  (:use
   clojure.contrib.zip-filter.xml
   [clojure.xml :as xml :only (parse)]
   clojure.contrib.logging
   [clojure.contrib.str-utils :only (str-join)]
   [clojure.walk :only (stringify-keys)])
  (require
   [clojure.zip :as zip]
   [clojure-http.client :as http]))

(load "cloki/util")
(load "cloki/page")
(load "cloki/session")
