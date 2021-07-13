(defproject hden/cloud-pubsub-batch-publisher "0.2.0-SNAPSHOT"
  :description "A batch publisher for Google Cloud PubSub."
  :url "http://example.com/FIXME"
  :license {:name "Apache-2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0.html"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [com.google.cloud/google-cloud-pubsub "1.82.0"]]
  :java-source-paths ["src/java"]
  :profiles {:uberjar {:aot :all}})
