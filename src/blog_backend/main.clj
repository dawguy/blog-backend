(ns blog-backend.main
  (:require [com.stuartsierra.component :as component]
            [blog-backend.db :as db]
            [blog-backend.server :as server]
            ))

(defrecord Application [config database state]
  component/Lifecycle
  (start [this]
    (assoc this :state "Running"))
  (stop [this]
    (assoc this :state "Stopped")))

(defn application [config]
  (component/using (map->Application {:config config})
                   [:database])
)

(defn new-system
  ([port] (new-system port true))
  ([port repl]
   (component/system-map :application (application {:repl repl})
                         :web-server (server/setup port)
                         :database (db/setup))))
(comment
  (def system (new-system 8888))
  (alter-var-root #'system component/start)
  (alter-var-root #'system component/stop)
  (def db (get-in system [:database :datasource]))
  ,)
