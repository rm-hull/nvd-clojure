#!/usr/bin/env bash
set -Euxo pipefail

CONFIG_FILE="$PWD/.github/nvd-config.json"
SUCCESS_REGEX="[1-9][0-9] vulnerabilities detected\. Severity: "

lein with-profile -user,-dev,+ci install
cd plugin || exit 1
lein with-profile -user,-dev,+ci install
cd .. || exit 1
cd example || exit 1

# 1.- Exercise Lein plugin

if lein with-profile -user nvd check > example-lein-output; then
  echo "Should have failed with non-zero code!"
  exit 1
fi

if ! grep --silent "$SUCCESS_REGEX" example-lein-output; then
  echo "Should have found vulnerabilities!"
  exit 1
fi

# 2.- Exercise `main` program

example_classpath="$(lein with-profile -user,-dev,-test classpath)"

# cd to the root dir, so that one runs `defproject nvd-clojure` which is the most clean and realistic way to run `main`:
cd .. || exit 1

if lein with-profile -user,-dev,+ci run -m nvd.task.check "$CONFIG_FILE" "$example_classpath" > example-lein-output; then
  echo "Should have failed with non-zero code!"
  exit 1
fi

if ! grep --silent "$SUCCESS_REGEX" example-lein-output; then
  echo "Should have found vulnerabilities!"
  exit 1
fi

# 3.- Exercise `tools.deps` integration

cd example || exit 1

example_classpath="$(clojure -Spath)"

# cd to the root dir, so that one runs `defproject nvd-clojure` which is the most clean and realistic way to run `main`:
cd .. || exit 1

if clojure -m nvd.task.check "$CONFIG_FILE" "$example_classpath" > example-lein-output; then
  echo "Should have failed with non-zero code!"
  exit 1
fi

if ! grep --silent "$SUCCESS_REGEX" example-lein-output; then
  echo "Should have found vulnerabilities!"
  exit 1
fi

exit 0
