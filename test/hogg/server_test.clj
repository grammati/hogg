(ns hogg.server-test
  (:require [clojure.test :refer :all]
            [hogg.server :as server]
            [org.httpkit.server :refer [run-server]]
            [clj-http.client :as client]
            [cheshire.core :as json]
            [ring.mock.request :as mock])
  (:import [java.net InetAddress]))


(defn hostname []
  (.getHostName (InetAddress/getLocalHost)))

(def foo-service-config
  {:hosts      ["localhost"]
   :downstream ["http://localhost:8999"]})

(def bar-service-config
  {:hosts [(hostname)]
   :downstream ["http://localhost:8998"]})

(def services (atom nil))

(defn start-json-service [name port]
  (let [stopper
        (run-server (fn [req]
                      {:status 200
                       :headers {"Content-Type" "application/json"}
                       :body    (json/encode {:service name
                                              :path    (:uri req)})})
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


(deftest test-proxy
  (let [get (fn [url]
              (:body (client/get url {:timeout 500 :as :json})))]
    (is (= {:service "foo"
            :path    "/blah"} (get "http://localhost:8080/blah")))
    (is (= {:service "bar"
            :path    "/flup"} (get (str "http://" (hostname) ":8080/flup"))))))

