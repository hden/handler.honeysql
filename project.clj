(defproject hden/handler.honeysql "0.5.0"
  :description "Duct library for building simple database-driven handlers"
  :url "https://github.com/hden/handler.honeysql"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [duct/handler.sql "0.4.0"]
                 [honeysql "1.0.444"]
                 [integrant "0.8.0"]]
  :profiles
  {:dev {:dependencies [[org.clojure/java.jdbc "0.7.11"]
                        [org.xerial/sqlite-jdbc "3.41.2.1"]
                        [duct/core "0.8.0"]
                        [duct/database.sql "0.1.0"]]}})
