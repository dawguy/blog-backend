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
         (def post-id (get-in req [:params :post-id]))
         ,)

;curl -X POST localhost:8888/post/save -d '{"text": "blog\ntest-draft-post\nTest Draft Post\n---\nsection-header\nHello world!\n---\ntext\nMy name is david.\n---\ntext\nMy online names are bloodisblue or dawguy.\n---\nimage\nabcdef.png\nA scorpion deer\n---\nindent\nThis is what an indented div looks like.\n---\nsection-header\nCode snippets\n---\ncode-clojure\n(prn (str \"I did\" (inc 2) \"lines here!\"))\n---\ncode-typescript\nconsole.out.println(\"Yoyo\");\nconsole.out.println(\"ABC\");\n---\ntext\nEnding with some text.\n---\n"}' -H 'Content-Type: application/json'
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

(defn get-post-by-name [req]
  (reset! a-req req)
  (let [name (get-in req [:params :post-url])
        db (get-in req [:application/component :database :datasource])
        post (d/get-post-by-name db name)
        contents (d/get-post-contents-by-id db (:post_id post))]
    (resp/response {:post post
                    :contents contents})
    ))
(defn get-post-by-id [req]
  (reset! a-req req)
  (let [post-id (get-in req [:params :post-id])
        db (get-in req [:application/component :database :datasource])
        post (d/get-post-by-id db post-id)
        contents (d/get-post-contents-by-id db post-id)]
    (resp/response {:post post
                    :contents contents})))

(defn get-recent-posts [req]
  (reset! a-req req)
  (let [db (get-in req [:application/component :database :datasource])
        type (get-in req [:params :type] nil)
        page (Integer. (get-in req [:params :page] 0))
        limit 10]
    (resp/response (d/get-recent-posts db {:limit limit, :page page, :type type}))))

(defn delete-post [req] nil)
