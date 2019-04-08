(defproject hden/cloud-pubsub-batch-publisher "0.1.0-SNAPSHOT"
  :description "A batch publisher for Google Cloud PubSub."
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [com.google.cloud/google-cloud-pubsub "1.69.0"]]
  :repl-options {:init-ns com.google.cloud.pubsub.v1}
  :source-paths      ["src/clojure"]
  :java-source-paths ["src/java"]
  :aot [com.google.cloud.pubsub.v1]
  :profiles {:uberjar {:aot :all}})
