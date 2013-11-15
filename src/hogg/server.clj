(ns hogg.server
  (:require [clojure.walk :refer [stringify-keys]]
            [clojure.core.async :as async :refer [go go-loop]]
            [org.httpkit.server :as server
             :refer [run-server with-channel on-receive on-close websocket? send! close]]
            [org.httpkit.client :as client]
            [http.async.client :as http :refer [websocket]])
  (:import [org.httpkit.server AsyncChannel]))


(def client (delay (http/create-client)))

(defn- send-handshake [^AsyncChannel channel key]
  (.sendHandshake channel
                  {"Upgrade"              "websocket"
                   "Connection"           "Upgrade"
                   "Sec-WebSocket-Accept" (server/accept key)}))

(defn proxy-websocket [config request]
  (let [downstream (rand-nth (filter #(.startsWith % "http:") (:downstream config)))
        url        (str downstream (:uri request))
        channel    (:async-channel request)
        key        (get-in request [:headers "sec-websocket-key"])]
    (if key
      (http/websocket
       @client url
       :text  (fn [soc t] (send! channel t))
       :byte  (fn [soc b] (send! channel b))
       :open  (fn [soc]   (send-handshake channel key))
       :close (fn [ws code reason] (close channel))
       :error (fn [ws ex] (close channel)))
      
      {:status 400 :body "Bad Sec-WebSocket-Key header"})

    ;; This return value tell http-kit that we are async
    {:body channel}))

(defn proxy-http [config request]
  (with-channel request channel
    (let [downstream-server (rand-nth (filter #(.startsWith % "http:") (:downstream config)))
          url               (str downstream-server (:uri request))]
      (println "proxying to " url)
      (client/request {:method  (:request-method request)
                       :url     url
                       :headers (:headers request)
                       :body    (:body request)
                       :timeout 5000}
                      (fn [{:keys [status headers body error] :as response}]
                        (println "Got response from downstream: " response)
                        (send! channel
                               (if error
                                 {:status 500 :body "Proxy error"}
                                 {:status status :headers (stringify-keys headers) :body body})))))))

(defn proxy-request [config request]
  (if (websocket? request)
    (proxy-websocket config request)
    (proxy-http config request)))

(defn matches? [config request]
  (and (some #(= (:server-name request) %) (:hosts config))
       (if (websocket? request)
         (some #(.startsWith % "ws:") (:downstream config))
         (some #(.startsWith % "http:") (:downstream config)))))

(defn build-router [[config & configs]]
  (if config
    (let [next (build-router configs)]
      (fn [request]
        (println "Checking " config)
        (if (matches? config request)
          (proxy-request config request)
          (next request))))
    (fn [req]
      {:status 404 :body "Unknown host"})))

(defn start [configs]
  (run-server (build-router configs) {:port 8080}))

