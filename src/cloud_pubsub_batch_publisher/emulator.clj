(ns cloud-pubsub-batch-publisher.emulator
  (:import
    [io.grpc ManagedChannelBuilder]
    [com.google.api.gax.core NoCredentialsProvider]
    [com.google.api.gax.grpc GrpcTransportChannel]
    [com.google.api.gax.retrying RetrySettings]
    [com.google.api.gax.rpc FixedTransportChannelProvider]
    [com.google.cloud.pubsub.v1 TopicAdminClient TopicAdminSettings]))

(defn- channel
  "Creates a gRPC channel for connecting to the PubSub emulator.

   This function establishes a connection to the PubSub emulator using the provided
   host and port, or falls back to environment variables and defaults.

   Args:
     opts: A map containing:
       :host - (optional) The hostname of the emulator
              (defaults to PUBSUB_EMULATOR_HOST or 'localhost')
       :port - (optional) The port number of the emulator
              (defaults to PUBSUB_EMULATOR_PORT or 8085)

   Returns:
     A ManagedChannel instance connected to the emulator

   Example:
     (channel {:host \"localhost\" :port 8085})"
  [{:keys [host port]}]
  (let [host (or host (System/getenv "PUBSUB_EMULATOR_HOST") "localhost")
        port (or port (System/getenv "PUBSUB_EMULATOR_PORT") 8085)]
    (.. (ManagedChannelBuilder/forTarget (str host ":" port))
        (usePlaintext)
        (build))))

(defn- channel-provider
  "Creates a transport channel provider from a gRPC channel.

   This function wraps a gRPC channel into a transport channel provider
   that can be used by the PubSub client.

   Args:
     channel - A ManagedChannel instance

   Returns:
     A FixedTransportChannelProvider instance"
  [channel]
  (-> (GrpcTransportChannel/create channel)
      (FixedTransportChannelProvider/create)))

(defn- credentials-provider
  "Creates a no-credentials provider for the emulator.

   Since the emulator doesn't require authentication, this function
   returns a NoCredentialsProvider instance.

   Returns:
     A NoCredentialsProvider instance"
  []
  (NoCredentialsProvider/create))

(def ^{:private true} retry-settings
  "Default retry settings for the emulator.

   These settings are used to configure retry behavior for operations
   that may fail temporarily."
  (.build (RetrySettings/newBuilder)))

(defn create-topic!
  "Creates a topic in the PubSub emulator if it doesn't already exist.

   This function attempts to create a new topic in the emulator. If the topic
   already exists, it will silently return nil.

   Args:
     context: A map containing:
       :channel-provider - A transport channel provider
       :credentials-provider - A credentials provider
     topic: The full topic name (e.g. 'projects/test-project/topics/test-topic')

   Returns:
     The created topic or nil if it already exists

   Example:
     (create-topic! context \"projects/test-project/topics/test-topic\")"
  [topic {:keys [channel-provider credentials-provider]}]
  (let [builder (TopicAdminSettings/newBuilder)]
    (doto builder
      (.setTransportChannelProvider channel-provider)
      (.setCredentialsProvider credentials-provider)
      (.. (createTopicSettings) (setRetrySettings retry-settings)))
    (try
      (.. (TopicAdminClient/create (.build builder))
          (createTopic topic))
      (catch com.google.api.gax.rpc.AlreadyExistsException _))))

(defn context
  "Creates a context map for PubSub emulator operations.

   This function sets up the necessary configuration for interacting
   with the PubSub emulator, including channel and credentials providers.
   The created context includes the raw channel instance which can be used
   for cleanup operations.

   Args:
     opts: A map containing:
       :host - (optional) The hostname of the emulator
              (defaults to PUBSUB_EMULATOR_HOST or 'localhost')
       :port - (optional) The port number of the emulator
              (defaults to PUBSUB_EMULATOR_PORT or 8085)

   Returns:
     A map containing:
       :channel - The raw ManagedChannel instance
       :channel-provider - A configured transport channel provider
       :credentials-provider - A no-credentials provider for the emulator

   Example:
     (context {:host \"localhost\" :port 8085})"
  [opts]
  (let [channel (channel opts)]
    {:channel channel
     :channel-provider (channel-provider channel)
     :credentials-provider (credentials-provider)}))

(defn create-fixture
  "Creates a test fixture for managing a PubSub topic during testing.

   This function returns a fixture function that:
   1. Creates the specified topic before running the test
   2. Executes the test function
   3. Shuts down the channel after the test completes

   Args:
     topic: The full topic name (e.g. 'projects/test-project/topics/test-topic')
     context: A context map created by the context function

   Returns:
     A function that takes a test function and returns a new function
     that manages the topic lifecycle around the test execution

   Example:
     (use-fixtures :once (create-fixture \"projects/test-project/topics/test-topic\" context))"
  [topic context]
  (fn [f]
    (let [channel (:channel context)]
      (try
        (create-topic! topic context)
        (f)
        (finally
          (.shutdown channel))))))
