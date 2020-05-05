
(ns lilac-parser.core
  (:require-macros [lilac-parser.core])
  (:require [clojure.string :as string]
            [cirru-edn.core :as cirru-edn]
            [lilac-parser.config :refer [dev?]]))

(declare parse-interleave)

(declare parse-some)

(declare parse-many)

(declare parse-component)

(declare parse-optional)

(declare parse-lilac)

(declare parse-or)

(declare parse-combine)

(defn combine+
  ([xs] (combine+ xs identity))
  ([xs transform] {:parser-node :combine, :items xs, :transform transform}))

(defn indent+
  ([] (indent+ identity))
  ([transform] {:parser-node :indent, :transform transform}))

(defn interleave+
  ([x y] (interleave+ x y identity))
  ([x y transform] {:parser-node :interleave, :x x, :y y, :transform transform}))

(defn is+
  ([x] (is+ x identity))
  ([x transform] {:parser-node :is, :item x, :transform transform}))

(defn many+ [item] {:parser-node :many, :item item})

(defn one-of+
  ([xs] (one-of+ xs identity))
  ([xs transform]
   (when (and dev? (not (or (string? xs) (set? xs))))
     (println "Unexpected argument passed to one-of+ :" xs))
   {:parser-node :one-of, :items xs, :transform transform}))

(defn optional+
  ([x] (optional+ x identity))
  ([x transform] {:parser-node :optional, :item x, :transform transform}))

(defn or+
  ([xs] (or+ xs identity))
  ([xs transform]
   (when (and dev? (not (sequential? xs))) (println "Expected argument passed to or+ :" xs))
   {:parser-node :or, :items xs, :transform transform}))

(defn other-than+
  ([items] (other-than+ items identity))
  ([items transform]
   (when (and dev? (not (or (string? items) (set? items))))
     (println "Unexpected parameter passed to other-than+ :" items))
   {:parser-node :other-than, :items items, :transform transform}))

(defn seq-strip-beginning [xs ys]
  (cond
    (empty? ys) {:ok? true, :rest xs}
    (empty? xs) {:ok? false, :rest nil, :reason {:message "xs ends", :ys ys}}
    (= (first xs) (first ys)) (recur (rest xs) (rest ys))
    :else {:ok? false, :message "not matching", :xs xs, :ys ys}))

(defn parse-is [xs rule]
  (let [item (:item rule)
        transform (:transform rule)
        strip-result (seq-strip-beginning xs (string/split item ""))]
    (if (:ok? strip-result)
      {:ok? true,
       :value (if (some? transform) (transform item) item),
       :rest (:rest strip-result),
       :parser-node :is}
      {:ok? false,
       :message (str "failed to match " item " in " (take 10 xs) "...."),
       :parser-node :is,
       :rest xs})))

(defn parse-one-of [xs rule]
  (let [items (:items rule), transform (:transform rule)]
    (if (if (string? items) (string/includes? items (first xs)) (contains? items (first xs)))
      {:ok? true,
       :value (let [v (first xs)] (if (some? transform) (transform v) v)),
       :rest (rest xs),
       :parser-node :one-of}
      {:ok? false, :message "not in list", :parser-node :one-of, :rest xs})))

(defn parse-other-than [xs rule]
  (if (empty? xs)
    {:ok? false,
     :message "Unexpected EOF in other-than+ rule",
     :parser-node :other-than,
     :rest xs}
    (let [items (:items rule), transform (:transform rule), x0 (first xs)]
      (if (if (string? items) (string/includes? items x0) (contains? items x0))
        {:ok? false,
         :message (str (pr-str x0) "is in not expected item in other-than+"),
         :parser-node :other-than,
         :rest xs}
        {:ok? true,
         :value (if (some? transform) (transform x0) x0),
         :rest (rest xs),
         :parser-node :other-than}))))

(defn parse-some [xs0 rule]
  (let [item (:item rule), transform (:transform rule)]
    (loop [acc [], xs xs0]
      (let [result (parse-lilac xs item)]
        (if (:ok? result)
          (recur (conj acc result) (:rest result))
          {:ok? true,
           :value (let [v (map :value acc)] (if (some? transform) (transform v) v)),
           :rest xs,
           :parser-node :some,
           :results acc,
           :peek-result result})))))

