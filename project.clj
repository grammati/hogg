(defproject com.rallydev./hogg "0.1.0-SNAPSHOT"
  
  :description "Hogg is the Boss"
  :url "http://github.com/RallySoftware/hogg.git"
  
  :license "MIT License"
  
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [enos "0.1.2"]
                 [http-kit "2.1.16"]
                 [http.async.client "0.6.0-SNAPSHOT"]
                 [cheshire "5.2.0"]]

  :jvm-opts ["-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"]
  
  :profiles
  {:dev {:source-paths ["dev"]
         :dependencies [[org.clojure/tools.namespace "0.2.4"]
                        [org.clojure/tools.trace "0.7.6"]
                        [clj-http "0.7.7"]
                        [ring-mock "0.1.5"]]}})
