
(ns lilac-parser.test
  (:require [cljs.test :refer [deftest is testing run-tests]]
            [lilac-parser.core
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
              or+]]))

(defn exactly-ok? [x] (and (:ok? x) (empty? (:rest x))))

(defn not-ok? [x] (not (:ok? x)))

(defn roughly-ok? [x] (and (:ok? x) (not (empty? (:rest x)))))

(deftest
 test-combine
 (testing
  "is xy"
  (is (exactly-ok? (parse-lilac (list "x" "y") (combine+ [(is+ "x") (is+ "y")])))))
 (testing
  "contains xy"
  (is (roughly-ok? (parse-lilac (list "x" "y" "z") (combine+ [(is+ "x") (is+ "y")])))))
 (testing
  "wrong order Of xy"
  (is (not-ok? (parse-lilac (list "x" "y") (combine+ [(is+ "y") (is+ "x")]))))))

(deftest
 test-interleave
 (testing
  "repeat xy"
  (is (exactly-ok? (parse-lilac (list "x" "y") (interleave+ (is+ "x") (is+ "y"))))))
 (testing
  "repeat xy of 3"
  (is (exactly-ok? (parse-lilac (list "x" "y" "x") (interleave+ (is+ "x") (is+ "y"))))))
 (testing
  "repeat xy of 4"
  (is (exactly-ok? (parse-lilac (list "x" "y" "x" "y") (interleave+ (is+ "x") (is+ "y"))))))
 (testing
  "repeat xy wrong"
  (is (not-ok? (parse-lilac (list "y" "x" "y") (interleave+ (is+ "x") (is+ "y")))))))

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
 test-many
 (testing "an x" (is (exactly-ok? (parse-lilac (list "x") (many+ (is+ "x"))))))
 (testing "two xs" (is (exactly-ok? (parse-lilac (list "x" "x") (many+ (is+ "x"))))))
 (testing "many xs" (is (exactly-ok? (parse-lilac (list "x" "x" "x") (many+ (is+ "x"))))))
 (testing
  "contains many xs"
  (is (roughly-ok? (parse-lilac (list "x" "x" "x" "y") (many+ (is+ "x")))))))

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

(deftest
 test-optional
 (testing "optional x" (is (exactly-ok? (parse-lilac (list "x") (optional+ (is+ "x"))))))
 (testing "optional nil x" (is (exactly-ok? (parse-lilac (list) (optional+ (is+ "x"))))))
 (testing
  "x for optional y"
  (is (roughly-ok? (parse-lilac (list "x") (optional+ (is+ "y")))))))

(deftest
 test-or
 (testing "x or y" (is (exactly-ok? (parse-lilac (list "x") (or+ [(is+ "x") (is+ "y")])))))
 (testing "x or y" (is (exactly-ok? (parse-lilac (list "y") (or+ [(is+ "x") (is+ "y")])))))
 (testing "z is x or y" (is (not-ok? (parse-lilac (list "z") (or+ [(is+ "x") (is+ "y")]))))))

(deftest
 test-other-than
 (testing "other than abc" (is (exactly-ok? (parse-lilac (list "x") (other-than+ "abc")))))
 (testing
  "contains text other than abc"
  (is (roughly-ok? (parse-lilac (list "x" "y") (other-than+ "abc")))))
 (testing "a is in abc" (is (not-ok? (parse-lilac (list "a") (other-than+ "abc"))))))

(deftest
 test-some
 (testing "no x" (is (exactly-ok? (parse-lilac (list) (some+ (is+ "x"))))))
 (testing "an x" (is (exactly-ok? (parse-lilac (list "x") (some+ (is+ "x"))))))
 (testing "multiple x" (is (exactly-ok? (parse-lilac (list "x" "x") (some+ (is+ "x"))))))
 (testing
  "contains multiple x"
  (is (roughly-ok? (parse-lilac (list "x" "x" "y") (some+ (is+ "x"))))))
 (testing "no x in y" (is (roughly-ok? (parse-lilac (list "y") (some+ (is+ "x")))))))
