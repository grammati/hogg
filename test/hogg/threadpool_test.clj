(ns hogg.threadpool-test
  (:require [clojure.test :refer :all]
            [hogg.threadpool :as tp]
            [clojure.core.async :as async :refer [<!!]]
            [enos.core :refer [dochan!]]))

(defn slow [f ms]
  (fn [& args]
    (Thread/sleep ms)
    (apply f args)))

(def foo (tp/pooled-fn ))

(deftest test-pooled-fn
  (let [chs (mapv (slow inc 100) (range 10))]
    (doseq [ch chs]
      (println (<!! ch)))))
