
(ns lilac-parser.comp.container
  (:require [hsl.core :refer [hsl]]
            [respo-ui.core :as ui]
            [respo.core
             :refer
             [defcomp defeffect >> list-> <> div button textarea span input]]
            [respo.comp.space :refer [=<]]
            [reel.comp.reel :refer [comp-reel]]
            [respo-md.comp.md :refer [comp-md]]
            [lilac-parser.config :refer [dev?]]
            [lilac-parser.core
             :refer
             [parse-lilac defparser is+ combine+ some+ many+ optional+ or+ one-of+ some+]]
            ["@mvc-works/codearea" :refer [codearea]]
            [clojure.string :as string]
            [cirru-edn.core :as cirru-edn]
            [feather.core :refer [comp-icon]]
            [lilac-parser.demo.s-expr :refer [s-expr-parser+]]
            [lilac-parser.demo.json
             :refer
             [demo-parser number-parser string-parser array-parser+ value-parser+]]))

(def style-label
  {:font-family ui/font-code,
   :color (hsl 0 0 100),
   :display :inline-block,
   :line-height "24px",
   :padding "0 4px",
   :border-radius "4px",
   :margin-right 8,
   :white-space :pre,
   :min-height 14})

(defcomp
 comp-node
 (states node)
 (let [cursor (:cursor states)
       state (or (:data states) {:folded? false})
       has-children? (or (some? (:result node))
                         (some? (:peek-result node))
                         (not (empty? (:results node))))]
   (div
    {:style (merge
             ui/expand
             {:padding 4,
              :border-left (str "1px solid " (hsl 0 0 90)),
              :border-top (str "1px solid " (hsl 0 0 90))})}
    (div
     {:style ui/row-middle}
     (if has-children?
       (comp-icon
        (if (:folded? state) :play :chevron-down)
        {:font-size 14,
         :color (if (:folded? state) (hsl 200 80 40) (hsl 200 80 80)),
         :margin 8,
         :cursor :pointer}
        (fn [e d!] (d! cursor (update state :folded? not))))
       (comp-icon
        :minus
        {:font-size 14, :color (hsl 200 80 90), :margin 8, :cursor :pointer}
        (fn [e d!] )))
     (if (:ok? node)
       (<>
        "OK"
        (merge style-label {:background-color (hsl 200 80 70), :font-family ui/font-fancy}))
       (<>
        "Failure"
        (merge style-label {:background-color (hsl 60 80 40), :font-family ui/font-fancy})))
     (if-not (:ok? node)
       (<>
        (:message node)
        (merge style-label {:background-color (hsl 0 80 60), :font-family ui/font-normal})))
     (<> (:parser-node node) (merge style-label {:background-color (hsl 200 80 70)}))
     (if (= :is (:parser-node node))
       (<> (:value node) (merge style-label {:background-color (hsl 200 80 70)})))
     (if (:ok? node)
       (<>
        (pr-str (:value node))
        (merge style-label {:background-color (hsl 200 80 80), :font-size 10})))
     (<>
      (->> (:rest node) (take 10) (string/join ""))
      (merge style-label {:background-color (hsl 100 10 60), :font-size 10, :min-height 16})))
    (if (and has-children? (not (:folded? state)))
      (div
       {}
       (list->
        {:style {:padding-left 16, :margin-top 8}}
        (->> (or (:results node) (:previous-results node))
             (map-indexed (fn [idx child] [idx (comp-node (>> states idx) child)]))))
       (if (some? (:result node))
         (div
          {:style {:padding-left 16, :margin-top 8}}
          (comp-node (>> states :result) (:result node))))
       (if (some? (:peek-result node))
         (div
          {:style {:padding-left 16, :margin-top 8}}
          (comp-node (>> states :peek-result) (:peek-result node)))))))))

(defcomp
 comp-container
 (reel)
 (let [store (:store reel)
       states (:states store)
       cursor []
       state (or (:data states) {:code "(def a (add 1 2))", :result nil, :gui? false})]
   (div
    {:style (merge ui/global ui/fullscreen ui/column)}
    (div
     {:style (merge ui/row-middle {:padding 8})}
     (button
      {:style ui/button,
       :inner-text "Parse",
       :on-click (fn [e d!]
         (let [result (parse-lilac (string/split (:code state) "") (s-expr-parser+))
               r1 (parse-lilac (string/split (:code state) "") (value-parser+))]
           (d! cursor (assoc state :result r1))))})
     (=< 16 nil)
     (span
      {:inner-text "GUI",
       :style {:font-family ui/font-fancy,
               :color (if (:gui? state) (hsl 200 80 40) (hsl 200 80 80)),
               :font-weight 300,
               :font-size 20,
               :cursor :pointer,
               :line-height "24px"},
       :on-click (fn [e d!] (d! cursor (update state :gui? not)))}))
    (div
     {:style (merge ui/expand ui/row)}
     (textarea
      {:value (:code state),
       :class-name "codearea",
       :placeholder "Content",
       :style (merge ui/textarea {:font-family ui/font-code, :width 300}),
       :on-input (fn [e d!] (d! cursor (assoc state :code (:value e))))})
     (if (:gui? state)
       (div
        {:style (merge ui/expand {:padding-bottom 600})}
        (comp-node (>> states :tree-viewer) (:result state)))
       (textarea
        {:style (merge
                 ui/expand
                 ui/textarea
                 {:font-family ui/font-code, :font-size 12, :white-space :nowrap}),
         :spellcheck false,
         :value (cirru-edn/write (:result state))})))
    (when dev? (comp-reel (>> states :reel) reel {})))))

(defeffect
 effect-codearea
 ()
 (action el)
 (when (= action :mount) (let [target (.querySelector el ".codearea")] (codearea target))))
