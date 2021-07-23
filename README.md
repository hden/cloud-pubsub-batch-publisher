# cloud-pubsub-batch-publisher

A batch publisher for Google Cloud PubSub.

## Installation

## Usage

```clj
(require '[cloud-pubsub-batch-publisher.core :as pubsub])

(def publisher (pubsub/publisher "TOPIC-NAME")

;; In a single publish request, all messages must have no ordering key
;; or they must all have the same ordering key.
;; See https://cloud.google.com/pubsub/docs/ordering
(def messages
  [{:message      "MESSAGE"       ;; String, required
    :metadata     {"KEY" "VALUE"} ;; Map<String, String>, optional
    :ordering-key "KEY"}])        ;; String, optional

(pubsub/publish! publisher {:messages messages})

(pubsub/shutdown! publisher {:await-msec 1000})
```

## License

Copyright © 2021 Haokang Den
