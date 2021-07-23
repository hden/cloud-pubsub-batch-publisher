.PHONY: lint test

lint:
	clj-kondo --parallel --lint src test

test:
	clojure -Adev:test:runner
