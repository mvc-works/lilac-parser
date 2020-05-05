
Lilac parser
----

> A toy combinator parser with better failure reasons.

Demo of `(def a (add 1 2))` or `{"json": [1, 2]` http://repo.mvc-works.org/lilac-parser/

### Usage

[![Clojars Project](https://img.shields.io/clojars/v/mvc-works/lilac-parser.svg)](https://clojars.org/mvc-works/lilac-parser)

```edn
[mvc-works/lilac-parser "0.0.2-a2"]
```

```clojure
(require '[lilac-parser.core :refer
            [parse-lilac defparser is+ many+ one-of+ some+ combine+ interleave+ other-than+]])

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

`defparser` is a macro for defining parser that can be used recursively. Notice that `s-expr-parser+` is different from a normal `number-parser`, it's a function so it need to be called before passing as a rule.

### Workflow

Workflow https://github.com/mvc-works/calcit-workflow

### License

MIT
