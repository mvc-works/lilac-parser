
(ns lilac-parser.demo.s-expr
  (:require [lilac-parser.core
             :refer
             [parse-lilac defparser is+ combine+ some+ many+ optional+ or+ one-of+ some+]]
            [clojure.string :as string]))

(def number-parser (many+ (one-of+ "1234567890")))

(def space-parser (is+ " "))

(def word-parser (many+ (one-of+ "qwertyuiopasdfghjklzxcvbnm")))

(defparser
 s-expr-parser+
 ()
 identity
 (combine+
  [(is+ "(")
   (some+ (or+ [number-parser word-parser space-parser (s-expr-parser+)]))
   (is+ ")")]))
