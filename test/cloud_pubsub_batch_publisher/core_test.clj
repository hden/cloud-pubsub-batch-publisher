(ns cloud-pubsub-batch-publisher.core-test
  (:require [clojure.test :refer :all]
            [cloud-pubsub-batch-publisher.core :refer :all])
  (:import [com.google.cloud.pubsub.v1 BatchPublisher]))

(deftest utils
  (testing "create-publish-request"
    (let [request (create-publish-request "topic" [])]
      (is (= "topic" (.getTopic request))))))
