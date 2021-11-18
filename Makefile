# CLOJARS_USERNAME=$USER CLOJARS_PASSWORD=$(pbpaste) make deploy
# Semicolons are used so that `cd` works.
deploy:
	lein clean; lein with-profile -user,-dev,+ci deploy clojars
	cd plugin; lein clean; lein with-profile -user,-dev,+ci deploy clojars
