(ns blog-backend.server
  (:require [com.stuartsierra.component :as component]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.json :as rjson]
            [ring.middleware.defaults :as ring-defaults]
            [compojure.coercions :refer [as-int]]
            [compojure.core :refer [GET POST DELETE let-routes]]
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

(defn my-print-handler [handler]
  (fn [req]
    (prn "my-middleware req")
    (prn req)
    (handler req)))

(defn my-middleware [handler] "Allows easier wrapping of handler output"
  (prn "my-middleware setup")
  (-> handler
    (rjson/wrap-json-body)
    (rjson/wrap-json-response)
    (#'my-print-handler)))

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
                                         (assoc-in [:params :multipart] true)
                                         (assoc-in [:proxy] true)
                                         (assoc-in [:responses :content-types] true))))))

(defn handler [application]
  (prn "handler setup")
  (let-routes [wrap (middleware-stack application #'my-middleware)]
    (GET "/" [] (wrap #'c/default))
    (POST "/" [] (wrap #'c/default))
    (GET "/post/:id{[0-9]+}" [post-id :<< as-int] (wrap #'c/get-post-by-id))
    (GET "/post/:name" [post-name] (wrap #'c/get-post-by-name))
    (POST "/post/save" [] (wrap #'c/save-post))
    (DELETE "/post/:id{[0-9]+}" [] (wrap #'c/delete-post))
    (route/not-found (do "NOT FOUND"))
  )
)

(defn setup [port]
  (component/using (map->WebServer {:handler-fn #'handler
                                    :port port})
                   [:application])
)
