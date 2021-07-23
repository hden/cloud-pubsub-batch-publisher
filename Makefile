.PHONY: lint test

lint:
	clj-kondo --parallel --lint src test

test:
	lein test
