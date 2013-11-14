(ns hogg.server
  (:require [clojure.walk :refer [stringify-keys]]
            [org.httpkit.server :as server
             :refer [run-server with-channel on-receive on-close websocket? send! close]]
            [org.httpkit.client :as client]
            [http.async.client :as http]))


(defn proxy-websocket [request channel]
  (send! channel {:status 200 :body "websocket"} true))

(defn proxy-http [request downstream-server]
  (let [url (str downstream-server (:uri request))]
    (println "proxying to " url)
    @(client/request {:method  (:request-method request)
                     :url     url
                     :headers (:headers request)
                     :body    (:body request)
                     :timeout 5000}
                    (fn [{:keys [status headers body error] :as response}]
                      (if error
                        {:status 500 :body "Proxy error"}
                        {:status status :headers (stringify-keys headers) :body body})))))

(defn build-router [[config & configs]]
  (if config
    (let [next (build-router configs)]
      (fn [req]
        (println "Checking " config)
        (if (some #(= (:server-name req) %) (:hosts config))
          (proxy-http req (first (:downstream config)))
          (next req))))
    (fn [req]
      {:status 404 :body "Unknown host"})))

(defn app [request]
  (with-channel request channel
    (println "Connected: " channel)
    (if (websocket? channel)
      (proxy-websocket request channel)
      (proxy-http request channel))))

(defn start [configs]
  (run-server (build-router configs) {:port 8080}))


