(ns hogg.routing-test
  (:require [clojure.test :refer :all]
            [clojure.core.async :as async :refer [go go-loop put! take!]]
            [hogg.routing :as routing]
            [enos.core :as enos :refer [pause!]]
            [org.httpkit.server :as server :refer [run-server with-channel websocket? send!]]
            [org.httpkit.timer :as timer]
            ))


;;; Async ring

;;; How to do async ring?
;;; Option 1: pass along two channels - push into one of them to pass
;;; the request on to the next middleware/handler, and the other to
;;; respond (short-circuit).
;;; Option 2: request is a linear chain of stages (no
;;; call-then-return), with a request going in one end and a response
;;; coming out the other.
;;; Option 3: middleware must return a channel, and the framework will
;;; take from it. If the thing that comes out of the channel can be a
;;; request (presumably modified in some way) or a response. (how to
;;; tell them apart?)
;;; Option 4: ...?

;;; Requirements:
;;; Lots of concurrent requests
;;; Don't use up the core.async thread pool
;;; Should be easy to write middleware that:
;;;  - processes only the request
;;;  - processes only the response
;;;  - processes both
;;;  - short-circuits
;;; Should be able to return a channel to stream a response
;;; 


(defn async-handler
  "Returns a function that can handle a Ring request asynchronously.
  "
  [& stages]
  (fn [request]
    (with-channel request async-channel
      (let [ch (async/chan)]
        (go-loop [[stage & stages] stages
                  context          (assoc request :ch ch)]
          (if (contains? context :response)
            (send! async-channel (:response context))
            (if (not stage)
              (send! async-channel {:status 404 :body "Not found"})
              (recur stages (stage context)))))))))




(def authenticated?
  (thread-pooled-fn
   (fn [id] (call-db id))))

(defn wrap-auth
  "Auth-check middleware - classic Ring style, with blocking call to `authenticated?`"
  [handler]
  (fn [request]
    (let [sessionid (get-in request [:cookies :sessionid])]
      (if (authenticated? sessionid)
        (handler request)
        (resopnse/unauthorized)))))

(def auth-stage
  "Auth-check - async."
  {:req (fn [context]
          (async/thread
            (if (authenticated? (get-in context [:request :cookies :sessionid]))
              context
              (assoc context :response {:status 401 :body "Unauthorized"}))))})

(defn auth-stage
  "Auth-check - async."
  [context]
  (async/thread
    (if (authenticated? (get-in context [:request :cookies :sessionid]))
      context
      (assoc context :response {:status 401 :body "Unauthorized"}))))

(defrecord AuthStage []
  hogg.protocols.Stage
  (request [this context]
    etc))






(defn foo-stage [context]
  (assoc-in [:context :request] :foo "foo"))

(defn blocking-stage
  "Blocking request stage (eg: makes db-call)"
  [request]
  (async/thread))

(defn make-async-stage [key value delay]
  (fn [ch]))

(deftest foo-test
  (let [handler (async-handler foo-stage blocking-stage )]))

(defn check-auth [request]
  ;; Check whether the request is authenticated. This involves calling
  ;; out to an external process, an hence is asynchronous. If
  ;; authenticated, pass the request on to the next handler. If not,
  ;; responsd immediately with a 401.
  ;; This illustrates the API that a middleware author would have to
  ;; use, so it should be reasonbly simple and easy to understand.
  (go
    (pause! 500)      ; pretend it took some time to check auth status
    (if-let [userid (get-in request [:params :userid])]
      ;; Authenticated. Pass the request on for further processing.
      (continue (assoc request :user {:id userid}))
      ;; Not authenticated. Return a 401.
      (respond request {:status 401 :body "Unauthorized"})
      )))



(defn middleware-1 [context]
  (go
    (pause! 1000); Pretend we're doing something time-consuming
    (if (get-in request [:params :fail])
      (>! ret {:status 500 :body "ooops!"})
      (>! next (assoc request :foo "bar")))))


(defn middleware-2 [context]
  ())


(defn- foo [_] "FOO!")
(defn- bar [_] "BAR!")

(defn- slow [f ms]
  (fn [& args]
    (go
      (pause! ms)
      (apply f args))))

(defn do-routing [request next ret]
  (go
    (if-let [route-params (case (:uri request)
                            "/foo" {:handler (slow  500)}
                            "/bar" {:handler (slow bar 200)}
                            nil)]
      (>! next (assoc request :routing route-params))
      (>! ret {:status 404 :body "Not found"}))))




(def handler
  (async-handler middlleware-1 middleware-2 proxy-handler))
