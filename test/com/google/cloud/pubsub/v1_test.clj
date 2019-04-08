(ns com.google.cloud.pubsub.v1-test
  (:require [clojure.test :refer :all]
            [com.google.cloud.pubsub.v1 :refer :all])
  (:import [com.google.cloud.pubsub.v1 BatchPublisher]))

(deftest utils
  (testing "create-publish-request"
    (let [request (create-publish-request "topic" [])]
      (is (= "topic" (.getTopic request))))))
