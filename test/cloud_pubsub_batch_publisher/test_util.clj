(ns cloud-pubsub-batch-publisher.test-util
  (:require [clojure.test :as t :refer [do-report]]
            [cloud-pubsub-batch-publisher.core :as core])
  (:import
    [io.grpc ManagedChannelBuilder]
    [com.google.api.gax.core NoCredentialsProvider]
    [com.google.api.gax.grpc GrpcTransportChannel]
    [com.google.api.gax.retrying RetrySettings]
    [com.google.api.gax.rpc FixedTransportChannelProvider]
    [com.google.cloud.pubsub.v1 TopicAdminClient TopicAdminSettings]))

(def ^:const topic "projects/test-project/topics/test-topic")
(def ^{:dynamic true :private true} *context*)

(defn channel
  [{:keys [host]
    :or {host (or (System/getenv "PUBSUB_EMULATOR_HOST")
                  "localhost:8085")}}]
  (.. (ManagedChannelBuilder/forTarget host)
      (usePlaintext)
      (build)))

(defn channel-provider
  [channel]
  (-> (GrpcTransportChannel/create channel)
      (FixedTransportChannelProvider/create)))

(defn credentials-provider
  []
  (NoCredentialsProvider/create))

(def retry-settings (.build (RetrySettings/newBuilder)))

(defn create-topic!
  []
  (let [builder (TopicAdminSettings/newBuilder)]
    (doto builder
      (.setTransportChannelProvider (:channel-provider *context*))
      (.setCredentialsProvider (:credentials-provider *context*))
      (.. (createTopicSettings) (setRetrySettings retry-settings)))
    (try
      (.. (TopicAdminClient/create (.build builder))
          (createTopic topic))
      (catch com.google.api.gax.rpc.AlreadyExistsException _))))

(defn publisher
  []
  (core/publisher topic {:channel-provider (:channel-provider *context*)
                         :credentials-provider (:credentials-provider *context*)
                         :enable-message-ordering true
                         :retry-settings retry-settings}))

(defn shared-channel
  [f]
  (let [ch (channel {})]
    (try
      (binding [*context* {:channel-provider (channel-provider ch)
                           :credentials-provider (credentials-provider)}]
        (create-topic!)
        (f))
      (catch Throwable ex
        (.shutdown ch)
        (throw ex)))))

;; https://clojureverse.org/t/testing-thrown-ex-info-exceptions/6146/3
(defmethod t/assert-expr 'thrown-with-data? [msg form]
  (let [data (second form)
        body (nthnext form 2)]
    `(try ~@body
          (do-report {:type :fail, :message ~msg,
                      :expected '~form, :actual nil})
          (catch clojure.lang.ExceptionInfo e#
            (let [expected# ~data
                  actual# (ex-data e#)]
              (if (= expected# actual#)
                (do-report {:type :pass, :message ~msg,
                            :expected expected#, :actual actual#})
                (do-report {:type :fail, :message ~msg,
                            :expected expected#, :actual actual#})))
            e#))))
