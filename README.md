# cloud-pubsub-batch-publisher [![CircleCI](https://dl.circleci.com/status-badge/img/gh/hden/cloud-pubsub-batch-publisher/tree/master.svg?style=svg)](https://dl.circleci.com/status-badge/redirect/gh/hden/cloud-pubsub-batch-publisher/tree/master)

A Clojure library for batch publishing messages to Google Cloud PubSub with support for message ordering.

## Table of Contents

- [Installation](#installation)
- [Usage](#usage)
- [Features](#features)
- [License](#license)

## Installation

Add the following dependency to your `project.clj` or `deps.edn`:

```clj
[hden/cloud-pubsub-batch-publisher "1.1.1"]
```

## Usage

### Basic Usage

```clj
(require '[cloud-pubsub-batch-publisher.core :as pubsub])

;; Create a publisher instance
(def publisher (pubsub/publisher "YOUR-TOPIC-NAME"))

;; Define messages to publish
(def messages
  [{:message      "Hello, PubSub!"  ;; String, required
    :metadata     {"source" "app1"} ;; Map<String, String>, optional
    :ordering-key "group1"}])       ;; String, optional

;; Publish messages
(pubsub/publish! publisher {:messages messages})

;; Shutdown the publisher when done
(pubsub/shutdown! publisher {:await-msec 1000})
```

### Message Format

Each message in the batch must follow this structure:

- `:message` (required): The message content as a string
- `:metadata` (optional): A map of string key-value pairs for message attributes
- `:ordering-key` (optional): A string key for message ordering

> **Note**: In a single publish request, all messages must either:
> - Have no ordering key, or
> - Have the same ordering key
>
> See [Google Cloud PubSub ordering documentation](https://cloud.google.com/pubsub/docs/ordering) for more details.

## Features

- Batch message publishing to Google Cloud PubSub
- Support for message ordering
- Configurable message metadata
- Graceful shutdown handling

## License

Copyright © 2021 Haokang Den
