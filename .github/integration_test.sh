#!/usr/bin/env bash
set -Euxo pipefail

cd "${BASH_SOURCE%/*}/.." || exit 1

if [ -z "${NVD_API_TOKEN}" ]; then
  echo "NVD_API_TOKEN not set!"
  exit 1
fi

export LEIN_JVM_OPTS="-Dclojure.main.report=stderr"
PROJECT_DIR="$PWD"

CONFIG_FILE="$PROJECT_DIR/.github/nvd-config.edn"
CONFIG_FILE_USING_DEFAULT_FILENAME="$PROJECT_DIR/nvd-clojure.edn"
DOGFOODING_CONFIG_FILE="$PROJECT_DIR/.github/nvd-dogfooding-config.edn"
TOOLS_CONFIG_FILE="$PROJECT_DIR/.github/nvd-tool-config.edn"
DATAFEED_CONFIG_FILE="$PROJECT_DIR/.github/nvd-datafeed-config.edn"

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

# 1.- Exercise check via lein

cd "$PROJECT_DIR/example" || exit 1

lein_example_classpath="$(lein with-profile -user,-dev,-test classpath)"

# cd to the root dir, so that one runs `defproject nvd-clojure` which is the most clean and realistic way to run `main`:
cd "$PROJECT_DIR" || exit 1

# 1.1 - lein w/EDN config
step_name=">>> [Step 1.1 - lein & EDN]"

echo "$step_name starting..."

if lein with-profile -user,-dev,+ci run -m nvd.task.check "$CONFIG_FILE" "$lein_example_classpath" > test-output; then
  echo "$step_name Should have failed with non-zero code!"
  exit 1
fi

if ! grep --silent "$SUCCESS_REGEX" test-output; then
  echo "$step_name Should have found vulnerabilities!"
  exit 1
fi

if grep --silent "$A_CUSTOM_CHANGE" test-output; then
  echo "$step_name $CONFIG_FILE and $CONFIG_FILE_USING_DEFAULT_FILENAME should have different contents!"
  exit 1
fi

if grep --silent "$A_CUSTOM_CHANGE" "$CONFIG_FILE"; then
  echo "$step_name $CONFIG_FILE and $CONFIG_FILE_USING_DEFAULT_FILENAME should have different contents!"
  exit 1
fi

# 1.2 - Exercise `main` program (EDN; implicitly using the default filename by specifying the empty string)

step_name=">>> [Step 1.2 lein & EDN - default filename]"

echo "$step_name starting..."

if lein with-profile -user,-dev,+ci run -m nvd.task.check "" "$lein_example_classpath" > test-output 2>&1; then
  echo "$step_name Should have failed with non-zero code!"
  exit 1
fi

if ! grep --silent "$SUCCESS_REGEX" test-output; then
  echo "$step_name Should have found vulnerabilities!"
  exit 1
fi

if ! grep --silent "$A_CUSTOM_CHANGE" test-output; then
  echo "$step_name Passing an empty string as the config name should result in the config having the default filename being used!"
  exit 1
fi

if ! grep --silent "$A_CUSTOM_CHANGE" "$CONFIG_FILE_USING_DEFAULT_FILENAME"; then
  echo "$step_name Passing an empty string as the config name should not result in the config file being overriden!"
  exit 1
fi

# 1.3 - Exercise `main` program (EDN) with a datafeed
step_name=">>> [Step 1.3 lein & EDN - w/datafeed]"

echo "$step_name starting..."

if lein with-profile -user,-dev,+ci run -m nvd.task.check "$DATAFEED_CONFIG_FILE" "$lein_example_classpath" > test-output; then
  echo "$step_name Should have failed with non-zero code!"
  exit 1
fi

if ! grep --silent "$SUCCESS_REGEX" test-output; then
  echo "$step_name Should have found vulnerabilities!"
  exit 1
fi

# 1.4 - Exercise `main` program (JSON)

step_name=">>> [Step 1.4 lein & JSON]"

echo "$step_name starting..."

if lein with-profile -user,-dev,+ci run -m nvd.task.check "$JSON_CONFIG_FILE" "$lein_example_classpath" > test-output; then
  echo "$step_name Should have failed with non-zero code!"
  exit 1
fi

