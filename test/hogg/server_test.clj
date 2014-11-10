(ns hogg.server-test
  (:require [clojure.test :refer :all]
            [clojure.core.async :as async :refer [go-loop <!! >!! alts! alts!! alt! alt!!]]
            [clojure.walk]
            [hogg.server :as server]
            [org.httpkit.server :refer [run-server with-channel websocket? send!]]
            [org.httpkit.timer :as timer]
            [clj-http.client :as client]
            [http.async.client :as async-client :refer [websocket]]
            [cheshire.core :as json]
            [ring.mock.request :as mock])
  (:import [java.net InetAddress]))


(defn hostname []
  (.getHostName (InetAddress/getLocalHost)))

(def foo-service-config
  {:hosts      ["localhost"]
   :downstream ["http://localhost:8999"
                "ws://localhost:8999"]})

(def bar-service-config
  {:hosts [(hostname)]
   :downstream ["http://localhost:8998"]})

(def services (atom nil))

(defn json-service-handler [name]
  (fn [request]
    (with-channel request channel
      (let [body (json/encode {:service name
                               :path    (:uri request)})]
        (timer/schedule-task
         1
         (send! channel
                (if (websocket? channel)
                  body
                  {:status 200
                   :headers {"Content-Type" "application/json"}
                   :body    body}))
         )))))

(defn start-json-service [name port]
  (let [stopper (run-server (json-service-handler name)
                            {:port port})]
    (swap! services assoc name stopper)))

(defn start-proxy []
  (let [stopper (server/start [foo-service-config bar-service-config])]
    (swap! services assoc "main" stopper)))

(defn stop-services []
  (doseq [[name f] @services]
    (f))
  (reset! services nil))

(defn with-services* [f]
  (try
    (start-json-service "foo" 8999)
    (start-json-service "bar" 8998)
    (start-proxy)
    (Thread/sleep 500)
    (f)
    (finally
      (stop-services))))

(defmacro with-services [& body]
  `(with-services* (fn [] ~@body)))

(use-fixtures :once with-services*)

(def client (delay (async-client/create-client)))

(deftest test-proxy
  (let [get (fn [url]
              (:body (client/get url {:timeout 500 :as :json})))]
    (is (= {:service "foo"
            :path    "/blah"} (get "http://localhost:8080/blah")))
    (is (= {:service "bar"
            :path    "/flup"} (get (str "http://" (hostname) ":8080/flup"))))))

(defn ws-channels [url]
  (let [r-ch (async/chan)
        w-ch (async/chan)
        ws   (websocket @client url
                        :text (fn [soc t] (async/put! r-ch [:text t]))
                        :byte (fn [soc b] (async/put! r-ch [:bytes b]))
                        :open (fn [soc] (async/put! r-ch [:open soc]))
                        :close (fn [ws code reason] (async/put! r-ch [:close ws code reason]))
                        :error (fn [ws ex] (async/put! r-ch [:error ws ex]))
                        )]
    (go-loop [v (<! w-ch)]
      (if (nil? v)
        (async-client/close ws)
        (async-client/send ws (if (string? v) :text :bytes) v)))
    [r-ch w-ch]))

;;; FIXME - put this elsewhere, if it pans out:
(def eq-any (reify Object (equals [this other] true)))
(defmacro is-match [expected result]
  `(let [~'_ eq-any] (is (= ~expected ~result))))

(deftest test-proxy-websocket
  (let [[r w] (ws-channels "ws://localhost:8080/foo")]
    (is-match [[:open _] _]
              (alts!! [r (async/timeout 1000)]))
    (let [[[type data] chan] (alts!! [r (async/timeout 1000)])]
      (is (identical? r chan))
      (is (= :text type))
      (is (= {:service "foo" :path "/foo"} (json/decode data true))))
    (loop [n 10]
      (let [[[type data] chan] (alts!! [r (async/timeout 1000)])]
        (println "got: " type data)
        (when (and (= chan r) (pos? n))
          (recur (dec n)))))
    ))
