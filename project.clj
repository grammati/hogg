(defproject com.rallydev./hogg "0.1.0-SNAPSHOT"
  
  :description "Hogg is the Boss"
  :url "http://github.com/RallySoftware/hogg.git"
  
  :license "MIT License"
  
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/core.async "0.1.242.0-44b1e3-alpha"]
                 [http-kit "2.1.13"]
                 [http.async.client "0.5.2"]
                 [cheshire "5.2.0"]]

  :profiles
  {:dev {:source-paths ["dev"]
         :dependencies [[org.clojure/tools.namespace "0.2.4"]
                        [org.clojure/tools.trace "0.7.6"]]}})
