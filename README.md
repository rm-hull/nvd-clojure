# nvd-clojure

> _Formerly known as_ `lein-nvd`

[![Downloads](https://versions.deps.co/rm-hull/nvd-clojure/downloads.svg)](https://versions.deps.co/rm-hull/nvd-clojure)
[![Clojars Project](https://img.shields.io/clojars/v/nvd-clojure.svg)](https://clojars.org/nvd-clojure)

[National Vulnerability Database](https://nvd.nist.gov/) dependency checker tool.

For a given project, all the .jar files from its classpath
will be checked for known security vulnerabilities. `nvd-clojure` passes them to a library called [DependencyCheck](https://github.com/jeremylong/DependencyCheck) which does the vulnerability analysis. Quoting the README from that library:

> DependencyCheck is a utility that attempts to detect publicly disclosed
> vulnerabilities contained within project dependencies. It does this by
> determining if there is a Common Platform Enumeration (CPE) identifier for
> a given dependency. If found, it will generate a report linking to the
> associated CVE entries.

### Installation and basic usage

> _Please see also:_ [Avoiding classpath interference](https://github.com/rm-hull/nvd-clojure/blob/v3.6.0/FAQ.md#what-is-classpath-interference)

#### Leiningen

<details>

Please create a separate project consisting of `[nvd-clojure/nvd-clojure "3.6.0"]`. Said project can be located inside the targeted repo's Git repository.

```clj
(defproject nvd-helper "local"
  :description "nvd-clojure helper project"
  :dependencies [[nvd-clojure "3.6.0"]
                 [org.clojure/clojure "1.11.1"]]
  :jvm-opts ["-Dclojure.main.report=stderr"])
```

Please do not add nvd-clojure as a dependency or plugin in the project.clj of the project to be analysed.

Then you can run, within this helper project:

```
lein with-profile -user run -m nvd.task.check "nvd-clojure.edn" "$(cd <YOUR_PROJECT>; lein with-profile -user,-dev classpath)"
```

The first argument denotes a .edn file with extra options ([example](https://github.com/rm-hull/nvd-clojure/blob/main/.github/nvd-config.edn), [doc](#configuration-options)). You can pass an empty string `""` to mean "please use the default filename" (which is `nvd-clojure.edn`). If this file didn't exist, it will be automatically created for you, with some useful contents and comments.

The `classpath` Leiningen command should reflect a production-like classpath as closely as possible: it should not include dev/test tooling, plugins, etc.

If you are using a multi-modules solution (e.g. `lein-monolith`), you should ensure that each module is included in this classpath; else they will not be analysed.

</details>

#### Clojure CLI

<details>

Please create a separate project consisting exclusively of `nvd-clojure/nvd-clojure {:mvn/version "3.6.0"}`. Said project can be located inside the targeted repo's Git repository.

Please do not add nvd-clojure as a dependency in the deps.edn of the project to be analysed.

> You can accomplish something similar with user-level aliases, or with the `:replace-deps` option, at your own risk.

Then you can run, within this helper project:

```
clojure -J-Dclojure.main.report=stderr -M -m nvd.task.check "nvd-clojure.edn" "$(cd <YOUR_PROJECT>; clojure -Spath -A:any:aliases)"
```

The first argument denotes a .edn file with extra options ([example](https://github.com/rm-hull/nvd-clojure/blob/main/.github/nvd-config.edn), [doc](#configuration-options)). You can pass an empty string `""` to mean "please use the default filename" (which is `nvd-clojure.edn`). If this file didn't exist, it will be automatically created for you, with some useful contents and comments.

The `-Spath` command should reflect a production-like classpath as closely as possible: it should not include dev/test tooling, etc.

If you are using a multi-modules solution (e.g. [Polylith](https://github.com/polyfy/polylith)), you should ensure that each module is included in this classpath; else they will not be analysed.

</details>

#### Clojure CLI Tool

<details>

If you have CLI version 1.10.3.933 or later, you can also install `nvd-clojure` as a "tool":

```bash
clojure -Ttools install nvd-clojure/nvd-clojure '{:mvn/version "RELEASE"}' :as nvd
```

Then you can run:

```bash
clojure -J-Dclojure.main.report=stderr -Tnvd nvd.task/check :classpath \""$(clojure -Spath -A:any:aliases)\"" :config-filename \""nvd-config.edn\""
```

The `:config-filename` argument denotes an .edn file with extra options ([example](https://github.com/rm-hull/nvd-clojure/blob/main/.github/nvd-config.edn), [doc](#configuration-options)).
If this file didn't exist, it will be automatically created for you, with some useful contents and comments.

The `-Spath` command should reflect a production-like classpath as closely as possible: it should not include dev/test tooling, etc.

If you are using a multi-modules solution (e.g. [Polylith](https://github.com/polyfy/polylith)), you should ensure that each module is included in this classpath; else they will not be analysed.

</details>

## Usage overview

Run the program as indicated in the previous section. The first time it runs, it will download (and
cache) various databases from https://nvd.nist.gov. Subsequent runs will
periodically check and update the local database, but the initial run could
therefore be quite slow - of the order of ten minutes or more, so give it time.

On completion, a summary table is output to the console, and a suite of reports
will be produced in the project's `./target/nvd/` directory. If vulnerabilities
are detected, then the check process will exit abnormally, thereby
causing any CI build environment to error. (This behaviour can be overriden by
setting a `:fail-threshold` in the project [configuration](#configuration-options)).

### Example

There is an [example project](https://github.com/rm-hull/nvd-clojure/blob/main/example/project.clj)
which has dependencies with known vulnerabilities
([CVE-2016-3720](https://web.nvd.nist.gov/view/vuln/detail?vulnId=CVE-2016-3720),
[CVE-2015-5262](https://web.nvd.nist.gov/view/vuln/detail?vulnId=CVE-2015-5262),
[CVE-2014-3577](https://web.nvd.nist.gov/view/vuln/detail?vulnId=CVE-2014-3577)).

This can be demonstrated by running the following:

```bash
clojure -J-Dclojure.main.report=stderr -Tnvd nvd.task/check :classpath \""$(cd example; lein with-profile -user classpath)\""
```

This will download the NVD database, and then cross-check the classpath
dependencies against known vulnerabilities. The following summary report will
be displayed on the console:

![summary-report](https://raw.githubusercontent.com/rm-hull/nvd-clojure/main/example/img/summary-report.png)

Note that as there were some vulnerabilities detected, the process was aborted,
with error code `-1` hence the reported `subprocess failed` message.

More detailed reports (both HTML & XML) are written into the
`./example/target/nvd/` directory as follows:

---
![detail-report](https://raw.githubusercontent.com/rm-hull/nvd-clojure/main/example/img/detail-report.png)

## Upgrading dependencies

You may use the built-in dependency tree reporters to find out what the
dependency relationships are:

    $ lein deps :tree # for Leiningen
    $ clojure -Stree # for deps.edn

...make sure to use aliases/profiles in such a way that reflects the production classpath.

[antq](https://github.com/liquidz/antq) will traverse your project
dependencies, and suggest upgraded versions, and can optionally be configured
to update the project file.

(Note that that is only one of the multiple ways of remediating a given vulnerability, please see [FAQ](https://github.com/rm-hull/nvd-clojure/blob/v3.6.0/FAQ.md#how-to-remediate-a-cve-is-it-a-good-idea-to-automate-remediation))

## Configuration

The default settings for `nvd-clojure` are usually sufficient for most projects, but
can be customized with an .edn config file ([example](https://github.com/rm-hull/nvd-clojure/blob/main/.github/nvd-config.edn)).
The filename denoting that file is the first argument to be passed to nvd-clojure when invoking it as a `main` (`-m`) program.

When invoking it via Clojure Tools, it must be passed as a `:config-filename` option, e.g.

```bash
clojure -Tnvd nvd.task/check :classpath \""$(clojure -Spath)\"" :config-filename \""nvd-config.edn\""
```

Note the escaped double quotes around the filename, to ensure that Clojure reads the command line argument as a string, not a symbol.

## Configuration options

There are many DependencyCheck settings (for example to connect via a proxy, or
to specify an alternative to the H2 database). The exact settings can be seen
in the [config.clj](https://github.com/rm-hull/nvd-clojure/blob/main/src/nvd/config.clj) source file and cross-referenced to the DependencyCheck
wiki.

There are some specific settings below which are worthy of a few comments:

* `:fail-threshold` default value `0`; checks the highest CVSS score across all dependencies, and fails if this threshold is breached.
  - As CVSS score ranges from `0..10`, the default value will cause a build to fail even for the lowest rated
  vulnerability.
  - Set to `11` if you never want the build to fail.
* `:data-directory` default value is the data dir of `DependencyCheck`, e.g. `~/.m2/repository/org/owasp/dependency-check-utils/3.2.1/data/`
  - It shouldn't normally be necessary to change this
* `:suppression-file` default unset
  - Allows for CVEs to be permanently or temporarily suppressed.
  - See [DependencyCheck documentation](https://jeremylong.github.io/DependencyCheck/general/suppression.html) for the XML file format.
  - If a nvd-clojure.edn file was automatically generated for you, then this file will also be automatically generated (and enabled) for you.
* `:verbose-summary` default false
  - When set to true, the summary table includes a severity determination for all dependencies.
  - When set to false, the summary table includes only packages that have either low or high severity determination.
* `:output-dir` default value `target/nvd/`: the directory to save reports into
* `:throw-if-check-unsuccessful` - makes the program exit by throwing an exception instead of by invoking `System/exit`.
  - This can ease certain usages.

## Logging

You can override the default logging behaviour by providing a `simplelogger.properties` file on the nvd-clojure classpath. 
Note that this is not the classpath of your project. See `resources/simplelogger.properties` for the default
config.

You can also set logging properties directly through Java system properties (the `-D` flags), for example:

```
clojure -J-Dclojure.main.report=stderr -J-Dorg.slf4j.simpleLogger.log.org.apache.commons=error -Tnvd nvd.task/check # ...
```

## [FAQ](https://github.com/rm-hull/nvd-clojure/blob/v3.6.0/FAQ.md)

## Attribution

`nvd-clojure` uses Jeremy Long's [DependencyCheck](https://github.com/jeremylong/DependencyCheck)
library to do the heavy lifting.

## References

* https://nvd.nist.gov/
* https://www.owasp.org/index.php/OWASP_Dependency_Check
* https://github.com/jeremylong/DependencyCheck
* https://github.com/liquidz/antq

## License

The MIT License (MIT)

Copyright (c) 2016-23 Richard Hull

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
