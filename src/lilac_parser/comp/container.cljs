
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
             [parse-lilac defparser is+ combine+ some+ many+ optional+]]
            ["@mvc-works/codearea" :refer [codearea]]
            [clojure.string :as string]
            [cirru-edn.core :as cirru-edn]))

(defeffect
 effect-codearea
 ()
 (action el)
 (when (= action :mount) (let [target (.querySelector el ".codearea")] (codearea target))))

(defcomp
 comp-container
 (reel)
 (let [store (:store reel)
       states (:states store)
       state (or (:data states) {:code "", :result nil})]
   [(effect-codearea)
    (div
     {:style (merge ui/global ui/fullscreen ui/column)}
     (div
      {:style {:padding 8}}
      (button
       {:style ui/button,
        :inner-text "Parse",
        :on-click (fn [e d! m!]
          (let [result (parse-lilac
                        (string/split (:code state) "")
                        (combine+ [(many+ (is+ "a")) (many+ (is+ "b"))]))]
            (m! (assoc state :result result))))}))
     (div
      {:style (merge ui/expand ui/row)}
      (textarea
       {:value (:code state),
        :class-name "codearea",
        :placeholder "Content",
        :style (merge ui/expand ui/textarea {:font-family ui/font-code}),
        :on-input (fn [e d! m!] (m! (assoc state :code (:value e))))})
      (textarea
       {:style (merge ui/expand ui/textarea {:font-family ui/font-code}),
        :value (cirru-edn/write (:result state))}))
     (when dev? (cursor-> :reel comp-reel states reel {})))]))
