# clj-sql-pred
Convert simple sql statements to predicates for filtering collections of Clojure/ ClojureScript maps.

The `sql-pred` function in the `core` namespace takes a sql like string and returns a Clojure predicate function that works on maps.

allowed sql keywords: `and, or, =, not, in, >, >=, <, <=`

### Clojars

    [clj-sql-pred "0.1.2"]


### Example usage:

    clj-sql-pred.core> (def coll [{:a "clouds" :c 20} {:a "trains" :b "red" :c 35}])
    #'clj-sql-pred.core/coll
    
    clj-sql-pred.core> (filter (sql-pred "c = 20" :keywordize-keys? true) coll)
    ({:a "clouds", :c 20})
    
    clj-sql-pred.core> (filter (sql-pred "c > 20" :keywordize-keys? true) coll)
    ({:a "trains", :b "red", :c 35})
    
    clj-sql-pred.core> (filter (sql-pred "c > 19 and c < 50" :keywordize-keys? true) coll)
    ({:a "clouds", :c 20} {:a "trains", :b "red", :c 35})
    
    clj-sql-pred.core> (filter (sql-pred "c > 19 or c < 50" :keywordize-keys? true) coll)
    ({:a "clouds", :c 20} {:a "trains", :b "red", :c 35})
    
    clj-sql-pred.core> (filter (sql-pred "a = clouds" :keywordize-keys? true) coll)
    ({:a "clouds", :c 20})
    
    clj-sql-pred.core> (filter (sql-pred "a in (clouds, trains, bikes, rocks)" :keywordize-keys? true) coll)
    ({:a "clouds", :c 20} {:a "trains", :b "red", :c 35})
    
    clj-sql-pred.core> (filter (sql-pred "a not in (clouds, bikes, rocks)" :keywordize-keys? true) coll)
    ({:a "trains", :b "red", :c 35})
    
    clj-sql-pred.core> (filter (sql-pred "a not in (clouds, bikes, rocks) or c = 20" :keywordize-keys? true) coll)
    ({:a "clouds", :c 20} {:a "trains", :b "red", :c 35})
    
    clj-sql-pred.core> (filter (sql-pred "c not = 20" :keywordize-keys? true) coll)
    ({:a "trains", :b "red", :c 35})

The `keywordize-keys?` optional boolean argument determines whether the keywords in your sql-like statement e.g. 'c' is converted to a clojure keyword, e.g. `:c` before any comparisons are done. In the above examples, that's necessary as the maps in the collection contain `:c` not `"c"`.


### License

MIT
