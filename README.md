## Lilac parser

> A toy combinator parser with better failure reasons.

Online demo http://repo.mvc-works.org/lilac-parser/

Try with `(def a (add 1 2))` or `{"json": [1, 2]}`.

### Usage

[![Clojars Project](https://img.shields.io/clojars/v/mvc-works/lilac-parser.svg)](https://clojars.org/mvc-works/lilac-parser)

```edn
[mvc-works/lilac-parser "0.0.2-a3"]
```

```clojure
(require '[lilac-parser.core :refer
            [parse-lilac defparser is+ many+ one-of+ other-than+
             some+ combine+ interleave+ label+]])

(parse-lilac "aaaa" (many+ (is+ "a")))
```

Demo of a stupid S-expression parser:

```clojure
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

(parse-lilac (string/split "(def a (add 1 2))" "") (s-expr-parser+))
```

[More demos](https://github.com/mvc-works/lilac-parser/tree/master/src/lilac_parser/demo).

### Rules

| Rule          | Example                                         | Description                                |
| ------------- | ----------------------------------------------- | ------------------------------------------ |
| `is+`         | `(is+ "a")` or `(is+ "abc")`                    | matches a piece of string                  |
| `one-of+`     | `(one-of+ "abc")` or `(one-of+ #{"a" "b" "c"})` | matches a character in one of candidates   |
| `other-than+` | `(other-than+ "abc")`                           | matches a character that is not listed     |
| `optional+`   | `(optional+ (is+ "a"))`                         | matching or nothing                        |
| `some+`       | `(some+ (is+ "a"))`                             | matches 0 or more items                    |
| `many+`       | `(many+ (is+ "a"))`                             | matches 1 or more items                    |
| `or+`         | `(or+ [(is+ "a") (is+ "b")])`                   | matches one among listed items             |
| `combine+`    | `(combine+ [(is+ "a") (is+ "b")])`              | matches items in ecxact order              |
| `interleave+` | `(interleave+ (is+ "a") (is+ ","))`             | matches two interleaving items             |
| `label+`      | `(label+ "just a" (is+ "a"))`                   | simpler rule for adding comments in result |

### `defparser`

`defparser` is a macro for defining parser that can be used recursively. The type is `:component`, which is like a more complicated version of `:label`. Notice that `s-expr-parser+` defined with `defparser` is different from a normal rule, it's a function so it need to be called before being used as a rule.

### Visual DEMO

lilac-parser would be pretter slow since it tries to store all information during parsing, which results in a piece of EDN data. The result can be rendered into a tree with GUI and that's what is [demonstrated in the demo](http://repo.mvc-works.org/lilac-parser/).

<details>
<summary>An example for EDN data in parsing a JSON number.</summary>
<pre><code>
{
  :ok? true, :value 112, :parser-node :component, :label :value-parser+
  :rest ("," "1")
  :result {
    :ok? true, :value 112, :parser-node :or
    :rest ("," "1")
    :result {
      :ok? true, :parser-node :label, :label "number", :value 112
      :rest ("," "1")
      :result {
        :ok? true, :value 112, :parser-node :combine
        :rest ("," "1")
        :results [
          {
            :ok? true, :value nil, :parser-node :optional
            :result {
              :ok? false, :message "expects \"-\" but got \"1\"", :parser-node :is
              :rest ["1" "1" "2" "," "1"]
            }
            :rest ["1" "1" "2" "," "1"]
          }
          {
            :ok? true, :parser-node :many
            :value ("1" "1" "2")
            :rest ("," "1")
            :results [
              {
                :ok? true, :value "1", :parser-node :one-of
                :rest ("1" "2" "," "1")
              }
              {
                :ok? true, :value "1", :parser-node :one-of
                :rest ("2" "," "1")
              }
              {
                :ok? true, :value "2", :parser-node :one-of
                :rest ("," "1")
              }
            ]
            :peek-result {
              :ok? false, :message "\",\" is not in \"1234567890\"", :parser-node :one-of
              :rest ("," "1")
            }
          }
          {
            :ok? true, :value nil, :parser-node :optional
            :result {
              :ok? false, :parser-node :combine, :message "failed to combine"
              :result {
                :ok? false, :message "expects \".\" but got \",\"", :parser-node :is
                :rest ("," "1")
              }
              :previous-results []
              :rest ("," "1")
            }
            :rest ("," "1")
          }
        ]
      }
    }
  }
}
</code></pre>
</details>

### Custom Rule

Parser rules can be expected by injecting functions. It could be quite tricky and is not recommended:

```clojure
(lilac-parser.core/resigter-custom-rule! :xyz
  (fn [xs rule]
    ; TODO
    ))

(defn xyz+ [xs transform]
  ; TODO
  )
```

### Workflow

Workflow https://github.com/mvc-works/calcit-workflow

### License

MIT
