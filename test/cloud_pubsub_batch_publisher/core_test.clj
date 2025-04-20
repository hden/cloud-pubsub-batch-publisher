(ns cloud-pubsub-batch-publisher.core-test
  (:require [clojure.test :as t :refer [deftest is testing use-fixtures do-report]]
            [cognitect.anomalies :as anomalies]
            [cloud-pubsub-batch-publisher.core :as core]
            [cloud-pubsub-batch-publisher.emulator :as emulator]
            [cuid.core :refer [cuid]]))

(def ^:const topic "projects/test-project/topics/test-topic")
(def context (emulator/context {}))

(use-fixtures :once (emulator/create-fixture topic context))

;; https://clojureverse.org/t/testing-thrown-ex-info-exceptions/6146/3
(defmethod t/assert-expr 'thrown-with-data? [msg form]
  (let [data (second form)
        body (nthnext form 2)]
    `(try ~@body
          (do-report {:type :fail, :message ~msg,
                      :expected '~form, :actual nil})
          (catch clojure.lang.ExceptionInfo e#
            (let [expected# ~data
                  actual# (ex-data e#)]
              (if (= expected# actual#)
                (do-report {:type :pass, :message ~msg,
                            :expected expected#, :actual actual#})
                (do-report {:type :fail, :message ~msg,
                            :expected expected#, :actual actual#})))
            e#))))

(defn generate-messages []
  (let [key (cuid)]
    [{:message      "message-only"
      :ordering-key key}
     {:message      "message-with-metadata"
      :metadata     {"foo" "bar"}
      :ordering-key key}]))

(deftest core-tests
  (let [publisher (core/publisher topic context)]
    (testing "test fixtures"
      (is publisher))

    (testing "publish!"
      (let [messages (generate-messages)
            results (core/publish! publisher {:messages messages})]
        (is (coll? results))
        (is (= (count messages) (count results)))
        (is (every? string? results)))))

  (let [publisher (core/publisher topic context)]
    (testing "shutdown!"
      (core/shutdown! publisher {:await-msec 10})
      (is (= true (.get (:shutdown publisher)))))

    (testing "publish! after shutdown!"
      (is (thrown-with-data?
            {::anomalies/category ::anomalies/fault
             ::anomalies/message  "Cannot publish on a shut-down publisher."}
            (core/publish! publisher {:messages (generate-messages)}))))))
