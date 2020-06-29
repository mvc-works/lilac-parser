
(ns lilac-parser.core
  (:require-macros [lilac-parser.core])
  (:require [clojure.string :as string]
            [cirru-edn.core :as cirru-edn]
            [lilac-parser.config :refer [dev?]]
            [lilac-parser.util :refer [seq-strip-beginning]]))

(declare parse-label)

(declare core-methods)

(declare parse-interleave)

(declare parse-some)

(declare parse-many)

(declare parse-component)

(declare parse-optional)

(declare parse-lilac)

(declare parse-or)

(declare parse-combine)

(defonce *custom-methods (atom {}))

(defn combine+
  ([xs] (combine+ xs identity))
  ([xs transform] {:parser-node :combine, :items xs, :transform transform}))

(defn parse-is [xs rule]
  (if (empty? xs)
    {:ok? false,
     :message (str "expects " (pr-str (:item rule)) " but got EOF"),
     :parser-node :is,
     :rest xs}
    (let [item (:item rule)
          transform (:transform rule)
          strip-result (seq-strip-beginning xs (string/split item ""))]
      (if (:ok? strip-result)
        {:ok? true,
         :value (if (some? transform) (transform item) item),
         :rest (:rest strip-result),
         :parser-node :is}
        {:ok? false,
         :message (str
                   "expects "
                   (pr-str item)
                   " but got "
                   (pr-str (string/join "" (take (count item) xs)))),
         :parser-node :is,
         :rest xs}))))

(defn parse-one-of [xs rule]
  (if (empty? xs)
    {:ok? false, :message "unexpected EOF", :parser-node :one-of, :rest xs}
    (let [items (:items rule), transform (:transform rule)]
      (if (if (string? items)
        (string/includes? items (first xs))
        (contains? items (first xs)))
        {:ok? true,
         :value (let [v (first xs)] (if (some? transform) (transform v) v)),
         :rest (rest xs),
         :parser-node :one-of}
        {:ok? false,
         :message (str
                   (pr-str (first xs))
                   " is not in "
                   (pr-str (if (string? items) items (string/join "" items)))),
         :parser-node :one-of,
         :rest xs}))))

(defn parse-other-than [xs rule]
  (if (empty? xs)
    {:ok? false, :message "Unexpected EOF", :parser-node :other-than, :rest xs}
    (let [items (:items rule), transform (:transform rule), x0 (first xs)]
      (if (if (string? items) (string/includes? items x0) (contains? items x0))
        {:ok? false,
         :message (str
                   (pr-str x0)
                   " among "
                   (pr-str (if (string? items) items (string/join "" items)))
                   " is invalid"),
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
         :message (str "all " (count items) " rules missed"),
         :parser-node :or,
         :results failures,
         :rest xs}
        (let [result (parse-lilac xs (first rules))]
          (if (:ok? result)
            {:ok? true,
             :value (let [v (:value result)] (if (some? transform) (transform v) v)),
             :rest (:rest result),
             :parser-node :or,
             :results failures,
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
             :message "no match",
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
  (let [node (:parser-node rule)
        method (get core-methods node)
        user-method (get @*custom-methods node)]
    (cond
      (fn? method) (method xs rule)
      (fn? user-method) (user-method xs rule)
      :else (do (js/console.warn "Unknown node" rule) nil))))

(defn parse-label [xs rule]
  (let [result (parse-lilac xs (:item rule))]
    (if (:ok? result)
      {:ok? true,
       :parser-node :label,
       :label (:label rule),
       :value (:value result),
       :rest (:rest result),
       :result result}
      {:ok? false,
       :message nil,
       :parser-node :label,
       :label (:label rule),
       :result result,
       :rest (:rest result)})))

(defn parse-interleave [xs0 rule]
  (let [x0 (:x rule), y0 (:y rule), transform (:transform rule)]
    (loop [acc [], xs xs0, x x0, y y0]
      (let [result (parse-lilac xs x)]
        (if (:ok? result)
          (recur (conj acc result) (:rest result) y x)
          (if (empty? acc)
            {:ok? false,
             :message "no match",
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
       :parser-node :component,
       :label rule-name,
       :result (if blackbox? nil result)}
      {:ok? false,
       :message "failed branch",
       :parser-node :component,
       :label rule-name,
       :result (if blackbox? nil result),
       :rest xs})))

(defn parse-combine [xs0 rule]
  (let [items (:items rule), transform (:transform rule)]
    (loop [acc [], xs xs0, ys items]
      (cond
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
               :message "failed to combine",
               :result result,
               :previous-results acc,
               :rest xs}))))))

(def core-methods
  {:is parse-is,
   :or parse-or,
   :many parse-many,
   :some parse-some,
   :optional parse-optional,
   :component parse-component,
   :combine parse-combine,
   :one-of parse-one-of,
   :interleave parse-interleave,
   :other-than parse-other-than,
   :label parse-label})

(defn indent+
  ([] (indent+ identity))
  ([transform] {:parser-node :indent, :transform transform}))

(defn interleave+
  ([x y] (interleave+ x y identity))
  ([x y transform] {:parser-node :interleave, :x x, :y y, :transform transform}))

(defn is+
  ([x] (is+ x identity))
  ([x transform] {:parser-node :is, :item x, :transform transform}))

(defn label+ [label item] {:parser-node :label, :label label, :item item})

(defn many+
  ([item] (many+ item nil))
  ([item transform] {:parser-node :many, :item item, :transform transform}))

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

(defn replace-lilac
  ([content rule replacer] (replace-lilac "" [] content rule replacer))
  ([acc attempts content rule replacer]
   (assert (sequential? content) "expects content in sequence")
   (if (empty? content)
     {:result acc, :attempts attempts}
     (let [attempt (parse-lilac content rule)]
       (if (:ok? attempt)
         (recur
          (str acc (replacer (:value attempt)))
          (conj attempts attempt)
          (:rest attempt)
          rule
          replacer)
         (recur
          (str acc (first content))
          (conj attempts attempt)
          (rest content)
          rule
          replacer))))))

(defn resigter-custom-rule! [kind f]
  (assert (keyword? kind) "expects kind in keyword")
  (assert (fn? f) "expects parser rule in function")
  (println "registering parser rule" kind)
  (swap! *custom-methods assoc kind f))

(defn some+
  ([x] (some+ x identity))
  ([x transform] {:parser-node :some, :item x, :transform transform}))

(defn unindent+ [] {:parser-node :unindent})
