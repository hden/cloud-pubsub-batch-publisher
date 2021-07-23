(ns cloud-pubsub-batch-publisher.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [cognitect.anomalies :as anomalies]
            [cloud-pubsub-batch-publisher.core :as core]
            [cloud-pubsub-batch-publisher.test-util :as util]))

(def messages
  [{:message  "message-only"}
   {:message  "message-with-metadata"
    :metadata {"foo" "bar"}}])

(deftest core-tests
  (util/with-publisher [publisher (util/test-topic)]
    (testing "test fixtures"
      (is publisher))

    (testing "publish!"
      (let [results (core/publish! publisher {:messages messages})]
        (is (coll? results))
        (is (= (count messages) (count results)))
        (is (every? string? results)))))

  (util/with-publisher [publisher (util/test-topic)]
    (testing "shutdown!"
      (core/shutdown! publisher {:await-msec 10})
      (is (= true (.get (:shutdown publisher)))))

    (testing "publish! after shutdown!"
      (is (thrown-with-data?
            {::anomalies/category ::anomalies/fault
             ::anomalies/message  "Cannot publish on a shut-down publisher."}
            (core/publish! publisher {:messages messages}))))))
