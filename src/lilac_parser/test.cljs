
(ns lilac-parser.test
  (:require [cljs.test :refer [deftest is testing run-tests]]
            [lilac-parser.core
             :refer
             [parse-lilac defparser many+ is+ interleave+ some+ one-of+ combine+ optional+]]))

(defn exactly-ok? [x] (and (:ok? x) (empty? (:rest x))))

(defn not-ok? [x] (not (:ok? x)))

(defn roughly-ok? [x] (and (:ok? x) (not (empty? (:rest x)))))

(deftest
 test-is
 (testing "is x" (is (exactly-ok? (parse-lilac (list "x") (is+ "x")))))
 (testing "is xyx" (is (exactly-ok? (parse-lilac (list "x" "y" "z") (is+ "xyz")))))
 (testing "has x" (is (roughly-ok? (parse-lilac (list "x" "y") (is+ "x")))))
 (testing
  "roughly ok is not same as exactly ok"
  (is (not (exactly-ok? (parse-lilac (list "x" "y") (is+ "x"))))))
 (testing "is not x" (is (not-ok? (parse-lilac (list "y") (is+ "x"))))))

(deftest
 test-oneof
 (testing
  "x/y/z is one of xyz"
  (is (exactly-ok? (parse-lilac (list "x") (one-of+ "xyz"))))
  (is (exactly-ok? (parse-lilac (list "y") (one-of+ "xyz"))))
  (is (exactly-ok? (parse-lilac (list "z") (one-of+ "xyz")))))
 (testing "w is not one of xyz" (is (not-ok? (parse-lilac (list "w") (one-of+ "xyz")))))
 (testing
  "xy has one of xyz"
  (is (roughly-ok? (parse-lilac (list "x" "y") (one-of+ "xyz"))))))
