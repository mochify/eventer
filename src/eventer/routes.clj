(ns eventer.routes
  (:use ring.middleware.stacktrace)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [compojure.response :as response]
            [compojure.core :refer [defroutes GET POST DELETE ANY context]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [clojure.java.io :as io]
            [taoensso.timbre :as timbre]
            [cheshire.core :refer :all]
            [clojure.core.match :only (match)]
            [org.httpkit.client :as http]
            [clojure.string :as string]
            [clojurewerkz.meltdown.reactor :as mr]
            [clojurewerkz.meltdown.selectors :refer [$]]
            ))

(timbre/refer-timbre)
(timbre/set-config! [:appenders :spit :enabled?] true)
(timbre/set-config! [:shared-appender-config :spit-filename] "/home/centos/eventer/logs/eventer.log")

;(def reactor (mr/create))


;(defn restore-from-snapshot []
;  "OK")



;(mr/on reactor ($ "key") (fn [event] (comment "Do something")))

(defn process-subscription-confirmation [subscribe-url]
  (let [{:keys [status headers body error] :as resp} @(http/get subscribe-url)]
    (info "Received request to subscribe to SNS channel at: " subscribe-url)
    (if error
      (info "Had an error confirming subscription: " error)
      (info "Successfully subscribed to channel: " status))))

(defn process-notification [subject message]
  "Process a notification and a JSON-encoded message from SNS"
  (let [msg-map (parse-string message (fn [k] (keyword (string/lower-case (string/replace k " " "-")))))
        source-id (last (string/split (get msg-map :identifier-link) #" "))
        event-id (last (string/split (get msg-map :event-id) #"#"))]
    (info (string/join " " ["source-id:" source-id "event-id:" event-id]))
    "OK"))

(defn handle-rds-event [headers body] 
  (let [json-body (parse-string (slurp body) true)
        message-type (get headers "x-amz-sns-message-type")
        topic-arn (get headers "x-amz-sns-topic-arn")
        subscription-arn (get headers "x-amz-sns-subscription-arn")]
    (info "Headers: " headers)
    (cond
      (= message-type "SubscriptionConfirmation") (process-subscription-confirmation (:SubscribeURL json-body))
      (= message-type "Notification") (process-notification (:Subject json-body) (:Message json-body))
      :else (info "No match"))
    "OK"))

(defroutes eventer-routes
  (POST "/rds-events" {:keys [headers body] :as request} (handle-rds-event headers body))
  (GET "/" [] "Hello")
  (route/not-found "Page not found")
  )

(def app
  (-> (handler/api eventer-routes)
      (wrap-json-body {:keyswords? true})
      (wrap-json-response)))
