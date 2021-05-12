#!/usr/bin/env bash
set -Euxo pipefail

original=$(pwd)

lein install
cd plugin || exit 1
lein install
cd .. || exit 1
cd example || exit 1

# 1.- Exercise Lein plugin

if lein nvd check > example-lein-output; then
  echo "Should have failed with non-zero code!"
  exit 1
fi

if ! grep --silent "[1-9][0-9] vulnerabilities detected\. Severity: " example-lein-output; then
  echo "Should have found vulnerabilities!"
  exit 1
fi

# 2.- Exercise `main` program

example_classpath="$(lein with-profile -user,-dev,-test classpath)"

# cd to the root dir, so that one runs `defproject nvd-clojure` which is the most clean and realistic way to run `main`:
cd .. || exit 1

if lein with-profile -user,-dev run -m nvd.task.check "" "$example_classpath" > example-lein-output; then
  echo "Should have failed with non-zero code!"
  exit 1
fi

if ! grep --silent "[1-9][0-9] vulnerabilities detected\. Severity: " example-lein-output; then
  echo "Should have found vulnerabilities!"
  exit 1
fi

cd "$original" || exit 1

exit 0
