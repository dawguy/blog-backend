(ns blog-backend.server
  (:require [com.stuartsierra.component :as component]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.json :as rjson]
            [ring.middleware.defaults :as ring-defaults]
            [compojure.core :refer [GET POST let-routes]]
            [compojure.route :as route]
            [blog-backend.controller :as c]
            ))

(defrecord WebServer [handler-fn port
                      application
                      http-server shutdown]
  component/Lifecycle
  (start [this]
    (if http-server
      this
      (assoc this
        :http-server (run-jetty (handler-fn application)
                                {:port port :join? false})
        :shutdown (promise))))
  (stop [this]
    (if http-server
      (do
        (.stop http-server)
        (deliver shutdown true)
        (assoc this :http-server nil))
      this)))

(defn my-middleware [handler] "Allows easier wrapping of handler output"
  (prn "my-middleware setup")
  (rjson/wrap-json-response
    (fn [req]
      (prn "my-middleware req")
      (handler req)
      )))

(defn add-app-component [handler application]
  (prn "adding app component")
  (fn [req]
    (prn "add-app-component req")
    (handler (assoc req :application/component application))))

(defn middleware-stack [application app-middleware]
  (prn "middleware-stack setup")
  (fn [handler]
    (-> handler
        (add-app-component application)
        (app-middleware)
        (ring-defaults/wrap-defaults (-> ring-defaults/site-defaults
                                         ;; disable XSRF for now
                                         (assoc-in [:security :anti-forgery] false)
                                         ;; support load balancers
                                         (assoc-in [:proxy] true)
                                         (assoc-in [:responses :content-types] true))))))

(defn handler [application]
  (prn "handler setup")
  (let-routes [wrapped-handler (middleware-stack application #'my-middleware)]
    (GET "/" [] (wrapped-handler #'c/default))
    (route/not-found (do "NOT FOUND"))
  )
)

(defn setup [port]
  (component/using (map->WebServer {:handler-fn #'handler
                                    :port port})
                   [:application])
)
