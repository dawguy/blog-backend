(ns blog-backend.controller
  (:require [ring.util.response :as resp]
            [blog-backend.db :as d]))

(defn default [req]
  (resp/response {:abc "123"}))

(def a-req (atom {}))
(def a-saved-post (atom {}))
(def a-contents (atom {}))

(comment ""
         (def req @a-req)
         (def saved-post @a-saved-post)
         (def contents @a-contents)
         (def db (get-in @a-req [:application/component :database :datasource]))
         (def post-id (second (first @a-saved-post)))
         ,)

;curl -X POST localhost:8888/post/save -d '{"text": "blog\nTest Draft Post\n---\nsection-header\nHello world!\n---\ntext\nMy name is david.\n---\ntext\nMy online names are bloodisblue or dawguy.\n---\nimage\nabcdef.png\nA scorpion deer\n---\nindent\nThis is what an indented div looks like.\n---\nsection-header\nCode snippets\n---\ncode-clojure\n(prn (str \"I did\" (inc 2) \"lines here!\"))\n---\ncode-typescript\nconsole.out.println(\"Yoyo\");\nconsole.out.println(\"ABC\");\n---\ntext\nEnding with some text.\n---\n"}' -H 'Content-Type: application/json'
(defn save-post [req]
  (reset! a-req req)
  (let [[post & contents] (d/parse-draft-post (get-in req [:body "text"]))
        db (get-in req [:application/component :database :datasource])
        saved-post (d/save-post db post)
        post-id (second (first saved-post))]
    (reset! a-saved-post saved-post)
    (reset! a-contents contents)
    (loop [[c & rem] contents
             i 0]
        (if (some? c)
          (do
            (d/save-content db (assoc c :post_id post-id :order_id i))
            (recur rem (inc i)))))
    (resp/response {:post post-id})))

(defn get-post-by-id [req] nil)
(defn get-post-by-name [req] nil)
(defn get-recent-posts [req] nil)
(defn delete-post [req] nil)
