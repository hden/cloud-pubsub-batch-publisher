(ns cloud-pubsub-batch-publisher.test-util
  (:require [clojure.test :as t :refer [do-report]]))

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

(defn tap [x]
  (println "tap>" x)
  x)

(defn test-topic []
  (System/getenv "PUBSUB_TOPIC"))

(defmacro with-publisher [[bound-var binding-expr] & body]
  `(let [~bound-var (core/publisher ~binding-expr)]
     (try
       ~@body
       (finally (core/shutdown! ~bound-var {:await-msec 10})))))