if ! grep --silent "$SUCCESS_REGEX" test-output; then
  echo "$step_name Should have found vulnerabilities!"
  exit 1
fi

# cd to the root dir, so that one runs `defproject nvd-clojure` which is the most clean and realistic way to run `main`:
cd "$PROJECT_DIR" || exit 1

# 2.- Exercise `tools.deps` integration

cd "$PROJECT_DIR/example" || exit 1

clojure_example_classpath="$(clojure -Spath)"

# cd to the root dir, so that one runs `defproject nvd-clojure` which is the most clean and realistic way to run `main`:
cd "$PROJECT_DIR" || exit 1

# 2.1 Exercise `tools.deps` integration (EDN)
step_name=">>> [Step 2.1 deps & EDN]"

echo "$step_name starting..."

if clojure -J-Dclojure.main.report=stderr -M -m nvd.task.check "$CONFIG_FILE" "$clojure_example_classpath" > test-output; then
  echo "$step_name Should have failed with non-zero code!"
  exit 1
fi

if ! grep --silent "$SUCCESS_REGEX" test-output; then
  echo "$step_name Should have found vulnerabilities!"
  exit 1
fi

# 2.2 - Exercise `tools.deps` integration (JSON)
step_name=">>> [Step 2.2 deps & JSON]"

echo "$step_name starting..."

if clojure -J-Dclojure.main.report=stderr -M -m nvd.task.check "$JSON_CONFIG_FILE" "$clojure_example_classpath" > test-output; then
  echo "$step_name Should have failed with non-zero code!"
  exit 1
fi

if ! grep --silent "$SUCCESS_REGEX" test-output; then
  echo "$step_name Should have found vulnerabilities!"
  exit 1
fi

# 3. - Exercise Clojure CLI Tools integration

cd "$PROJECT_DIR/example" || exit 1

clojure_example_classpath="$(clojure -Spath)"

# cd to $HOME, to demonstrate that the Tool does not depend on a deps.edn file:
cd || exit 1

# 3.1 - Exercise Clojure CLI Tools integration (EDN)
step_name=">>> [Step 3.1 clojure tool & EDN]"

echo "$step_name starting..."

if clojure -J-Dclojure.main.report=stderr -Tnvd nvd.task/check :classpath \""$clojure_example_classpath\"" :config-filename \""$TOOLS_CONFIG_FILE\"" > test-output; then
  echo "$step_name Should have failed with non-zero code!"
  exit 1
fi

if ! grep --silent "$SUCCESS_REGEX" test-output; then
  echo "$step_name Should have found vulnerabilities!"
  exit 1
fi

# 3.2 - Exercise Clojure CLI Tools integration (JSON)

step_name=">>> [Step 3.2 clojure tool & JSON]"

echo "$step_name starting..."

if clojure -J-Dclojure.main.report=stderr -Tnvd nvd.task/check :classpath \""$clojure_example_classpath\"" :config-filename \""$JSON_TOOLS_CONFIG_FILE\"" > test-output; then
  echo "$step_name Should have failed with non-zero code!"
  exit 1
fi

if ! grep --silent "$SUCCESS_REGEX" test-output; then
  echo "$step_name Should have found vulnerabilities!"
  exit 1
fi

# 4.- Dogfood the `nvd-clojure` project

cd "$PROJECT_DIR" || exit 1

own_classpath="$(lein with-profile -user,-dev,-test classpath)"

# 4.1 - Dogfood the `nvd-clojure` project (EDN)
#
step_name=">>> [Step 4.1 lein dogfooding & EDN]"

echo "$step_name starting..."

if ! lein with-profile -user,-dev,+ci,+skip-self-check run -m nvd.task.check "$DOGFOODING_CONFIG_FILE" "$own_classpath"; then
  echo "$step_name nvd-clojure did not pass dogfooding! (EDN)"
  exit 1
fi

# 4.2. - Dogfood the `nvd-clojure` project (JSON)

step_name=">>> [Step 4.2 lein dogfooding & JSON]"

echo "$step_name starting..."

if ! lein with-profile -user,-dev,+ci,+skip-self-check run -m nvd.task.check "$JSON_DOGFOODING_CONFIG_FILE" "$own_classpath"; then
  echo "$step_name nvd-clojure did not pass dogfooding! (JSON)"
  exit 1
fi

exit 0
