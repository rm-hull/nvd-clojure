## Changes from 2.5.0 to 2.6.0

* Update `dependency-check-core`.

## Changes from 2.4.0 to 2.5.0

* Update `dependency-check-core`.

## Changes from 2.3.0 to 2.4.0

* [#123](https://github.com/rm-hull/nvd-clojure/issues/123): Explicitly only analyze dependencies/artifacts that are relevant to JVM projects.
  * i.e. the internal analyzers that are specialized in other ecosystems e.g. .NET, Ruby, Node.js, etc will not be run at all, improving performance and accuracy.
  * The nvd-clojure implementation never allowed non-jar files to be analyzed, so in practice no behavior has possibly been changed.  
* Update `dependency-check-core`.
* Misc cosmetic improvements for what is printed during execution.

## Changes from 2.2.0 to 2.3.0

* Update `dependency-check-core`.

## Changes from 2.1.0 to 2.2.0

* Update `dependency-check-core`.

## Changes from 2.0.0 to 2.1.0

* Update `dependency-check-core`.

## Changes from 1.9.0 to 2.0.0

* [#113](https://github.com/rm-hull/nvd-clojure/issues/113): Remove all unsafe APIs.
  * Please refer to the README for the recommended installation/usage patterns.
* [#117](https://github.com/rm-hull/nvd-clojure/issues/117): Detect when `nvd-clojure` is being used in a likely-incorrect way, and fail the program when that happens.
* Remove deprecated tasks, related to DB management.

## Changes from 1.8.0 to 1.9.0

#### New

* Offer a new API oriented for Clojure CLI Tools users.
* Skip analyzing source directories (as opposed to .jar files), which are irrelevant (as analyzing sources is beyond the scope of `dependency-check-core`) and can hinder Clojure CLI usage patterns.

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
