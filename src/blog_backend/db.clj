(ns blog-backend.db
  (:require [com.stuartsierra.component :as component]
            [ring.adapter.jetty :refer [run-jetty]]
            [next.jdbc :as jdbc]
            ))

(def ^:private my-local-db
  "SQLite database connection spec."
  {:dbtype "sqlite" :dbname "blog_db"})

(def post-table (str "
create table post (
  post_id integer primary key autoincrement,
  summary text,
  type text,
  title text,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
)"))

(def content-table (str "
create table content (
  content_id integer primary key autoincrement,
  type text,
  order_id integer,
  post_id integer,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  FOREIGN KEY(post_id) REFERENCES post(post_id)
)"))

(def line-table (str "
create table line (
  line_id integer primary key autoincrement,
  content_id integer,
  line text,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  FOREIGN KEY(content_id) REFERENCES content(content_id)
)"))

(defn populate [db]
  (try
    (jdbc/execute-one! (db)
                       [post-table]))
  (try
    (jdbc/execute-one! (db)
                       [content-table]))
  (try
    (jdbc/execute-one! (db)
                       [line-table])))

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
