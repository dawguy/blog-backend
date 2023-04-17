(ns blog-backend.controller
  (:require [ring.util.response :as resp]))

(defn default [req]
  (println (str "DEFAULT HIT" req))
  (resp/response {:abc "123"}))

(defn not-found [req]
  (prn (str "ROUTE NOT FOUND")))
