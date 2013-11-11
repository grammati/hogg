(ns hogg.server
  (:require [clojure.walk :refer [stringify-keys]]
            [org.httpkit.server :as server
             :refer [run-server with-channel on-receive on-close websocket? send! close]]
            [org.httpkit.client :as client]
            [http.async.client :as http]))


(defn downstream-url [request]
  (str "http://localhost:8999" (:uri request)))

(defn proxy-websocket [request channel]
  (send! channel {:status 200 :body "websocket"} true))

(defn proxy-http [request channel]
  (let [url (downstream-url request)]
    (client/request {:method  (:request-method request)
                     :url     (downstream-url request)
                     :headers (:headers request)
                     :body    (:body request)
                     :timeout 5000}
                    (fn [{:keys [status headers body error] :as response}]
                      (if error
                        (send! channel {:status 500 :body "Proxy error"})
                        (send! channel {:status status :headers (stringify-keys headers) :body body}))))))

(defn app [request]
  (clojure.pprint/pprint request)
  (with-channel request channel
    (println "Connected: " channel)
    (if (websocket? channel)
      (proxy-websocket request channel)
      (proxy-http request channel))))

(defn start []
  (run-server #'app {:port 8080}))


