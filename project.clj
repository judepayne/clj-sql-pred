(defproject clj-sql-pred "0.1.3"
  :description "Convert simple sql statements to predicates for filtering collections of Clojure/ Clojurescript maps"
  :url "https://github.com/judepayne/clj-sql-pred"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [aysylu/loom "1.0.2"]
                 [instaparse "1.4.9"]]
  :deploy-repositories {"clojars"  {:sign-releases false :url "https://clojars.org/repo"}
                        "snapshots" {:url "https://clojars.org/repo"}})
