(ns blog-backend.db
  (:require [com.stuartsierra.component :as component]
            [ring.adapter.jetty :refer [run-jetty]]
            [next.jdbc :as jdbc]
            ))

(def ^:private my-local-db
  "SQLite database connection spec."
  {:dbtype "sqlite" :dbname "blog_db"})

(defrecord Database [db-spec     ; configuration
                     datasource] ; state
  component/Lifecycle
  (start [this]
    (if datasource
      this ; already initialized
      (assoc this :datasource (jdbc/get-datasource db-spec))))
  (stop [this]
    (assoc this :datasource nil)))

(defn setup []
  (component/using (map->Database {:db-spec my-local-db})
                   [])
  )
