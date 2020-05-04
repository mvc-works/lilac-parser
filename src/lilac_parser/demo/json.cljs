
(ns lilac-parser.demo.json (:require [lilac-parser.core :refer [interleave+ is+]]))

(def demo-parser (interleave+ (is+ "aaa") (is+ "b")))

(defn number-parser [] )

(defn string-parser [] )
