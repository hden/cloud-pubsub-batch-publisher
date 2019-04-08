(ns com.google.cloud.pubsub.test
  (:require [clojure.test :refer :all])
  (:import [com.google.cloud.pubsub.v1 BatchPublisher]))

(deftest batch-publisher
  (testing "constructor"
    (let [builder (BatchPublisher/newBuilder "topic")]
      (is publisher (.build builder)))))
