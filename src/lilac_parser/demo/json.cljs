
(ns lilac-parser.demo.json
  (:require [lilac-parser.core
             :refer
             [interleave+
              is+
              other-than+
              many+
              combine+
              optional+
              one-of+
              some+
              or+
              defparser]]
            [clojure.string :as string]))

(declare value-parser+)

(declare array-parser+)

(declare object-parser+)

(def boolean-parser
  (or+ [(is+ "true") (is+ "false")] (fn [x] (if (= x "true") true false))))

(def space-parser (some+ (is+ " ") (fn [x] nil)))

(def comma-parser (combine+ [space-parser (is+ ",") space-parser] (fn [x] nil)))

(def digit-parser (one-of+ "1234567890"))

(def nil-parser (or+ [(is+ "null") (is+ "undefined")] (fn [x] nil)))

(def number-parser
  (combine+
   [(optional+ (is+ "-"))
    (many+ digit-parser)
    (optional+ (combine+ [(is+ ".") (many+ digit-parser)]))]
   (fn [xs] (js/Number (string/join "" (nth xs 1))))))

(def string-parser
  (combine+
   [(is+ "\"")
    (some+ (or+ [(other-than+ "\"\\") (is+ "\\\"") (is+ "\\\\") (is+ "\\n")]))
    (is+ "\"")]
   (fn [xs] (string/join "" (nth xs 1)))))

(defparser
 value-parser+
 ()
 identity
 (or+
  [number-parser string-parser nil-parser boolean-parser (array-parser+) (object-parser+)]))

(defparser
 object-parser+
 ()
 identity
 (combine+
  [(is+ "{")
   (optional+
    (interleave+
     (combine+
      [string-parser space-parser (is+ ":") space-parser (value-parser+)]
      (fn [xs] [(nth xs 0) (nth xs 4)]))
     comma-parser
     (fn [xs] (take-nth 2 xs))))
   (is+ "}")]
  (fn [xs] (into {} (nth xs 1)))))

(defparser
 array-parser+
 ()
 (fn [x] (vec (first (nth x 1))))
 (combine+
  [(is+ "[")
   (some+ (interleave+ (value-parser+) comma-parser (fn [xs] (take-nth 2 xs))))
   (is+ "]")]))

(def demo-parser (many+ (other-than+ "abc")))
