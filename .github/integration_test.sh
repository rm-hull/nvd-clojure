#!/usr/bin/env bash
set -Euxo pipefail

cd "${BASH_SOURCE%/*}/.." || exit 1

export LEIN_JVM_OPTS="-Dclojure.main.report=stderr"
PROJECT_DIR="$PWD"

CONFIG_FILE="$PROJECT_DIR/.github/nvd-config.edn"
CONFIG_FILE_USING_DEFAULT_FILENAME="$PROJECT_DIR/nvd-clojure.edn"
DOGFOODING_CONFIG_FILE="$PROJECT_DIR/.github/nvd-dogfooding-config.edn"
TOOLS_CONFIG_FILE="$PROJECT_DIR/.github/nvd-tool-config.edn"

JSON_CONFIG_FILE="$PROJECT_DIR/.github/nvd-config.json"
JSON_DOGFOODING_CONFIG_FILE="$PROJECT_DIR/.github/nvd-dogfooding-config.json"
JSON_TOOLS_CONFIG_FILE="$PROJECT_DIR/.github/nvd-tool-config.json"

A_CUSTOM_CHANGE=":a-custom-change"
SUCCESS_REGEX="[1-9][0-9] vulnerabilities detected\. Severity: "

if ! lein with-profile -user,-dev,+ci install; then
  exit 1
fi

if ! clojure -Ttools install nvd-clojure/nvd-clojure '{:mvn/version "RELEASE"}' :as nvd; then
  exit 1
fi

# 1.- Exercise `main` program (EDN)

cd "$PROJECT_DIR/example" || exit 1

example_classpath="$(lein with-profile -user,-dev,-test classpath)"

# cd to the root dir, so that one runs `defproject nvd-clojure` which is the most clean and realistic way to run `main`:
cd "$PROJECT_DIR" || exit 1

if lein with-profile -user,-dev,+ci run -m nvd.task.check "$CONFIG_FILE" "$example_classpath" > example-lein-output; then
  echo "Should have failed with non-zero code!"
  exit 1
fi

if ! grep --silent "$SUCCESS_REGEX" example-lein-output; then
  echo "Should have found vulnerabilities! (Step 1 - EDN)"
  exit 1
fi

if grep --silent "$A_CUSTOM_CHANGE" example-lein-output; then
  echo "$CONFIG_FILE and $CONFIG_FILE_USING_DEFAULT_FILENAME should have different contents!"
  exit 1
fi

if grep --silent "$A_CUSTOM_CHANGE" "$CONFIG_FILE"; then
  echo "$CONFIG_FILE and $CONFIG_FILE_USING_DEFAULT_FILENAME should have different contents!"
  exit 1
fi

# 1.- Exercise `main` program (EDN; implicitly using the default filename)

cd "$PROJECT_DIR/example" || exit 1

example_classpath="$(lein with-profile -user,-dev,-test classpath)"

# cd to the root dir, so that one runs `defproject nvd-clojure` which is the most clean and realistic way to run `main`:
cd "$PROJECT_DIR" || exit 1

if lein with-profile -user,-dev,+ci run -m nvd.task.check "" "$example_classpath" > example-lein-output 2>&1; then
  echo "Should have failed with non-zero code!"
  exit 1
fi

if ! grep --silent "$SUCCESS_REGEX" example-lein-output; then
  echo "Should have found vulnerabilities! (Step 1 - EDN - default filename)"
  exit 1
fi

if ! grep --silent "$A_CUSTOM_CHANGE" example-lein-output; then
  echo "Passing an empty string as the config name should result in the config having the default filename being used!"
  exit 1
fi

if ! grep --silent "$A_CUSTOM_CHANGE" "$CONFIG_FILE_USING_DEFAULT_FILENAME"; then
  echo "Passing an empty string as the config name should not result in the config file being overriden!"
  exit 1
fi

# 1.- Exercise `main` program (JSON)

cd "$PROJECT_DIR/example" || exit 1

example_classpath="$(lein with-profile -user,-dev,-test classpath)"

# cd to the root dir, so that one runs `defproject nvd-clojure` which is the most clean and realistic way to run `main`:
cd "$PROJECT_DIR" || exit 1

