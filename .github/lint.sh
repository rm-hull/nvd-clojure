#!/usr/bin/env bash
set -Eeuxo pipefail

# 1.- lint nvd-clojure:

classpath="$(lein with-profile -user,+test classpath)"
# populate a clj-kondo cache per https://github.com/clj-kondo/clj-kondo/tree/4f1252748b128da6ea23033f14b2bec8662dc5fd#project-setup :
lein with-profile -user,+test,+clj-kondo run -m clj-kondo.main --lint "$classpath" --dependencies --parallel --copy-configs
lein with-profile -user,+test,+clj-kondo run -m clj-kondo.main --lint src test
lein eastwood
