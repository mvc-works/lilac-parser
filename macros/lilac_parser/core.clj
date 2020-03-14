
(ns lilac-parser.core)

(defmacro defparser [comp-name args value-fn body]
 `(defn ~comp-name [~@args] {
    :parser-node :component
    :name (keyword '~comp-name)
    :value-fn value-fn
    :args [~@args]
    :fn (fn [~@args] ~body)
    }))

