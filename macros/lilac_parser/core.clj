
(ns lilac-parser.core)

(defmacro defparser [comp-name args value-fn body]
 `(defn ~comp-name [~@args] {
    :parser-node :component
    :name (keyword '~comp-name)
    :blackbox? false
    :value-fn ~value-fn
    :args [~@args]
    :fn (fn [~@args] ~body)
    }))

(defmacro defparser- [comp-name args value-fn body]
 `(defn ~comp-name [~@args] {
    :parser-node :component
    :name (keyword '~comp-name)
    :blackbox? true
    :value-fn ~value-fn
    :args [~@args]
    :fn (fn [~@args] ~body)
    }))

