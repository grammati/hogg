(ns hogg.threadpool
  (:require [clojure.core.async :refer [chan put! close!]])
  (:import [java.util.concurrent Executors]))


(def ^:private executor
  (Executors/newCachedThreadPool))

(defn run-in-thread-pool [f]
  (.submit executor f))

(defn pooled-fn
  "Returns a function that, when called, will run `f` in a thread-pool
  and return a channel that will receive the return value of `f`. "
  [f]
  (fn [& args]
    (let [ch (chan)]
      (run-in-thread-pool
       (fn []
         (try
           (let [ret (apply f args)]
             (put! ch ret))
           (finally
             (close! ch)))))
      ch)))
