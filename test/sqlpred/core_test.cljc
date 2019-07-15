(ns sqlpred.core-test
  (:require [clojure.test :refer :all]
            [sqlpred.core :refer :all]))


(def test-coll [{:a "clouds" :c 20} {:a "trains" :b "red" :c 35}])


(deftest test-equals
  (testing "does = work"
    (is (= (filter (sql-pred "c = 20" :keywordize-keys? true) test-coll)
           '({:a "clouds", :c 20})))))


(deftest test-GT
  (testing "does > work"
    (is (= (filter (sql-pred "c > 20" :keywordize-keys? true) test-coll)
           '({:a "trains", :b "red", :c 35})))))


(deftest test-and
  (testing "does and work"
    (is (= (filter (sql-pred "c > 19 and c < 50" :keywordize-keys? true) test-coll)
           '({:a "clouds", :c 20} {:a "trains", :b "red", :c 35})))))


(deftest test-or
  (testing "does = work"
    (is (= (filter (sql-pred "c > 19 or c < 50" :keywordize-keys? true) test-coll)
           '({:a "clouds", :c 20} {:a "trains", :b "red", :c 35})))))


(deftest test-equals2
  (testing "does = work 2"
    (is (= (filter (sql-pred "a = clouds" :keywordize-keys? true) test-coll)
           '({:a "clouds", :c 20})))))


(deftest test-in
  (testing "does in work"
    (is (= (filter (sql-pred "a in (clouds, trains, bikes, rocks)" :keywordize-keys? true) test-coll)
           '({:a "clouds", :c 20} {:a "trains", :b "red", :c 35})))))


(deftest test-not-in
  (testing "does not in work"
    (is (= (filter (sql-pred "a not in (clouds, bikes, rocks)" :keywordize-keys? true) test-coll)
           '({:a "trains", :b "red", :c 35})))))


(deftest test-not-in-or
  (testing "does not in or work"
    (is (= (filter (sql-pred "a not in (clouds, bikes, rocks) or c = 20" :keywordize-keys? true) test-coll)
           '({:a "clouds", :c 20} {:a "trains", :b "red", :c 35})))))


(deftest test-not-equals
  (testing "does not equal work"
    (is (= (filter (sql-pred "a not = clouds" :keywordize-keys? true) test-coll)
           '({:a "trains", :b "red", :c 35})))))


(deftest test-skip-missing
  (testing "does skip missing work"
    (is (= (filter (sql-pred "b = red" :keywordize-keys? true :skip-missing? true) test-coll)
           '({:a "clouds", :c 20} {:a "trains", :b "red", :c 35})))))



(def test-coll2 [{:a "clouds" :c 20} {:a "the trains" :b "red" :c 35}])


(deftest test-quoted
  (testing "do quoted words work"
    (is (= (filter (sql-pred "a = 'the trains'" :keywordize-keys? true :skip-missing? true) test-coll2)
           '({:a "the trains", :b "red", :c 35})))))


(deftest test-double-and-or
  (testing "does double and + or work"
    (is (= (filter (sql-pred "a = clouds or a = 'the trains' and c = 20 and c = 'clouds'"
                             :keywordize-keys? true) test-coll2)
           '({:a "clouds" :c 20})))))


