(defproject hden/handler.honeysql "0.4.0"
  :description "Duct library for building simple database-driven handlers"
  :url "https://github.com/hden/handler.honeysql"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [duct/handler.sql "0.4.0"]
                 [honeysql "0.9.7"]
                 [integrant "0.7.0"]]
  :profiles
  {:dev {:dependencies [[org.clojure/java.jdbc "0.7.10"]
                        [org.xerial/sqlite-jdbc "3.28.0"]
                        [duct/core "0.7.0"]
                        [duct/database.sql "0.1.0"]]}})
