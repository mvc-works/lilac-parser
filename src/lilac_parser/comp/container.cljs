
(ns lilac-parser.comp.container
  (:require [hsl.core :refer [hsl]]
            [respo-ui.core :as ui]
            [respo.core
             :refer
             [defcomp defeffect >> list-> <> div button textarea span input a]]
            [respo.comp.space :refer [=<]]
            [reel.comp.reel :refer [comp-reel]]
            [respo-md.comp.md :refer [comp-md]]
            [lilac-parser.config :refer [dev?]]
            [lilac-parser.core
             :refer
             [parse-lilac
              replace-lilac
              find-lilac
              defparser
              is+
              combine+
              some+
              many+
              optional+
              or+
              one-of+
              some+]]
            ["@mvc-works/codearea" :refer [codearea]]
            [clojure.string :as string]
            [cirru-edn.core :as cirru-edn]
            [feather.core :refer [comp-icon]]
            [lilac-parser.demo.s-expr :refer [s-expr-parser+]]
            [lilac-parser.demo.json
             :refer
             [demo-parser
              number-parser
              string-parser
              array-parser+
              value-parser+
              boolean-parser]]
            [respo-alerts.core :refer [use-prompt]]
            [cljs.reader :refer [read-string]]
            [favored-edn.core :refer [write-edn]]))

(def style-label
  {:font-family ui/font-code,
   :color (hsl 0 0 100),
   :display :inline-block,
   :line-height "22px",
   :padding "0 4px",
   :border-radius "4px",
   :margin-right 8,
   :white-space :pre,
   :min-height 14,
   :font-size 13})

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
        "Ok"
        (merge style-label {:background-color (hsl 200 80 70), :font-family ui/font-fancy}))
       (<>
        "Fail"
        (merge style-label {:background-color (hsl 20 80 50), :font-family ui/font-fancy})))
     (<>
      (name (:parser-node node))
      (merge style-label {:background-color (hsl 200 80 76), :font-family ui/font-fancy}))
     (if (or (= :label (:parser-node node)) (= :component (:parser-node node)))
       (<> (:label node) (merge style-label {:background-color (hsl 200 90 60)})))
     (if-not (:ok? node)
       (<> (:message node) (merge style-label {:background-color (hsl 0 80 60)})))
     (if (and (:ok? node) (= :is (:parser-node node)))
       (<> (:value node) (merge style-label {:background-color (hsl 200 80 70)})))
     (if (:ok? node)
       (<>
        (pr-str (:value node))
        (merge style-label {:background-color (hsl 200 80 80), :font-size 10})))
     (<>
      (->> (:rest node) (take 10) (string/join ""))
      (merge style-label {:background-color (hsl 100 10 70), :font-size 10, :min-height 16})))
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
       state (or (:data states) {:code "(def a (add 1 2))", :result nil, :gui? false})
       load-plugin (use-prompt
                    (>> states :load)
                    {:text "Load EDN",
                     :multiline? true,
                     :placeholder "lilac-parser parsing rule...",
                     :input-style {:font-family ui/font-code,
                                   :height 400,
                                   :white-space :pre,
                                   :font-size 12,
                                   :line-height "18px"},
                     :initial (write-edn (:result state) {:indent 2}),
                     :validator (fn [x]
                       (try
                        (do (read-string x) nil)
                        (catch js/Error e (js/console.log "Failed to parse") e)))})]
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
       :on-click (fn [e d!] (d! cursor (update state :gui? not)))})
     (=< 16 nil)
     (a
      {:inner-text "Load EDN",
       :style ui/link,
       :on-click (fn [e d!]
         ((:show load-plugin)
          d!
          (fn [text]
            (let [snapshot (read-string text)]
              (comment println "text" snapshot)
              (if (vector? snapshot)
                (d! cursor (assoc state :result snapshot))
                (d! cursor (assoc state :result snapshot)))))))})
     (=< 16 nil)
     (a
      {:inner-text "Replacer",
       :style ui/link,
       :on-click (fn [e d!]
         (let [result (replace-lilac
                       (string/split (:code state) "")
                       (s-expr-parser+)
                       (fn [result]
                         (println "replacing" result)
                         (str "<<<" (pr-str result) ">>>")))
               find-result (find-lilac (string/split (:code state) "") (s-expr-parser+))]
           (println (:result result))
           (d! cursor (assoc state :result (:attempts result)))
           (println "Find results:" (pr-str (:result find-result)))))}))
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
        {:style (merge ui/expand {:padding-bottom 400})}
        (if (vector? (:result state))
          (list->
           {}
           (->> (:result state)
                (map-indexed
                 (fn [idx value] [idx (comp-node (>> states (str :tree-viewer idx)) value)]))))
          (comp-node (>> states :tree-viewer) (:result state))))
       (textarea
        {:style (merge
                 ui/expand
                 ui/textarea
                 {:font-family ui/font-code, :font-size 12, :white-space :pre}),
         :disabled true,
         :spellcheck false,
         :value (cirru-edn/write (:result state))})))
    (when dev? (comp-reel (>> states :reel) reel {}))
    (:ui load-plugin))))

(defeffect
 effect-codearea
 ()
 (action el)
 (when (= action :mount) (let [target (.querySelector el ".codearea")] (codearea target))))
