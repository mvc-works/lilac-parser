
(ns lilac-parser.core
  (:require-macros [lilac-parser.core])
  (:require [clojure.string :as string] [cirru-edn.core :as cirru-edn]))

(declare parse-some)

(declare parse-many)

(declare parse-component)

(declare parse-optional)

(declare parse-lilac)

(declare parse-or)

(declare parse-combine)

(defn combine+ [xs] {:parser-node :combine, :items xs})

(defn indent+ [] {:parser-node :indent})

(defn is+ [x] {:parser-node :is, :item x})

(defn many+ [item] {:parser-node :many, :item item})

(defn one-of+ [xs] {:parser-node :one-of, :items xs})

(defn optional+ [x] {:parser-node :optional, :item x})

(defn or+ [xs] {:parser-node :or, :items xs})

(defn seq-strip-beginning [xs ys]
  (cond
    (empty? ys) {:ok? true, :rest xs}
    (empty? xs) {:ok? false, :rest nil, :reason {:message "xs ends", :ys ys}}
    (= (first xs) (first ys)) (recur (rest xs) (rest ys))
    :else {:ok? false, :message "not matching", :xs xs, :ys ys}))

(defn parse-is [xs rule]
  (let [item (:item rule), strip-result (seq-strip-beginning xs (string/split item ""))]
    (if (:ok? strip-result)
      (merge strip-result {:node :is, :value item, :result strip-result})
      (assoc strip-result :node :is))))

(defn parse-one-of [xs rule]
  (let [items (:items rule)]
    (if (contains? items (first xs))
      {:ok? true, :value (first xs), :rest (rest xs)}
      {:ok? false, :message "not in list", :value (first xs)})))

(defn parse-some [xs0 rule]
  (let [item (:item rule)]
    (loop [acc [], xs xs0]
      (let [result (parse-lilac xs item)]
        (if (:ok? result)
          (recur (conj acc result) (:rest result))
          {:ok? true, :node :some, :value (map :value acc), :rest xs})))))

(defn parse-or [xs rule]
  (let [items (:items rule)]
    (loop [rules items, failures []]
      (if (empty rules)
        {:ok? false, :message "No more rules to try", :failures failures}
        (let [result (parse-lilac xs (first rules))]
          (if (:ok? result)
            (assoc result :node :or)
            (recur (rest rules) (conj failures result))))))))

(defn parse-optional [xs rule]
  (let [item (:item rule), result (parse-lilac xs item)]
    (if (:ok? result)
      (assoc result :node :optional)
      {:ok? true, :node :optional, :value nil, :result result})))

(defn parse-many [xs0 rule]
  (let [item (:item rule)]
    (loop [acc [], xs xs0]
      (let [result (parse-lilac xs item)]
        (if (:ok? result)
          (recur (conj acc result) (:rest result))
          (if (empty? acc)
            {:ok? false, :message "zero match", :attempt result}
            {:ok? true, :node :many, :value (map :value acc), :rest xs}))))))

(defn parse-lilac [xs rule]
  (assert (sequential? xs) "expected to parse from a sequence")
  (case (:parser-node rule)
    :is (parse-is xs rule)
    :or (parse-or xs rule)
    :many (parse-many xs rule)
    :some (parse-some xs rule)
    :optional (parse-optional xs rule)
    :component (parse-component xs rule)
    :combine (parse-combine xs rule)
    :one-of (parse-one-of xs rule)
    (do (println "Unknown node" rule) nil)))

(defn parse-component [xs rule]
  (let [rule-name (:name rule)
        item (apply (:fn rule) (:args rule))
        result (parse-lilac xs item)
        value-fn (:value-fn rule)]
    (if (:ok? result)
      {:ok? true, :node rule-name, :value (value-fn (:value result)), :result result}
      {:ok? false, :node rule-name, :message "failed to parse component", :result result})))

(defn parse-combine [xs0 rule]
  (let [items (:items rule)]
    (loop [acc [], xs xs0, ys items]
      (if (empty? xs)
        {:ok? true, :value (map :value acc), :results acc}
        (let [result (parse-lilac xs (first ys))]
          (if (:ok? result)
            (recur (conj acc result) (:rest result) (rest ys))
            {:ok? false, :result result}))))))

(defn some+ [x] {:parser-node :some, :item x})

(defn unindent+ [] {:parser-node :unindent})
