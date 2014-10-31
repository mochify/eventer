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
            ))

(timbre/refer-timbre)
(timbre/set-config! [:appenders :spit :enabled?] true)
(timbre/set-config! [:shared-appender-config :spit-filename] "/home/centos/eventer/logs/eventer.log")

(defn process-subscription-confirmation [subscribe-url]
  (let [{:keys [status headers body error] :as resp} @(http/get subscribe-url)]
    (do
      (info "Received request to subscribe to SNS channel at: " subscribe-url)
      (if error
        (info "Had an error confirming subscription: " error)
        (info "Successfully subscribed to channel: " status)))))

(defn process-notification [subject message]
  (do 
    (info "Subject: " subject)
    (info "Message: " message)
  "OK"))

(defn handle-rds-event [headers body] 
  (let [json-body (parse-string (slurp body) true)
        message-type (get headers "x-amz-sns-message-type")]
    (do
      (cond
        (= message-type "SubscriptionConfirmation") (process-subscription-confirmation (:SubscribeURL json-body))
        (= message-type "Notification") (process-notification (:Subject json-body) (:Message json-body))
        :else (info "No match"))
      "OK")))

(defroutes eventer-routes
  (POST "/rds-events" {:keys [headers body] :as request} (handle-rds-event headers body))
  (GET "/" [] "Hello")
  (route/not-found "Page not found")
  )

(def app
  (-> (handler/api eventer-routes)
      (wrap-json-body {:keyswords? true})
      (wrap-json-response)))
