(ns com.google.cloud.pubsub.v1
  (:import [com.google.api.core ApiFuture]
           [com.google.pubsub.v1 PublishRequest]
           [com.google.cloud.pubsub.v1 Publisher Publisher$Builder])
  (:gen-class
    :name com.google.cloud.pubsub.v1.BatchPublisher
    :extends com.google.cloud.pubsub.v1.Publisher
    :init init
    :constructors {[String] [com.google.cloud.pubsub.v1.Publisher$Builder]}
    :exposes {publisherStub {:get getStub :set setStub}
              topicName {:get getTopic :set setTopic}}
    :methods [[publish [java.util.List] com.google.api.core.ApiFuture]
              [publish [String java.util.List] com.google.api.core.ApiFuture]]))

(defn create-publish-request [topic coll]
  (let [builder (PublishRequest/newBuilder)]
    (doto builder
      (.setTopic topic)
      (.addAllMessages coll))
    (.build builder)))

(defn -init [topic]
  (let [builder (Publisher/newBuilder topic)]
    [[builder]]))

(defn -publish [this coll]
  (let [callable (.publishCallable (.getStub this))
        request (create-publish-request (.getTopic this) coll)]
    (.futureCall callable request)))

(defn -publish [this topic coll]
  (let [callable (.publishCallable (.getStub this))
        request (create-publish-request topic coll)]
    (.futureCall callable request)))
