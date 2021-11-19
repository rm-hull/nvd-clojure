#!/usr/bin/env bash
set -Euxo pipefail

cd "${BASH_SOURCE%/*}/.." || exit 1

PROJECT_DIR="$PWD"
CONFIG_FILE="$PROJECT_DIR/.github/nvd-config.json"
SUCCESS_REGEX="[1-9][0-9] vulnerabilities detected\. Severity: "

if ! lein with-profile -user,-dev,+ci install; then
  exit 1
fi

# add some debugging to verify CLI state:
clojure -Sdescribe
echo "Root deps"
cat /opt/hostedtoolcache/ClojureToolsDeps/1.10.3-1029/x64/lib/clojure/deps.edn
echo "User deps"
cat /home/runner/.config/clojure/deps.edn
clojure -Ttool list

if ! clojure -Ttools install nvd-clojure/nvd-clojure '{:mvn/version "RELEASE"}' :as nvd; then
  exit 1
fi

cd "$PROJECT_DIR/plugin" || exit 1

if ! lein with-profile -user,-dev,+ci install; then
  exit 1
fi

cd "$PROJECT_DIR/example" || exit 1

# 1.- Exercise Lein plugin

if lein with-profile -user nvd check > example-lein-output; then
  echo "Should have failed with non-zero code!"
  exit 1
fi

if ! grep --silent "$SUCCESS_REGEX" example-lein-output; then
  echo "Should have found vulnerabilities!"
  exit 1
fi

# 2.- Exercise Lein plugin, with :throw-if-check-unsuccessful? option

if lein with-profile -user,+nvd-throw-on-exit nvd check > example-lein-output 2>&1; then
  echo "Should have failed with non-zero code!"
  exit 1
fi

if ! grep --silent "$SUCCESS_REGEX" example-lein-output; then
  echo "Should have found vulnerabilities!"
  exit 1
fi

if ! grep --silent "Error encountered performing task 'nvd'" example-lein-output; then
  echo "Should have thrown an exception!"
  exit 1
fi

if ! grep --silent "clojure.lang.ExceptionInfo: nvd-clojure failed / found vulnerabilities" example-lein-output; then
  echo "Should have thrown an exception with a specific message!"
  exit 1
fi

# 3.- Exercise `main` program

example_classpath="$(lein with-profile -user,-dev,-test classpath)"

# cd to the root dir, so that one runs `defproject nvd-clojure` which is the most clean and realistic way to run `main`:
cd "$PROJECT_DIR" || exit 1

if lein with-profile -user,-dev,+ci run -m nvd.task.check "$CONFIG_FILE" "$example_classpath" > example-lein-output; then
  echo "Should have failed with non-zero code!"
  exit 1
fi

if ! grep --silent "$SUCCESS_REGEX" example-lein-output; then
  echo "Should have found vulnerabilities!"
  exit 1
fi

# 4.- Exercise `tools.deps` integration

cd "$PROJECT_DIR/example" || exit 1

example_classpath="$(clojure -Spath)"

# cd to the root dir, so that one runs `defproject nvd-clojure` which is the most clean and realistic way to run `main`:
cd "$PROJECT_DIR" || exit 1

if clojure -M -m nvd.task.check "$CONFIG_FILE" "$example_classpath" > example-lein-output; then
  echo "Should have failed with non-zero code!"
  exit 1
fi

if ! grep --silent "$SUCCESS_REGEX" example-lein-output; then
  echo "Should have found vulnerabilities!"
  exit 1
fi

# 5.- Exercise Clojure CLI Tools integration

cd "$PROJECT_DIR/example" || exit 1

example_classpath="$(clojure -Spath)"

# cd to $HOME, to demonstrate that the Tool does not depend on a deps.edn file:
cd || exit 1

if clojure -Tnvd nvd.task/check :classpath '"'"$example_classpath"'"' > example-lein-output; then
  echo "Should have failed with non-zero code!"
  exit 1
fi

if ! grep --silent "$SUCCESS_REGEX" example-lein-output; then
  echo "Should have found vulnerabilities!"
  exit 1
fi

# 6.- Dogfood the `nvd-clojure` project

cd "$PROJECT_DIR" || exit 1

own_classpath="$(lein with-profile -user,-dev,-test classpath)"

if ! lein with-profile -user,-dev,+ci run -m nvd.task.check "" "$own_classpath"; then
  echo "nvd-clojure did not pass dogfooding!"
  exit 1
fi

# 7.- Dogfood the `lein-nvd` project

cd "$PROJECT_DIR/plugin" || exit 1

plugin_classpath="$(lein with-profile -user,-dev,-test classpath)"

cd "$PROJECT_DIR" || exit 1

if ! lein with-profile -user,-dev,+ci run -m nvd.task.check "" "$plugin_classpath"; then
  echo "lein-nvd did not pass dogfooding!"
  exit 1
fi

exit 0
