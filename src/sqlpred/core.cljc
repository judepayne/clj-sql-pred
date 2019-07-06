(ns sqlpred.core
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
   Op-S = '='|'not ='|'>'|'>='|'<'|'<='
   Op-M = ' in ' | ' not in '
   value = Word | QuotedWord
   list = <'('> value (<','> value)* <')'>
   <Word> = #'([a-zA-Z0-9\\(\\)-\\.]*)'
   QuotedWord = #\"'([^']*?)'\""))


(defparser ^{:private true} filter-parser
  filter-grammar
  :auto-whitespace whitespace)


(defn drop-first-last [s]
  (apply str (rest (drop-last s))))


(defn- filter-transform [parsed keywordize-keys?]
  (insta/transform
   {:S (fn [& args] args)
    :Conjunction str/trim
    :Op-S (fn [arg] [:op (str/trim arg)])
    :Op-M (fn [arg] [:op (str/trim arg)])
    :term (fn [arg] [:term (if keywordize-keys? (keyword arg) arg)])
    :list (fn [& args] [:value (map second args)])
    :QuotedWord (fn [arg] (drop-first-last arg))
    :value (fn [arg] [:value (str/trim arg)])
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
  [k v not? skip? item]
  (if (and skip? (not (contains? item k)))
    true              ;; return true is item doesn't contain key and skip is on
    (let [v (if (number? (get item k)) (parse-num v) v)]  ;; convert to number if necessary
      (if not?
        (not-submap? {k v} item)
        (submap? {k v} item)))))


(defn- inequality-match?
  "takes a term key, an op and term value and assessing whether the value of the
   key in the item matches the condition."
  [k op v skip? item]
  (if (and skip? (not (contains? item k)))
    true              ;; skip. return true
    (let [v (parse-num v)
          v-item (parse-num (get item k))]
      (when (not (number? v-item)) (throw (err "internal oops!")))
      (case op
        ">" (> v-item v)
        "<" (< v-item v)
        ">=" (>= v-item v)
        "<=" (<= v-item v)
        (throw (err (str op " is not a valid comparison operator.")))))))


(defn- multi-equality?
  [k vs not? skip? item]
  (if (and skip? (not (contains? item k)))
    true               ;; skip. return true
    (if not?
      (every? true? (map #(equality-match? k % true skip? item) vs))
      (some? (some true? (map #(equality-match? k % false skip? item) vs))))))


(defn- match-anything? [clause item]
  "If the value of the item is the special char then matching always succeeds."
  (some #(= (get item (:term clause)) %)
        ["<all>"]))


(defn- clause-matches?
  [clause skip? item]
  (let [op   (:op clause)
        term (:term clause)
        val  (:value clause)]
    (cond
      (match-anything? clause item)    true
      (= "=" op)       (equality-match? term val false skip? item)
      (= "not =" op)   (equality-match? term val true skip? item)
      (or (= ">" op)
          (= "<" op)
          (= ">=" op)
          (= "<=" op)) (inequality-match? term op val skip? item)
      (= "in" op)      (multi-equality? term val false skip? item)
      (= "not in" op)  (multi-equality? term val true skip? item)
      :else (throw (err (str op " is not a valid comparison operator."))))))


(defn- and-statements?
  [ands skip? item]
  (reduce
   (fn [a c]
     (if (not a)
       (reduced false)
       (clause-matches? c skip? item)))
   true
   ands))


(defn- or-statements?
  [ors skip? item]
  (reduce
   (fn [a c]
     (if a
       (reduced true)
       (and-statements? c skip? item)))
   false
   ors))


(defn sql-pred
  [statement & {:keys [keywordize-keys? skip-missing?]
                :or {keywordize-keys? false skip-missing? false}}]
  (let [terms (-> statement
                  filter-parser
                  (filter-transform keywordize-keys?)
                  filter-group)]
    (partial or-statements? terms skip-missing?)))
