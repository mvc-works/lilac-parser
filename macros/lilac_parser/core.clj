
(ns lilac-parser.core)

(defmacro deflilac [comp-name args body]
 `(defn ~comp-name [~@args] {
    :parser-node :component
    :name (keyword '~comp-name)
    :args [~@args]
    :fn (fn [~@args] ~body)
    }))

