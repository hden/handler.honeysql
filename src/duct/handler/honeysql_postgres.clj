(ns duct.handler.honeysql-postgres
  (:require [clojure.walk :refer [postwalk]]
            [duct.handler.sql :as sql]
            [honeysql.core :as honeysql]
            [integrant.core :as ig])
  (:import [honeysql.types SqlParam]))

(defn honeysql->sql [x]
  (let [params (atom {})
        symbol->param! (fn [x]
                         (if (symbol? x)
                           (let [param-name (name x)]
                             (swap! params assoc param-name x)
                             (SqlParam. param-name))
                           x))]
    (honeysql/format (postwalk symbol->param! x) :params @params)))

(defmethod ig/prep-key :duct.handler/honeysql-postgres [_ opts]
  (if (:db opts)
    opts
    (assoc opts :db (ig/ref :duct.database/sql))))

(defmethod ig/init-key ::query
  [_ options]
  (ig/init-key ::sql/query (update options :sql honeysql->sql)))

(defmethod ig/init-key ::query-one
  [_ options]
  (ig/init-key ::sql/query-one (update options :sql honeysql->sql)))

(defmethod ig/init-key ::execute
  [_ options]
  (ig/init-key ::sql/execute (update options :sql honeysql->sql)))

(defmethod ig/init-key ::insert
  [_ options]
  (ig/init-key ::sql/insert (update options :sql honeysql->sql)))
