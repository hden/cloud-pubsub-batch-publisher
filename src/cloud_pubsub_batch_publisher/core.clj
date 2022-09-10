(ns cloud-pubsub-batch-publisher.core
  (:require [cognitect.anomalies :as anomalies])
  (:import
    [java.io ByteArrayInputStream]
    [java.util.concurrent TimeUnit]
    [com.google.api.gax.core FixedCredentialsProvider]
    [com.google.auth.oauth2 ServiceAccountCredentials]
    [com.google.protobuf ByteString]
    [com.google.pubsub.v1 PublishRequest PubsubMessage]
    [com.google.cloud.pubsub.v1 Publisher]))

;; ## Protocols & Records ##
;; You could mock the protocol for unit-tests
;; See https://github.com/duct-framework/duct/wiki/Boundaries
(defprotocol PublisherImpls
  (publish-impl! [publisher arg-map])
  (shutdown-impl! [publisher arg-map]))

(defrecord Boundary [publisher stub topic shutdown])

;; Utils
(defn- private-field [obj ^String field-name]
  (let [m (.. obj getClass (getDeclaredField field-name))]
    (. m (setAccessible true))
    (. m (get obj))))

(defn- pubsub-message [{:keys [message metadata ordering-key]}]
  (let [builder (PubsubMessage/newBuilder)]
    (cond-> builder
      message (.setData (ByteString/copyFromUtf8 message))
      metadata (.putAllAttributes metadata)
      ordering-key (.setOrderingKey ordering-key))
    (.build builder)))

(defn- pubsub-request [topic messages]
  (let [message-list (map pubsub-message messages)
        builder (PublishRequest/newBuilder)]
    (doto builder
      (.setTopic topic)
      (.addAllMessages message-list))
    (.build builder)))

(defn- fixed-credentials-provider [^String s]
  (let [stream (new ByteArrayInputStream (.getBytes s))
        credentials (ServiceAccountCredentials/fromStream stream)]
    (FixedCredentialsProvider/create credentials)))

;; Implementations
(extend-protocol PublisherImpls
  Boundary
  (publish-impl! [{:keys [stub topic shutdown]} {:keys [messages]}]
    (when (.get shutdown)
      (throw (ex-info "Cannot publish on a shut-down publisher."
                      {::anomalies/category ::anomalies/fault
                       ::anomalies/message  "Cannot publish on a shut-down publisher."})))
    (when (seq messages)
      (let [request (pubsub-request topic messages)
            response (-> stub
                       (.publishCallable)
                       (.futureCall request))]
        (into [] (.getMessageIdsList @response)))))

  (shutdown-impl! [{:keys [publisher shutdown]} {:keys [await-msec]}]
    (when-not (.get shutdown)
      (.shutdown publisher))
    (when await-msec
      (.awaitTermination publisher await-msec TimeUnit/MILLISECONDS))))

;; Public APIs
(defn publisher
  ([topic]
   (publisher topic {}))
  ([^String topic {:keys [channel-provider
                          credentials
                          credentials-provider
                          enable-message-ordering]
                   :or {enable-message-ordering true}}]
   (let [p (cond-> (Publisher/newBuilder topic)
             credentials             (.setCredentialsProvider (fixed-credentials-provider credentials))
             credentials-provider    (.setCredentialsProvider credentials-provider)
             channel-provider        (.setChannelProvider channel-provider)
             ;; In a single publish request, all messages must have no ordering key
             ;; or they must all have the same ordering key.
             ;; See https://cloud.google.com/pubsub/docs/ordering
             enable-message-ordering (.setEnableMessageOrdering true)
             true                    (.build))
         stub (private-field p "publisherStub")
         shutdown (private-field p "shutdown")]
     (->Boundary p stub topic shutdown))))

(defn publish!
  "Synchronously publish a collection of messages.
  Returns a list of message IDs."
  [publisher arg-map]
  (publish-impl! publisher arg-map))

(defn shutdown!
  "Synchronously shutdown the publisher."
  ([publisher]
   (shutdown! publisher {}))
  ([publisher arg-map]
   (shutdown-impl! publisher arg-map)))