if lein with-profile -user,-dev,+ci run -m nvd.task.check "$JSON_CONFIG_FILE" "$example_classpath" > example-lein-output; then
  echo "Should have failed with non-zero code!"
  exit 1
fi

if ! grep --silent "$SUCCESS_REGEX" example-lein-output; then
  echo "Should have found vulnerabilities! (Step 1 - JSON)"
  exit 1
fi

# 2.- Exercise `tools.deps` integration (EDN)

cd "$PROJECT_DIR/example" || exit 1

example_classpath="$(clojure -Spath)"

# cd to the root dir, so that one runs `defproject nvd-clojure` which is the most clean and realistic way to run `main`:
cd "$PROJECT_DIR" || exit 1

if clojure -J-Dclojure.main.report=stderr -M -m nvd.task.check "$CONFIG_FILE" "$example_classpath" > example-lein-output; then
  echo "Should have failed with non-zero code!"
  exit 1
fi

if ! grep --silent "$SUCCESS_REGEX" example-lein-output; then
  echo "Should have found vulnerabilities! (Step 2 - EDN)"
  exit 1
fi

# 2.- Exercise `tools.deps` integration (JSON)

cd "$PROJECT_DIR/example" || exit 1

example_classpath="$(clojure -Spath)"

# cd to the root dir, so that one runs `defproject nvd-clojure` which is the most clean and realistic way to run `main`:
cd "$PROJECT_DIR" || exit 1

if clojure -J-Dclojure.main.report=stderr -M -m nvd.task.check "$JSON_CONFIG_FILE" "$example_classpath" > example-lein-output; then
  echo "Should have failed with non-zero code!"
  exit 1
fi

if ! grep --silent "$SUCCESS_REGEX" example-lein-output; then
  echo "Should have found vulnerabilities! (Step 2 - JSON)"
  exit 1
fi

# 3.- Exercise Clojure CLI Tools integration (EDN)

cd "$PROJECT_DIR/example" || exit 1

example_classpath="$(clojure -Spath)"

# cd to $HOME, to demonstrate that the Tool does not depend on a deps.edn file:
cd || exit 1

if clojure -J-Dclojure.main.report=stderr -Tnvd nvd.task/check :classpath \""$example_classpath\"" :config-filename \""$TOOLS_CONFIG_FILE\"" > example-lein-output; then
  echo "Should have failed with non-zero code!"
  exit 1
fi

if ! grep --silent "$SUCCESS_REGEX" example-lein-output; then
  echo "Should have found vulnerabilities! (Step 3 - EDN)"
  exit 1
fi

# 3.- Exercise Clojure CLI Tools integration (JSON)

cd "$PROJECT_DIR/example" || exit 1

example_classpath="$(clojure -Spath)"

# cd to $HOME, to demonstrate that the Tool does not depend on a deps.edn file:
cd || exit 1

if clojure -J-Dclojure.main.report=stderr -Tnvd nvd.task/check :classpath \""$example_classpath\"" :config-filename \""$JSON_TOOLS_CONFIG_FILE\"" > example-lein-output; then
  echo "Should have failed with non-zero code!"
  exit 1
fi

if ! grep --silent "$SUCCESS_REGEX" example-lein-output; then
  echo "Should have found vulnerabilities! (Step 3 - JSON)"
  exit 1
fi

# 4.- Dogfood the `nvd-clojure` project (EDN)

cd "$PROJECT_DIR" || exit 1

own_classpath="$(lein with-profile -user,-dev,-test classpath)"

if ! lein with-profile -user,-dev,+ci,+skip-self-check run -m nvd.task.check "$DOGFOODING_CONFIG_FILE" "$own_classpath"; then
  echo "nvd-clojure did not pass dogfooding! (EDN)"
  exit 1
fi

# 4.- Dogfood the `nvd-clojure` project (JSON)

cd "$PROJECT_DIR" || exit 1

own_classpath="$(lein with-profile -user,-dev,-test classpath)"

if ! lein with-profile -user,-dev,+ci,+skip-self-check run -m nvd.task.check "$JSON_DOGFOODING_CONFIG_FILE" "$own_classpath"; then
  echo "nvd-clojure did not pass dogfooding! (JSON)"
  exit 1
fi

exit 0
