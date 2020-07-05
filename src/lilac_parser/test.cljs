
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
              or+
              unicode-range+
              replace-lilac
              find-lilac]]
            [lilac-parser.preset
             :refer
             [lilac-digit lilac-alphabet lilac-comma-space lilac-chinese-char]]))

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
 test-find
 (testing
  (is
   (=
    2
    (count
     (:result (find-lilac "write cumulo and respo" (or+ [(is+ "cumulo") (is+ "respo")]))))))
  (is
   (=
    1
    (count
     (:result (find-lilac "write cumulo and phlox" (or+ [(is+ "cumulo") (is+ "respo")]))))))
  (is
   (=
    0
    (count
     (:result (find-lilac "write cumulo and phlox" (or+ [(is+ "cirru") (is+ "respo")]))))))))

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
 test-preset
 (testing
  "find alphabet"
  (is (exactly-ok? (parse-lilac "a" lilac-alphabet)))
  (is (exactly-ok? (parse-lilac "A" lilac-alphabet)))
  (is (not-ok? (parse-lilac "." lilac-alphabet))))
 (testing
  "digits"
  (is (exactly-ok? (parse-lilac "1" lilac-digit)))
  (is (not-ok? (parse-lilac "a" lilac-digit))))
 (testing
  "comma with spaces"
  (is (exactly-ok? (parse-lilac "," lilac-comma-space)))
  (is (exactly-ok? (parse-lilac ", " lilac-comma-space)))
  (is (exactly-ok? (parse-lilac " ," lilac-comma-space)))
  (is (exactly-ok? (parse-lilac " , " lilac-comma-space)))
  (is (exactly-ok? (parse-lilac "  , " lilac-comma-space)))
  (is (not-ok? (parse-lilac "." lilac-comma-space))))
 (testing
  "chinese character"
  (is (exactly-ok? (parse-lilac "汉" lilac-chinese-char)))
  (is (not-ok? (parse-lilac "E" lilac-chinese-char)))
  (is (not-ok? (parse-lilac "," lilac-chinese-char)))
  (is (not-ok? (parse-lilac "，" lilac-chinese-char)))))

(deftest
 test-replace
 (testing
  "replaced content"
  (is
   (=
    "my project"
    (:result
     (replace-lilac "cumulo project" (or+ [(is+ "cumulo") (is+ "respo")]) (fn [x] "my")))))
  (is
   (=
    "my project"
    (:result
     (replace-lilac "respo project" (or+ [(is+ "cumulo") (is+ "respo")]) (fn [x] "my")))))
  (is
   (=
    "phlox project"
    (:result
     (replace-lilac "phlox project" (or+ [(is+ "cumulo") (is+ "respo")]) (fn [x] "my")))))))

(deftest
 test-some
 (testing "no x" (is (exactly-ok? (parse-lilac (list) (some+ (is+ "x"))))))
 (testing "an x" (is (exactly-ok? (parse-lilac (list "x") (some+ (is+ "x"))))))
 (testing "multiple x" (is (exactly-ok? (parse-lilac (list "x" "x") (some+ (is+ "x"))))))
 (testing
  "contains multiple x"
  (is (roughly-ok? (parse-lilac (list "x" "x" "y") (some+ (is+ "x"))))))
 (testing "no x in y" (is (roughly-ok? (parse-lilac (list "y") (some+ (is+ "x")))))))

(deftest
 test-unicode-range
 (testing
  "parse by unicode"
  (is (exactly-ok? (parse-lilac "a" (unicode-range+ 97 122))))
  (is (exactly-ok? (parse-lilac "z" (unicode-range+ 97 122))))
  (is (not-ok? (parse-lilac "A" (unicode-range+ 97 122))))))