(defn parse-or [xs rule]
  (let [items (:items rule), transform (:transform rule)]
    (loop [rules items, failures []]
      (if (empty? rules)
        {:ok? false,
         :message "No more rules to try",
         :parser-node :or,
         :results failures,
         :rest xs}
        (let [result (parse-lilac xs (first rules))]
          (if (:ok? result)
            {:ok? true,
             :value (let [v (:value result)] (if (some? transform) (transform v) v)),
             :rest (:rest result),
             :parser-node :or,
             :result result}
            (recur (rest rules) (conj failures result))))))))

(defn parse-optional [xs rule]
  (let [item (:item rule), transform (:transform rule), result (parse-lilac xs item)]
    (if (:ok? result)
      {:ok? true,
       :value (let [v (:value result)] (if (some? transform) (transform v) v)),
       :rest (:rest result),
       :parser-node :optional,
       :result result}
      {:ok? true, :value nil, :result result, :parser-node :optional, :rest xs})))

(defn parse-many [xs0 rule]
  (let [item (:item rule), transform (:transform rule)]
    (loop [acc [], xs xs0]
      (let [result (parse-lilac xs item)]
        (if (:ok? result)
          (recur (conj acc result) (:rest result))
          (if (empty? acc)
            {:ok? false,
             :message "no match in many",
             :parser-node :many,
             :peek-result result,
             :rest xs}
            {:ok? true,
             :value (let [v (map :value acc)] (if (some? transform) (transform v) v)),
             :rest xs,
             :parser-node :many,
             :results acc,
             :peek-result result}))))))

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
    :interleave (parse-interleave xs rule)
    :other-than (parse-other-than xs rule)
    (do (js/console.warn "Unknown node" rule) nil)))

(defn parse-interleave [xs0 rule]
  (let [x0 (:x rule), y0 (:y rule), transform (:transform rule)]
    (loop [acc [], xs xs0, x x0, y y0]
      (let [result (parse-lilac xs x)]
        (if (:ok? result)
          (recur (conj acc result) (:rest result) y x)
          (if (empty? acc)
            {:ok? false,
             :message "no match in interleave",
             :parser-node :interleave,
             :peek-result result,
             :rest xs}
            {:ok? true,
             :value (let [v (map :value acc)] (if (some? transform) (transform v) v)),
             :rest xs,
             :parser-node :interleave,
             :results acc,
             :peek-result result}))))))

(defn parse-component [xs rule]
  (let [rule-name (:name rule)
        item (apply (:fn rule) (:args rule))
        result (parse-lilac xs item)
        value-fn (:value-fn rule)
        blackbox? (:blackbox? rule)]
    (if (:ok? result)
      {:ok? true,
       :value (value-fn (:value result)),
       :rest (:rest result),
       :parser-node rule-name,
       :result (if blackbox? nil result)}
      {:ok? false,
       :message "failed to parse component",
       :parser-node rule-name,
       :result (if blackbox? nil result),
       :rest xs})))

(defn parse-combine [xs0 rule]
  (let [items (:items rule), transform (:transform rule)]
    (loop [acc [], xs xs0, ys items]
      (cond
        (and (empty? xs) (not (empty? ys)))
          {:ok? false,
           :message "unexpected end of file",
           :parser-node :combine,
           :results acc,
           :rest xs}
        (empty? ys)
          {:ok? true,
           :value (let [v (map :value acc)] (if (some? transform) (transform v) v)),
           :rest xs,
           :parser-node :combine,
           :results acc}
        :else
          (let [result (parse-lilac xs (first ys))]
            (if (:ok? result)
              (recur (conj acc result) (:rest result) (rest ys))
              {:ok? false,
               :parser-node :combine,
               :message "not matched during combine",
               :result result,
               :previous-results acc,
               :rest xs}))))))

(defn some+
  ([x] (some+ x identity))
  ([x transform] {:parser-node :some, :item x, :transform transform}))

(defn unindent+ [] {:parser-node :unindent})
