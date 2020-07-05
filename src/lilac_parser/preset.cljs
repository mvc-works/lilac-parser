
(ns lilac-parser.preset
  (:require [lilac-parser.core
             :refer
             [parse-lilac
              defparser
              many+
              is+
              interleave+
              some+
              one-of+
              combine+
              optional+
              other-than+
              or+
              unicode-range+
              label+]]))

(def lilac-alphabet
  (label+ "alphabet" (one-of+ "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz")))

(def lilac-chinese-char (label+ "Chinese char" (unicode-range+ 0x4e00 0x9fa5)))

(def lilac-space (is+ " "))

(def lilac-comma-space
  (label+
   "comma with spaces"
   (combine+ [(some+ lilac-space) (is+ ",") (some+ lilac-space)] (fn [x] nil))))

(def lilac-digit (label+ "digit" (one-of+ "0123456789")))
