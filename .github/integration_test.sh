#!/usr/bin/env bash
set -Euxo pipefail

cd "${BASH_SOURCE%/*}/.." || exit 1

export LEIN_JVM_OPTS="-Dclojure.main.report=stderr"
PROJECT_DIR="$PWD"
CONFIG_FILE="$PROJECT_DIR/.github/nvd-config.json"
DOGFOODING_CONFIG_FILE="$PROJECT_DIR/.github/nvd-dogfooding-config.json"
TOOLS_CONFIG_FILE="$PROJECT_DIR/.github/nvd-tool-config.json"
SUCCESS_REGEX="[1-9][0-9] vulnerabilities detected\. Severity: "

if ! lein with-profile -user,-dev,+ci install; then
  exit 1
fi

if ! clojure -Ttools install nvd-clojure/nvd-clojure '{:mvn/version "RELEASE"}' :as nvd; then
  exit 1
fi

cd "$PROJECT_DIR/example" || exit 1

# 1.- Exercise `main` program

example_classpath="$(lein with-profile -user,-dev,-test classpath)"

# cd to the root dir, so that one runs `defproject nvd-clojure` which is the most clean and realistic way to run `main`:
cd "$PROJECT_DIR" || exit 1

if lein with-profile -user,-dev,+ci run -m nvd.task.check "$CONFIG_FILE" "$example_classpath" > example-lein-output; then
  echo "Should have failed with non-zero code!"
  exit 1
fi

if ! grep --silent "$SUCCESS_REGEX" example-lein-output; then
  echo "Should have found vulnerabilities! (Step 1)"
  exit 1
fi

# 2.- Exercise `tools.deps` integration

cd "$PROJECT_DIR/example" || exit 1

example_classpath="$(clojure -Spath)"

# cd to the root dir, so that one runs `defproject nvd-clojure` which is the most clean and realistic way to run `main`:
cd "$PROJECT_DIR" || exit 1

if clojure -J-Dclojure.main.report=stderr -M -m nvd.task.check "$CONFIG_FILE" "$example_classpath" > example-lein-output; then
  echo "Should have failed with non-zero code!"
  exit 1
fi

if ! grep --silent "$SUCCESS_REGEX" example-lein-output; then
  echo "Should have found vulnerabilities! (Step 2)"
  exit 1
fi

# 3.- Exercise Clojure CLI Tools integration

cd "$PROJECT_DIR/example" || exit 1

example_classpath="$(clojure -Spath)"

# cd to $HOME, to demonstrate that the Tool does not depend on a deps.edn file:
cd || exit 1

if clojure -J-Dclojure.main.report=stderr -Tnvd nvd.task/check :classpath \""$example_classpath\"" :config-filename \""$TOOLS_CONFIG_FILE\"" > example-lein-output; then
  echo "Should have failed with non-zero code!"
  exit 1
fi

if ! grep --silent "$SUCCESS_REGEX" example-lein-output; then
  echo "Should have found vulnerabilities! (Step 3)"
  exit 1
fi

# 4.- Dogfood the `nvd-clojure` project

cd "$PROJECT_DIR" || exit 1

own_classpath="$(lein with-profile -user,-dev,-test classpath)"

if ! lein with-profile -user,-dev,+ci,+skip-self-check run -m nvd.task.check "$DOGFOODING_CONFIG_FILE" "$own_classpath"; then
  echo "nvd-clojure did not pass dogfooding!"
  exit 1
fi

exit 0
