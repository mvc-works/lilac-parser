
(ns lilac-parser.util )

(defn seq-strip-beginning [xs ys]
  (cond
    (empty? ys) {:ok? true, :rest xs}
    (empty? xs) {:ok? false, :rest nil, :reason {:message "xs ends", :ys ys}}
    (= (first xs) (first ys)) (recur (rest xs) (rest ys))
    :else {:ok? false, :message "not matching", :xs xs, :ys ys}))
