(defproject hden/cloud-pubsub-batch-publisher "1.1.2-SNAPSHOT"
  :description "A batch publisher for Google Cloud PubSub."
  :url "https://github.com/hden/cloud-pubsub-batch-publisher"
  :license {:name "Apache-2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0.html"}
  :dependencies [[org.clojure/clojure "1.12.0"]
                 [com.cognitect/anomalies "0.1.12"]
                 [com.google.cloud/google-cloud-pubsub "1.123.14"]]
  :profiles {:dev {:dependencies [[cuid/cuid "0.1.2"]]
                   :global-vars {*warn-on-reflection* true}}})
