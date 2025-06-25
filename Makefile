.PHONY: check-env

# Example usage:
# copy a one-off Clojars token to your clipboard
# GIT_TAG=v5.1.0 CLOJARS_USERNAME=$USER CLOJARS_PASSWORD=$(pbpaste) make deploy

deploy: check-env
	lein clean
	lein with-profile -user,-dev,+ci deploy clojars
	git tag -a "$$GIT_TAG" -m "$$GIT_TAG"
	git push
	git push --tags

install:
	lein with-profile -user,-dev,+ci install
	clojure -Ttools install nvd-clojure/nvd-clojure '{:mvn/version "RELEASE"}' :as nvd

check-env:
ifndef CLOJARS_USERNAME
	$(error CLOJARS_USERNAME is undefined)
endif
ifndef CLOJARS_PASSWORD
	$(error CLOJARS_PASSWORD is undefined)
endif
ifndef GIT_TAG
	$(error GIT_TAG is undefined)
endif
