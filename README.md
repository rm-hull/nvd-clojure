# nvd-clojure

> _Formerly known as_ `lein-nvd`

[![Downloads](https://versions.deps.co/rm-hull/nvd-clojure/downloads.svg)](https://versions.deps.co/rm-hull/nvd-clojure)
[![Clojars Project](https://img.shields.io/clojars/v/nvd-clojure.svg)](https://clojars.org/nvd-clojure)

[National Vulnerability Database](https://nvd.nist.gov/) dependency checker
library.

When run in your project, all the JARs on the classpath
will be checked for known security vulnerabilities. `nvd-clojure` extracts project
dependencies and passes them to a library called [Dependency-Check](https://github.com/jeremylong/DependencyCheck) which does the vulnerability analysis. Quoting the README for that library:

> Dependency-Check is a utility that attempts to detect publicly disclosed
> vulnerabilities contained within project dependencies. It does this by
> determining if there is a Common Platform Enumeration (CPE) identifier for
> a given dependency. If found, it will generate a report linking to the
> associated CVE entries.

### Installation and basic usage

> _Please see also:_ [Avoiding classpath interference](#avoiding-classpath-interference)

#### Leiningen

<details>

Please create a separate project consisting of `[nvd-clojure/nvd-clojure "2.7.0"]`. Said project can be located inside the targeted repo's Git repository.

```
(defproject nvd-helper "local"
  :description "nvd-clojure helper project"
  :dependencies [[nvd-clojure "2.7.0"]
                 [org.clojure/clojure "1.11.1"]]
  :jvm-opts ["-Dclojure.main.report=stderr"])
```

Please do not add nvd-clojure as a dependency or plugin in the project.clj of the project to be analysed.

Then you can run, within this helper project:

```
lein with-profile -user run -m nvd.task.check "" "$(cd <YOUR_PROJECT>; lein with-profile -user,-dev classpath)"
```

An empty string is passed as the first argument, for backwards compatibility reasons. You can also pass a filename instead, denoting a .json file with extra options ([example](https://github.com/rm-hull/nvd-clojure/blob/master/.github/nvd-config.json)).

The `classpath` command should reflect a production-like classpath as closely as possible: it should not include dev/test tooling, plugins, etc.

If you are using a multi-modules solution (e.g. `lein-monolith`), you should ensure that each module is included in this classpath; else they will not be analysed.

</details>

#### Clojure CLI

<details>

Please create a separate project consisting exclusively of `nvd-clojure/nvd-clojure {:mvn/version "2.7.0"}`. Said project can be located inside the targeted repo's Git repository.

Please do not add nvd-clojure as a dependency in the deps.edn of the project to be analysed.

> You can accomplish something similar with user-level aliases, or with the `:replace-deps` option, at your own risk.

Then you can run, within this helper project:

```
clojure -J-Dclojure.main.report=stderr -M -m nvd.task.check "" "$(cd <YOUR_PROJECT>; clojure -Spath -A:any:aliases)"
```

An empty string is passed as the first argument, for backwards compatibility reasons. You can also pass a filename instead, denoting a .json file with extra options ([example](https://github.com/rm-hull/nvd-clojure/blob/master/.github/nvd-config.json)).

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
clojure -J-Dclojure.main.report=stderr -Tnvd nvd.task/check :classpath '"'"$(clojure -Spath -A:any:aliases)"'"'
```

You can optionally pass a `:config-filename`, denoting a .json file with extra options ([example](https://github.com/rm-hull/nvd-clojure/blob/master/.github/nvd-config.json)).

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

There is an [example project](https://github.com/rm-hull/nvd-clojure/blob/master/example/project.clj)
which has dependencies with known vulnerabilities
([CVE-2016-3720](https://web.nvd.nist.gov/view/vuln/detail?vulnId=CVE-2016-3720),
[CVE-2015-5262](https://web.nvd.nist.gov/view/vuln/detail?vulnId=CVE-2015-5262),
[CVE-2014-3577](https://web.nvd.nist.gov/view/vuln/detail?vulnId=CVE-2014-3577)).
This can be demonstrated by running the following:

```bash
clojure -J-Dclojure.main.report=stderr -Tnvd nvd.task/check :classpath '"'"$(cd example; lein with-profile -user classpath)"'"'
```

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

## Configuration options

The default settings for `nvd-clojure` are usually sufficient for most projects, but
can be customized with  a .json config file ([example](https://github.com/rm-hull/nvd-clojure/blob/master/.github/nvd-config.json)).

The filename denoting that file is the first argument to be passed to nvd-clojure when invoking it as a `main` (`-m`) program.
The keys must reside inside a `"nvd": {...}` entry, not at the top-level. A top-level `"delete-config?": false` entry is expected for the time being ([example](https://github.com/rm-hull/nvd-clojure/blob/59dd3f33cf87b1527fdc06f78eb97d9fad990ff0/.github/nvd-config.json)), for backwards compatibility reasons.

There are many dependency-check settings (for example to connect via a proxy, or
to specify an alternative to the H2 database). The exact settings can be seen
in the [config.clj](https://github.com/rm-hull/nvd-clojure/blob/master/src/nvd/config.clj) source file and cross-referenced to the dependency-check
wiki.

There are some specific settings below which are worthy of a few comments:

* `"fail-threshold"` default value `0`; checks the highest CVSS score across all dependencies, and fails if this threshold is breached.
  - As CVSS score ranges from `0..10`, the default value will cause a build to fail even for the lowest rated
  vulnerability.
  - Set to `11` if you never want the build to fail.
* `"data-directory"` default value is the data dir of `DependencyCheck`, e.g. `~/.m2/repository/org/owasp/dependency-check-utils/3.2.1/data/`
  - It shouldn't normally be necessary to change this
* `"suppression-file"` default unset
  - Allows for CVEs to be permanently suppressed.
  - See [DependencyCheck documentation](https://jeremylong.github.io/DependencyCheck/) for the XML file-format.
  - [See also](https://jeremylong.github.io/DependencyCheck/general/suppression.html)
* `"verbose-summary"` default false
  - When set to true, the summary table includes a severity determination for all dependencies.
  - When set to false, the summary table includes only packages that have either low or high severity determination.
* `"output-dir"` default value `target/nvd/`: the directory to save reports into
* `"throw-if-check-unsuccessful"` - makes the program exit by throwing an exception instead of by invoking `System/exit`.
  - This can ease certain usages.

## Avoiding classpath interference

nvd-clojure has some Java dependencies, which in turn can have CVEs themselves.

Likewise, a given project's dependencies can overlap and therefore affect nvd-clojure's, leading it to incorrect functioning.

For these reasons, it is strongly advised to follow the installation/usage instructions carefully.

## Attribution

`nvd-clojure` uses **Jeremy Long**'s [Dependency-Check](https://github.com/jeremylong/DependencyCheck)
library to do the heavy lifting.

## References

* https://nvd.nist.gov/
* https://www.owasp.org/index.php/OWASP_Dependency_Check
* https://github.com/jeremylong/DependencyCheck
* https://github.com/liquidz/antq

## License

The MIT License (MIT)

Copyright (c) 2016-22 Richard Hull

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
