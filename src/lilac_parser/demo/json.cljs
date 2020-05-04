
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
              defparser]]))

(declare value-parser+)

(declare array-parser+)

(declare object-parser+)

(def boolean-parser (or+ [(is+ "true") (is+ "false")]))

(def space-parser (some+ (is+ " ")))

(def comma-parser (combine+ [space-parser (is+ ",") space-parser]))

(def digit-parser (one-of+ "1234567890"))

(def nil-parser (or+ [(is+ "null") (is+ "undefined")]))

(def number-parser
  (combine+
   [(optional+ (is+ "-"))
    (many+ digit-parser)
    (optional+ (combine+ [(is+ ".") (many+ digit-parser)]))]))

(def string-parser
  (combine+
   [(is+ "\"")
    (some+ (or+ [(other-than+ "\"\\") (is+ "\\\"") (is+ "\\\\") (is+ "\\n")]))
    (is+ "\"")]))

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
   (some+
    (interleave+
     (combine+ [string-parser space-parser (is+ ":") space-parser (value-parser+)])
     comma-parser))
   (is+ "}")]))

(defparser
 array-parser+
 ()
 identity
 (combine+ [(is+ "[") (some+ (interleave+ (value-parser+) comma-parser)) (is+ "]")]))

(def demo-parser (many+ (other-than+ "abc")))
