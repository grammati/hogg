(ns hogg.server-test
  (:require [clojure.test :refer :all]
            [hogg.server :as server]
            [org.httpkit.server :refer [run-server]]
            [cheshire.core :as json])
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
    (swap! services assoc name #(stopper :timeout 100))))

(defn stop-services []
  (doseq [[name f] @services]
    (f))
  (reset! services nil))

(defn with-services* [f]
  (try
    (start-json-service "foo" 8999)
    (start-json-service "bar" 8998)
    (f)
    (finally
      (stop-services))))

(use-fixtures :once with-services*)

(deftest test-proxy
  (is (= 0 1 "I'm too tired to write any tests today")))
