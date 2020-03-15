
Lilac parser
----

> A toy combinator parser with better failure reasons.

Demo of `(def a (add 1 2))` http://repo.mvc-works.org/lilac-parser/

### Usage

_TODO_

```clojure
(require '[lilac-parser.core :refer [parse-lilac defparser is+ many+ one-of+ some+ combine+]])

(parse-lilac "aaaa" (many+ (is+ "a")))
```

Demo of a stupid S-expression parser:

```clojure
(def number-parser (many+ (one-of+ (set (string/split "1234567890" "")))))

(def space-parser (is+ " "))

(def word-parser (many+ (one-of+ (set (string/split "qwertyuiopasdfghjklzxcvbnm" "")))))

(defparser
 s-expr-parser+
 ()
 identity
 (combine+
  [(is+ "(")
   (some+ (or+ [number-parser word-parser space-parser (s-expr-parser+)]))
   (is+ ")")]))

(parse-lilac (string/split "(def a (add 1 2))" "") (s-expr-parser+))
```

### Workflow

Workflow https://github.com/mvc-works/calcit-workflow

### License

MIT
