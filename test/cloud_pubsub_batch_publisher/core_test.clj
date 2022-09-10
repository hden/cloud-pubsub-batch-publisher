(ns cloud-pubsub-batch-publisher.core-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [cognitect.anomalies :as anomalies]
            [cloud-pubsub-batch-publisher.core :as core]
            [cloud-pubsub-batch-publisher.test-util :as util]
            [cuid.core :refer [cuid]]))

(use-fixtures :once util/shared-channel)

(defn generate-messages []
  (let [key (cuid)]
    [{:message      "message-only"
      :ordering-key key}
     {:message      "message-with-metadata"
      :metadata     {"foo" "bar"}
      :ordering-key key}]))

(deftest core-tests
  (let [publisher (util/publisher)]
    (testing "test fixtures"
      (is publisher))

    (testing "publish!"
      (let [messages (generate-messages)
            results (core/publish! publisher {:messages messages})]
        (is (coll? results))
        (is (= (count messages) (count results)))
        (is (every? string? results)))))

  (let [publisher (util/publisher)]
    (testing "shutdown!"
      (core/shutdown! publisher {:await-msec 10})
      (is (= true (.get (:shutdown publisher)))))

    (testing "publish! after shutdown!"
      (is (thrown-with-data?
            {::anomalies/category ::anomalies/fault
             ::anomalies/message  "Cannot publish on a shut-down publisher."}
            (core/publish! publisher {:messages (generate-messages)}))))))
