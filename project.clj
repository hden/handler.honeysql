(defproject hden/handler.honeysql "0.3.1"
  :description "Duct library for building simple database-driven handlers"
  :url "https://github.com/duct-framework/handler.sql"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/java.jdbc "0.7.3"]
                 [duct/core "0.6.1"]
                 [duct/database.sql "0.1.0"]
                 [integrant "0.6.1"]
                 [honeysql "0.9.4"]
                 [medley "1.0.0"]
                 [ring/ring-core "1.6.3"]
                 [uritemplate-clj "1.1.1"]]
  :profiles
  {:dev {:dependencies [[org.xerial/sqlite-jdbc "3.21.0"]]}})
