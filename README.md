# cloud-pubsub-batch-publisher

A batch publisher for Google Cloud PubSub.

## Usage

```clj
(import '[com.google.cloud.pubsub.v1 BatchPublisher])

(defn create-publisher [default-topic]
  (let [builder (BatchPublisher/newBuilder default-topic)]
    (.build builder)))

(def publisher (create-publisher (format "projects/%s/topics/%s" project topic)))

;; Publish to the default topic.
(let [response @(.publish publisher messages)]
  (.getMessageIdsList response))

;; Publish to another topic.
(let [response @(.publish publisher "projects/project/topics/another-topic" messages)]
  (.getMessageIdsList response))
```

## License

Copyright © 2019 Haokang Den
