
(ns lilac-parser.comp.container
  (:require [hsl.core :refer [hsl]]
            [respo-ui.core :as ui]
            [respo.core
             :refer
             [defcomp defeffect cursor-> <> div button textarea span input]]
            [respo.comp.space :refer [=<]]
            [reel.comp.reel :refer [comp-reel]]
            [respo-md.comp.md :refer [comp-md]]
            [lilac-parser.config :refer [dev?]]
            [lilac-parser.core
             :refer
             [parse-lilac defparser is+ combine+ some+ many+ optional+ or+ one-of+ some+]]
            ["@mvc-works/codearea" :refer [codearea]]
            [clojure.string :as string]
            [cirru-edn.core :as cirru-edn]))

(def number-parser (many+ (one-of+ (set (string/split "1234567890" "")))))

(def space-parser (is+ " "))

(def word-parser (many+ (one-of+ (set (string/split "qwertyuiopasdfghjklzxcvbnm" "")))))

(defparser
 s-expr-parser+
 ()
 identity
 (combine+
  [(is+ "(")
   (some+ (or+ [number-parser word-parser space-parser (s-expr-parser+)]))
   (is+ ")")]))

(defcomp
 comp-container
 (reel)
 (let [store (:store reel)
       states (:states store)
       state (or (:data states) {:code "(def a (add 1 2))", :result nil})]
   (div
    {:style (merge ui/global ui/fullscreen ui/column)}
    (div
     {:style {:padding 8}}
     (button
      {:style ui/button,
       :inner-text "Parse",
       :on-click (fn [e d! m!]
         (let [result (parse-lilac (string/split (:code state) "") (s-expr-parser+))]
           (m! (assoc state :result result))))}))
    (div
     {:style (merge ui/expand ui/row)}
     (textarea
      {:value (:code state),
       :class-name "codearea",
       :placeholder "Content",
       :style (merge ui/textarea {:font-family ui/font-code, :width 300}),
       :on-input (fn [e d! m!] (m! (assoc state :code (:value e))))})
     (textarea
      {:style (merge
               ui/expand
               ui/textarea
               {:font-family ui/font-code, :font-size 12, :white-space :nowrap}),
       :spellcheck false,
       :value (cirru-edn/write (:result state))}))
    (when dev? (cursor-> :reel comp-reel states reel {})))))

(defeffect
 effect-codearea
 ()
 (action el)
 (when (= action :mount) (let [target (.querySelector el ".codearea")] (codearea target))))
