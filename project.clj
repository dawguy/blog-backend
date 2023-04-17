(defproject blog-backend "0.1.0-SNAPSHOT"
  :description "Backend for dawguy's blog"
  :url "https://github.com/dawguy/Blog-Angular"
  :license {:name "MIT"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [com.github.seancorfield/next.jdbc "1.3.847"]
                 [com.stuartsierra/component "1.1.0"]
                 [compojure "1.7.0"]
                 [ring "1.9.6"]
                 [ring/ring-defaults "0.3.4"]
                 [ring/ring-json "0.5.1"]
                 [cheshire/cheshire "5.7.1"]]
  :repl-options {:init-ns blog-backend.main})
