(ns hogg.system
  (:require [hogg.server]))


(defn system []
  {::server {:start (fn [system]
                      (when-let [stopper (hogg.server/start [])]
                        (update-in system [::server] assoc
                                   :stop-fn  #(stopper :timeout 1000)
                                   :started? true)))
             :stop (fn [system]
                     (when-let [stop-fn (get-in system [::server :stop-fn])]
                       (stop-fn)
                       (update-in system [::server] dissoc :stop-fn :started?)))}})

(defn- apply-to-system [key system]
  (loop [system system
         [[name component] & more] (seq system)]
    (if component
      (recur ((key component identity) system) more)
      system)))

(def start (partial apply-to-system :start))

(def stop (partial apply-to-system :stop))
