
(ns lilac-parser.core (:require-macros [lilac-parser.core]))

(defn combine+ [xs] {:parser-node :combine, :seq xs})

(defn indent+ [] {:parser-node :indent})

(defn is+ [x] {:parser-node :is, :item x})

(defn many+ [item] {:parser-node :many, :item x})

(defn optional+ [x] {:parser-node :optional, :item x})

(defn or+ [xs] {:parser-node :or, :choices xs})

(defn some+ [x] {:parser-node :some, :item x})

(defn unindent+ [] {:parser-node :unindent})
