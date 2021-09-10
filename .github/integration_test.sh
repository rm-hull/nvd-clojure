#!/usr/bin/env bash
set -Euxo pipefail

CONFIG_FILE="$PWD/.github/nvd-config.json"
SUCCESS_REGEX="[1-9][0-9] vulnerabilities detected\. Severity: "

if ! lein with-profile -user,-dev,+ci install; then
  exit 1
fi

cd plugin || exit 1

if ! lein with-profile -user,-dev,+ci install; then
  exit 1
fi

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
cd .. || exit 1

if lein with-profile -user,-dev,+ci run -m nvd.task.check "$CONFIG_FILE" "$example_classpath" > example-lein-output; then
  echo "Should have failed with non-zero code!"
  exit 1
fi

if ! grep --silent "$SUCCESS_REGEX" example-lein-output; then
  echo "Should have found vulnerabilities!"
  exit 1
fi

# 4.- Exercise `tools.deps` integration

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

# 5.- Dogfood the `nvd-clojure` project

own_classpath="$(lein with-profile -user,-dev,-test classpath)"

if ! lein with-profile -user,-dev,+ci run -m nvd.task.check "" "$own_classpath"; then
  echo "nvd-clojure did not pass dogfooding!"
  exit 1
fi

# 6.- Dogfood the `lein-nvd` project

cd plugin || exit 1

plugin_classpath="$(lein with-profile -user,-dev,-test classpath)"

cd .. || exit 1

if ! lein with-profile -user,-dev,+ci run -m nvd.task.check "" "$plugin_classpath"; then
  echo "lein-nvd did not pass dogfooding!"
  exit 1
fi

exit 0
