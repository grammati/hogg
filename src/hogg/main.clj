(ns hogg.main
  (:require [hogg.server])
  (:gen-class))

(defn -main [& args]
  (hogg.system/start))


