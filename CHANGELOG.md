## Changes from 1.7.0 to 1.8.0

#### New

* Update `dependency-check-core`.

## Changes from 1.6.0 to 1.7.0

#### New

* Update `dependency-check-core`.

## Changes from 1.5.0 to 1.6.0

#### New

* Implement `:throw-if-check-unsuccessful?` option.
  * Fixes https://github.com/rm-hull/nvd-clojure/issues/50
* Upgrade `dependency-check-core` dependency.

## Changes from 0.6.0 to 1.0

#### Breaking

From now on, the program will only show a summary table of packages that
are demarcated as having a CVSS score greater than zero (i.e any that are
rated OK, are now not shown by default). Any that are rated low or high severity
continue to be shown. To revert to pre-1.0 behavior, add `:verbose-summary true`
to your project [configuration](#configuration-options).
