(ns clj-sql-pred.core
  (:require [clojure.string                       :as str]
            [loom.graph                           :as loom]
            #?(:clj [instaparse.core :as insta :refer [defparser]]
               :cljs [instaparse.core :as insta :refer-macros [defparser]])))


(defn- err
  "Creates an exception object with error-string."
  [error-string]
  #?(:clj (Exception. ^String error-string)
     :cljs (js/Error. error-string)))


(def ^{:private true} whitespace
  (insta/parser
    "whitespace = #'\\s+'"))


(def ^{:private true} filter-grammar
  (str
   "S = Clause (Conjunction Clause)*
   Conjunction = ' and ' | ' or '
   Clause = term Op-S value | term Op-M list
   term = Word
   Op-S = ' = '|' not = '|' > '|' >= '|' < '|' <= '
   Op-M = ' in ' | ' not in '
   value = Word
   list = <'('> value (<','> value)* <')'>
   <Word> = #'[a-zA-Z0-9]*'"))


(defparser ^{:private true} filter-parser
  filter-grammar
  :auto-whitespace whitespace)


(defn- filter-transform [parsed keywordize-keys?]
  (insta/transform
   {:S (fn [& args] args)
    :Conjunction str/trim
    :Op-S (fn [arg] [:op (str/trim arg)])
    :Op-M (fn [arg] [:op (str/trim arg)])
    :term (fn [arg] [:term (if keywordize-keys? (keyword arg) arg)])
    :list (fn [& args] [:value (map second args)])
    :Clause (fn [& args] (into {} args))}
   parsed))


(defn- filter-group [transformed]
  (let [t1 (->> transformed
                (partition-by #(= "or" %))
                (filter #(not= '("or") %)))]
    (map (fn [s] (filter #(not= "and" %) s)) t1)))


(defn- submap?
  "Checks whether m contains all entries in sub."
  [sub m]
  (= sub (select-keys m (keys sub))))


(def ^{:private true} not-submap? (complement submap?))


(defn- parse-num [s]
  (if (number? s) s
      (try
        (let [n #?(:clj (clojure.edn/read-string s)
                   :cljs (cljs.reader/read-string s))]
          (if (number? n) n (throw (err (str "Could not convert " s " to a number.")))))
        #? (:clj (catch
                     Exception
                     e
                   (throw (err (str "Could not convert " s " to a number."))))
            :cljs (catch
                      js/Error
                      e
                    (throw (err (str "Could not convert " s " to a number."))))))))


(defn- equality-match?
  "takes a term key and term value and assesses whether the key and value
   is a submap of item."
  [k v not? item]
  (let [v (if (number? (get item k)) (parse-num v) v)]  ;; convert to number if necessary
    (if not?
      (not-submap? {k v} item)
      (submap? {k v} item))))


(defn- inequality-match?
  "takes a term key, an op and term value and assessing whether the value of the
   key in the item matches the condition."
  [k op v item]
  (let [v (parse-num v)
        v-item (parse-num (get item k))]
    (when (not (number? v-item)) (throw (err "internal oops!")))
    (case op
      ">" (> v-item v)
      "<" (< v-item v)
      ">=" (>= v-item v)
      "<=" (<= v-item v)
      (throw (err (str op " is not a valid comparison operator."))))))


(defn- multi-equality?
  [k vs not? item]
  (if not?
    (every? true? (map #(equality-match? k % true item) vs))
    (some? (some true? (map #(equality-match? k % false item) vs)))))


(defn- clause-matches?
  [clause item]
  (let [op   (:op clause)
        term (:term clause)
        val  (:value clause)]
    (cond
      (= "=" op)       (equality-match? term val false item)
      (= "not =" op)   (equality-match? term val true item)
      (or (= ">" op)
          (= "<" op)
          (= ">=" op)
          (= "<=" op)) (inequality-match? term op val item)
      (= "in" op)      (multi-equality? term val false item)
      (= "not in" op)  (multi-equality? term val true item)
      :else (throw (err (str op " is not a valid comparison operator."))))))


(defn- and-statements?
  [ands item]
  (reduce
   (fn [a c]
     (if (not a)
       (reduced false)
       (clause-matches? c item)))
   true
   ands))


(defn- or-statements?
  [ors item]
  (reduce
   (fn [a c]
     (if a
       (reduced true)
       (and-statements? c item)))
   false
   ors))


(defn sql-pred
  [statement & {:keys [keywordize-keys?] :or {keywordize-keys? false}}]
  (let [terms (-> statement
                  filter-parser
                  (filter-transform keywordize-keys?)
                  filter-group)]
    (partial or-statements? terms)))