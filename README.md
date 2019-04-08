# cloud-pubsub-batch-publisher

A batch publisher for Google Cloud PubSub.

## Usage

```clj
(ns foobar
  (:import [com.google.cloud.pubsub.v1 BatchPublisher]))

(def publisher (BatchPublisher. "defaultTopicName"))

;; Publish to the default topic.
(.publish publisher messages)
;; Publish to the specified topic.
(.publish publisher "topic" messages)

;; Wait for compilation.
(def future (.publish publisher messages))
@future
```

## License

Copyright © 2019 Haokang Den

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
