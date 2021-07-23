(defproject hden/cloud-pubsub-batch-publisher "1.0.0-SNAPSHOT"
  :description "A batch publisher for Google Cloud PubSub."
  :url "https://github.com/hden/cloud-pubsub-batch-publisher"
  :license {:name "Apache-2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0.html"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [com.cognitect/anomalies "0.1.12"]
                 [com.google.cloud/google-cloud-pubsub "1.113.5"]]
  :profiles {:uberjar {:aot :all}
             :dev {:plugins [[lein-dotenv "RELEASE"]]}})
