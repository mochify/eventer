(defproject eventer "1.0.0"
  :description "The event firer for Leaf"
  :url "http://leaf.me"
  :license {:name "Proprietary?"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [cheshire            "5.3.1"]
                 [ring                "1.3.1"]
                 [ring/ring-json      "0.3.1"]
                 [compojure           "1.1.9"]
                 [com.taoensso/timbre "3.1.1"]
                 [org.clojure/core.match "0.2.1"]
                 [http-kit "2.1.16"]
                 [clojurewerkz/meltdown "1.1.0"]
                 [amazonica "0.2.29" :exclusions [joda-time]]]
  :plugins      [[lein-ring "0.8.11"]
                 [lein-environ "1.0.0"]]
  :ring {:handler eventer.routes/app})
