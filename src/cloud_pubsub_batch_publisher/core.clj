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
  "Protocol defining the core operations for a PubSub publisher.

   This protocol provides an abstraction layer for publishing messages
   and managing publisher lifecycle. It can be extended for testing
   purposes or to implement custom publisher behavior.

   Implementations must provide:
   - publish-impl! - Publish messages to a topic
   - shutdown-impl! - Gracefully shutdown the publisher"
  (publish-impl! [publisher arg-map]
    "Publish messages to a topic.

     Args:
       publisher - The publisher instance
       arg-map - A map containing:
         :messages - A collection of message maps with keys:
           :message - The message content
           :metadata - (optional) Message attributes
           :ordering-key - (optional) Key for ordered delivery

     Returns:
       A collection of message IDs

     Throws:
       An exception if the publisher is already shut down")
  (shutdown-impl! [publisher arg-map]
    "Shutdown the publisher gracefully.

     Args:
       publisher - The publisher instance
       arg-map - A map containing:
         :await-msec - (optional) Time to wait for shutdown in milliseconds

     Returns:
       nil"))

;; Boundary record represents a PubSub publisher boundary.
;;
;; This record encapsulates the state and behavior of a Cloud PubSub publisher,
;; providing a clean interface for publishing messages and managing the publisher's
;; lifecycle.
;;
;; Fields:
;;   :publisher - The underlying Publisher instance
;;   :stub - The publisher stub for making RPC calls
;;   :topic - The topic name
;;   :shutdown - An AtomicBoolean indicating if the publisher is shut down
(defrecord Boundary [publisher stub topic shutdown])

;; Utils
(defn- private-field
  "Access a private field of an object using reflection.

   Args:
     obj - The object containing the private field
     field-name - The name of the private field

   Returns:
     The value of the private field"
  [obj ^String field-name]
  (let [m (.. obj getClass (getDeclaredField field-name))]
    (. m (setAccessible true))
    (. m (get obj))))

(defn- pubsub-message
  "Create a PubSub message from a map of attributes.

   Args:
     message-map - A map containing:
       :message - The message content
       :metadata - (optional) Message attributes
       :ordering-key - (optional) Key for ordered delivery

   Returns:
     A PubsubMessage instance"
  [{:keys [message metadata ordering-key]}]
  (let [builder (PubsubMessage/newBuilder)]
    (cond-> builder
      message (.setData (ByteString/copyFromUtf8 message))
      metadata (.putAllAttributes metadata)
      ordering-key (.setOrderingKey ordering-key))
    (.build builder)))

(defn- pubsub-request
  "Create a publish request for a batch of messages.

   Args:
     topic - The topic name
     messages - A collection of message maps

   Returns:
     A PublishRequest instance"
  [topic messages]
  (let [message-list (map pubsub-message messages)
        builder (PublishRequest/newBuilder)]
    (doto builder
      (.setTopic topic)
      (.addAllMessages message-list))
    (.build builder)))

(defn- fixed-credentials-provider
  "Create a credentials provider from a service account JSON string.

   Args:
     s - The service account JSON string

   Returns:
     A FixedCredentialsProvider instance"
  [^String s]
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
  "Create a new publisher instance for the given topic.

   This function creates a publisher that can be used to publish messages
   to a Cloud PubSub topic. The publisher supports message ordering and
   can be configured with custom credentials and channel providers.

   Args:
     topic - The full topic name (e.g. 'projects/test-project/topics/test-topic')
     opts - (optional) A map containing:
       :channel-provider - A custom channel provider
       :credentials - Service account JSON string
       :credentials-provider - A custom credentials provider
       :enable-message-ordering - Whether to enable message ordering (default: true)

   Returns:
     A Boundary record implementing the PublisherImpls protocol

   Example:
     (publisher \"projects/test-project/topics/test-topic\"
               {:credentials \"{\\\"type\\\": \\\"service_account\\\"...}\"})"
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
  "Synchronously publish a collection of messages to a topic.

   This function publishes messages in a batch and returns their IDs.
   All messages in a single batch must either have no ordering key
   or share the same ordering key.

   Args:
     publisher - A publisher instance
     arg-map - A map containing:
       :messages - A collection of message maps with keys:
         :message - The message content
         :metadata - (optional) Message attributes
         :ordering-key - (optional) Key for ordered delivery

   Returns:
     A collection of message IDs

   Throws:
     An exception if the publisher is already shut down

   Example:
     (publish! publisher {:messages [{:message \"Hello\"
                                    :metadata {\"key\" \"value\"}}]})"
  [publisher arg-map]
  (publish-impl! publisher arg-map))

(defn shutdown!
  "Synchronously shutdown the publisher.

   This function gracefully shuts down the publisher, optionally waiting
   for pending operations to complete.

   Args:
     publisher - A publisher instance
     arg-map - (optional) A map containing:
       :await-msec - Time to wait for shutdown in milliseconds

   Returns:
     nil

   Example:
     (shutdown! publisher {:await-msec 5000})"
  ([publisher]
   (shutdown! publisher {}))
  ([publisher arg-map]
   (shutdown-impl! publisher arg-map)))
