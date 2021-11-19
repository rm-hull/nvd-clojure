# nvd-clojure

> _Formerly known as_ `lein-nvd`

[![Build Status](https://github.com/rm-hull/nvd-clojure/workflows/Continuous%20Integration/badge.svg)](https://github.com/rm-hull/nvd-clojure/actions?query=workflow%3A%22Continuous+Integration%22)
[![Coverage Status](https://coveralls.io/repos/rm-hull/nvd-clojure/badge.svg?branch=master)](https://coveralls.io/r/rm-hull/nvd-clojure?branch=master)
[![Dependencies Status](https://byob.yarr.is/dotemacs/actions-play/dependencies)](https://github.com/rm-hull/nvd-clojure/actions?query=workflow%3A%22dependencies%22)
[![Downloads](https://versions.deps.co/rm-hull/nvd-clojure/downloads.svg)](https://versions.deps.co/rm-hull/nvd-clojure)
[![Clojars Project](https://img.shields.io/clojars/v/nvd-clojure.svg)](https://clojars.org/nvd-clojure)
[![Maintenance](https://img.shields.io/maintenance/yes/2021.svg?maxAge=2592000)]()

[National Vulnerability Database](https://nvd.nist.gov/) dependency-checker
library (and plugin for Leiningen).

When run in your project, all the JARs on the classpath
will be checked for known security vulnerabilities. `nvd-clojure` extracts project
dependencies and passes them to a library called [Dependency-Check](https://github.com/jeremylong/DependencyCheck) which does the vulnerability analysis. Quoting the README for that library:

> Dependency-Check is a utility that attempts to detect publicly disclosed
> vulnerabilities contained within project dependencies. It does this by
> determining if there is a Common Platform Enumeration (CPE) identifier for
> a given dependency. If found, it will generate a report linking to the
> associated CVE entries.

### Installation

> _Please see also:_ [Avoiding classpath interference](#avoiding-classpath-interference)

#### Clojure CLI

To install in a given project, you can add `nvd-clojure/nvd-clojure {:mvn/version "1.9.0"}` to your deps.edn.

If you have CLI version 1.10.3.933 or later, you can also install `nvd-clojure` as a "tool":

```bash
clojure -Ttools install nvd-clojure/nvd-clojure '{:mvn/version "RELEASE"}' :as nvd
```
and then you can run the tool like this:

```bash
clojure -Tnvd nvd.task/check :classpath '"'"$(clojure -Spath -A:any:aliases)"'"'
```

under `:aliases` in _~/.clojure/deps.edn_, or add it to `:aliases` in
the project local `deps.edn`, to look something like this:

```clojure
:aliases {:nvd {:extra-deps {nvd-clojure/nvd-clojure {:mvn/version "1.9.0"}}
                :main-opts ["-m" "nvd.task.check"]}}
```

#### Leiningen

To install globally, add `[lein-nvd "1.9.0"]` into the `:plugins` vector of
your `:user` profile in _~/.lein/profiles.clj_, or on a per-project basis, add
to the profiles section of your _project.clj_.

## Usage

Run `lein nvd check` or `clj -M:nvd` (if you've chosen the alias `:nvd`, like
above) in your project. The first time the plugin runs,it will download (and
cache) various databases from https://nvd.nist.gov. Subsequent runs will
periodically check and update the local database, but the initial run could
therefore be quite slow - of the order of ten minutes or more, so give it time.

On completion, a summary table is output to the console, and a suite of reports
will be produced in the project's _./target/nvd/_ directory. If vulnerabilities
are detected, then the check process will exit abnormally, thereby
causing any CI build environment to error. (This behaviour can be overriden by
setting a `:fail-threshold` in the project [configuration](#configuration-options)).

### Example

There is an [example project](https://github.com/rm-hull/nvd-clojure/blob/master/example/project.clj)
which has dependencies with known vulnerabilities
([CVE-2016-3720](https://web.nvd.nist.gov/view/vuln/detail?vulnId=CVE-2016-3720),
[CVE-2015-5262](https://web.nvd.nist.gov/view/vuln/detail?vulnId=CVE-2015-5262),
[CVE-2014-3577](https://web.nvd.nist.gov/view/vuln/detail?vulnId=CVE-2014-3577)).
This can be demonstrated by running the following:

    $ cd example
    $ lein nvd check

This will download the NVD database, and then cross-check the classpath
dependencies against known vulnerabilities. The following summary report will
be displayed on the console:

![summary-report](https://raw.githubusercontent.com/rm-hull/nvd-clojure/master/example/img/summary-report.png)

Note that as there were some vulnerabilities detected, the process was aborted,
with error code -1 hence the reported _subprocess failed_ message.

More detailed reports (both HTML & XML) are written into the
_./example/target/nvd/_ directory as follows:

---
![detail-report](https://raw.githubusercontent.com/rm-hull/nvd-clojure/master/example/img/detail-report.png)

## Upgrading dependencies

You may use the built-in dependency tree reporters to find out what the
dependency relationships are:

    $ lein deps :tree # for Leiningen
    $ clojure -Stree # for deps.edn

...make sure to use aliases/profiles in such a way that reflects the production classpath.

[antq](https://github.com/liquidz/antq) will traverse your project
dependencies, and suggest upgraded versions, and can optionally be configured
to update the project file.

## Other commands

Running the following command shows what sub-commands are available:

    $ lein help nvd

      Scans project dependencies, attempting to detect publicly disclosed
      vulnerabilities contained within dependent JAR files. It does this by
      determining if there is a Common Platform Enumeration (CPE) identifier
      for a given dependency. On completion, a summary table is displayed on
      the console (showing the status for each dependency), and detailed report
      linking to the associated CVE entries.

      This task should be invoked with one of three commands:

          check  - will optionally download the latest database update files,
                   and then run the analyze and report stages. Typically, if
                   the database has been updated recently, then the update
                   stage will be skipped.

          purge  - will remove the local database files. Subsequently running
                   the 'check' command will force downloading the files again,
                   which could take a long time.

          update - will attempt to download the latest database updates, and
                   incorporate them into the local store. Usually not necessary,
                   as this is incorporated into the 'check' command.

      Any text after the command are treated as arguments and are passed directly
      directly to the command for further processing.

    Arguments: ([command & args])

While `purge` and `update` are available, it is not normally required to use them,
and purging will cause a subsequent `check` or `update` to download the whole
database again.

## Configuration options

The default settings for `nvd-clojure` are usually sufficient for most projects, but
can be customized by adding an `:nvd { ... }` section in your _project.clj_.

These options can also be expressed as the keys in a .json config file ([example](https://github.com/rm-hull/nvd-clojure/blob/master/.github/nvd-config.json)).
The filename denoting that file is the first argument to be passed to nvd-clojure when invoking it as a `main` (`-m`) program.
The keys must reside inside a `"nvd": {...}` entry, not at the top-level.

There are many dependency-check settings (for example to connect via a proxy, or
to specify an alternative to the H2 database). The exact settings can be seen
in the [config.clj](https://github.com/rm-hull/nvd-clojure/blob/master/src/nvd/config.clj) source file and cross-referenced to the dependency-check
wiki.

There are some specific settings below which are worthy of a few comments:

* `:fail-threshold` default value `0`; checks the highest CVSS score across all dependencies, and fails if this threshold is breached.
  - As CVSS score ranges from `0..10`, the default value will cause a build to fail even for the lowest rated
  vulnerability.
  - Set to `11` if you never want the build to fail.
* `:data-directory` default value is the data dir of `DependencyCheck`, e.g. `~/.m2/repository/org/owasp/dependency-check-utils/3.2.1/data/`
  - It shouldn't normally be necessary to change this
* `:suppression-file` default unset
  - Allows for CVEs to be permanently suppressed.
  - See [DependencyCheck documentation](https://jeremylong.github.io/DependencyCheck/) for the XML file-format.
  - [See also](https://jeremylong.github.io/DependencyCheck/general/suppression.html)
* `:verbose-summary` default false
  - When set to true, the summary table includes a severity determination for all dependencies.
  - When set to false, the summary table includes only packages that have either low or high severity determination.
* `:output-dir` default value `target/nvd/`: the directory to save reports into
* `:throw-if-check-unsuccessful?` - makes the program exit by throwing an exception instead of by invoking `System/exit`.
  - This can ease certain usages.

## Avoiding classpath interference

nvd-clojure has some Java dependencies, which in turn can have CVEs themselves, or in any case interfere with your project's dependency tree, that would be computed in absence of nvd-clojure.

For this reason, you might want to invoke `nvd.task.check`'s main function by passing a classpath string as an argument.

Said classpath string should try reflecting a _production's classpath_ as accurately as possible: it should not include dev/test tooling, plugins (like nvd-clojure or any other), etc.

#### Lein example

```bash
lein run -m nvd.task.check "" "$(lein with-profile -user,-dev classpath)"
```

#### deps.edn example

```bash
clojure -m nvd.task.check "" "$(clojure -Spath)"
```

...in both cases, an empty string is passed as the first argument, for backwards compatibility reasons. You can also pass a filename instead, denoting a .json file with extra options ([example](https://github.com/rm-hull/nvd-clojure/blob/master/.github/nvd-config.json)).

For extra isolation, it is recommended that you invoke `nvd.task.check` from _outside_ your project - e.g. from an empty project, a git clone of this very repo, or from $HOME (assuming you have nvd-clojure as a dependency in your [user-wide Lein profile](https://github.com/technomancy/leiningen/blob/2586957f9d099ff11d50d312a6daf397c2a06fb1/doc/PROFILES.md)).

## Building locally

Build and install the core module, then do the same for the plugin:

    $ lein test
    $ lein install
    $ cd plugin
    $ lein test
    $ lein install
    $ cd ../example
    $ lein nvd check

A sample report is available for testing in the _example_ sub-directory.

## Attribution

`nvd-clojure` uses **Jeremy Long**'s [Dependency-Check](https://github.com/jeremylong/DependencyCheck)
library to do the heavy lifting.

## References

* https://nvd.nist.gov/
* https://www.owasp.org/index.php/OWASP_Dependency_Check
* https://github.com/jeremylong/DependencyCheck
* https://github.com/xsc/lein-ancient

## License

The MIT License (MIT)

Copyright (c) 2016-21 Richard Hull

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
