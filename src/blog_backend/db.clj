(ns blog-backend.db
  (:require [com.stuartsierra.component :as component]
            [ring.adapter.jetty :refer [run-jetty]]
            [next.jdbc :as jdbc]
            ))

(def ^:private my-local-db
  "SQLite database connection spec."
  {:dbtype "sqlite" :dbname "blog_db"})

(defn populate [db]
  (try
    (jdbc/execute-one! (db)
                       [(str "
create table department (
  id            integer primary key autoincrement,
  name          varchar(64)
)")])))

(defrecord Database [db-spec     ; configuration
                     datasource] ; state
  component/Lifecycle
  (start [this]
    (if datasource
      this ; already initialized
      (let [database (assoc this :datasource (jdbc/get-datasource db-spec))]
        (prn database)
        (populate database)
        database)))
  (stop [this]
    (assoc this :datasource nil))
  ;; allow the Database component to be "called" with no arguments
  ;; to produce the underlying datasource object
  clojure.lang.IFn
  (invoke [_] datasource))

(defn setup []
  (component/using (map->Database {:db-spec my-local-db})
                   []))
