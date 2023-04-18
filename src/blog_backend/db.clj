(ns blog-backend.db
  (:require [clojure.string :as string]
            [com.stuartsierra.component :as component]
            [ring.adapter.jetty :refer [run-jetty]]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
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
    (jdbc/execute-one! db
                       [post-table])
    (catch Exception e (str "Exception: " (.getMessage e))))
  (try
    (jdbc/execute-one! db
                       [content-table])
    (catch Exception e (str "Exception: " (.getMessage e))))
  (try
    (jdbc/execute-one! db
                       [line-table])
    (catch Exception e (str "Exception: " (.getMessage e))))
  )

(defrecord Database [db-spec     ; configuration
                     datasource] ; state
  component/Lifecycle
  (start [this]
    (if datasource
      this ; already initialized
      (let [database (assoc this :datasource (jdbc/get-datasource db-spec))]
        (prn database)
        (populate (:datasource database))
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

(defn parse-draft-post [text]
  (->>
      (clojure.string/split text #"---")
      (map #(clojure.string/split % #"\n"))
      (map #(filter not-empty %))
      (filter not-empty)
      (map (fn [block] {:type  (first block)
                        :lines (rest block)}))))

(defn save-post [db post]
  (let [type (:type post)
        title (first (:lines post))]
    (sql/insert! db :post {:type type
                             :title title})))

(defn save-line [db line]
  (sql/insert! db :line (dissoc line :line_id)))

(def a-content (atom {}))

(comment ""
         (def content @a-content)
         ,)

(defn save-content [db content]
  (reset! a-content content)
  (let [insert-c (sql/insert! db :content (dissoc content :content_id :lines))
        content-id (second (first insert-c))]
    (prn (str "Inserted " insert-c))
    (loop [[line & rem] (:lines content)
           i 0]
      (if (not (nil? line))
        (do
          (save-line db (assoc {} :line line :content_id content-id))
          (recur rem (inc i)))))))

