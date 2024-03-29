# clj-sql-pred
Convert simple sql statements to predicates for filtering collections of Clojure/ ClojureScript maps.

The `sql-pred` function in the `core` namespace takes a sql like string and returns a Clojure predicate function that works on maps.

allowed sql keywords: `and, or, =, not, in, >, >=, <, <=`

### Clojars

    [clj-sql-pred "0.1.4"]


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
    
    ;; demonstrate the use of skip-missing? the first map does not contain :b
    clj-sql-pred.core> (filter (sql-pred "b = red" :keywordize-keys? true :skip-missing? true) coll)
    ({:a "clouds", :c 20} {:a "trains", :b "red", :c 35})
    ;; but still makes it through the filter

The `keywordize-keys?` optional boolean argument determines whether the keywords in your sql-like statement e.g. 'c' is converted to a clojure keyword, e.g. `:c` before any comparisons are done. In the above examples, that's necessary as the maps in the collection contain `:c` not `"c"`.

The `skip-missing?` optional boolean argument determines what happens when a map in your collection does not contain the key being filtered on. A value of true will mean that if the map doesn't contain the key being filtered for that the map will pass successfully and not be filtered. A value of false will mean that the filtered is applied to the map, but without the key, the fetch of the key's value will return nil and unless your term is looking for nils it will be filtered.

#### Special cases

1) Strings with spaces

If one of the strings to be filtered contains spaces, e.g.

    (def coll [{:a "the clouds" :c 20} {:a "trains" :b "red" :c 35}])
    
To get a macth your sql-pred must be quoted with single quotes, e.g.

    clj-sql-pred.core> (filter (sql-pred "a = 'the clouds'" :keywordize-keys? true) coll)
    ({:a "clouds", :c 20})
    
2) Match anything

Sometimes you want a particular key in one of your to be tested maps to always pass a match test.
This is where the special value "<all>" comes in. For example.

    (def coll [{:a "the clouds" :c "<all>"} {:a "trains" :b "red" :c 35}])
    
     clj-sql-pred.core> (filter (sql-pred "c = 0" :keywordize-keys? true) coll)
    ({:a "clouds", :c "<all>"})



### License

MIT
