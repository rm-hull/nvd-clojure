# lein-nvd
[![Build Status](https://travis-ci.org/rm-hull/lein-nvd.svg?branch=master)](http://travis-ci.org/rm-hull/lein-nvd)
[![Coverage Status](https://coveralls.io/repos/rm-hull/lein-nvd/badge.svg?branch=master)](https://coveralls.io/r/rm-hull/lein-nvd?branch=master)
[![Dependencies Status](https://jarkeeper.com/rm-hull/lein-nvd/status.svg)](https://jarkeeper.com/rm-hull/lein-nvd)
[![Downloads](https://jarkeeper.com/rm-hull/lein-nvd/downloads.svg)](https://jarkeeper.com/rm-hull/lein-nvd)
[![Clojars Project](https://img.shields.io/clojars/v/lein-nvd.svg)](https://clojars.org/lein-nvd)
[![Maintenance](https://img.shields.io/maintenance/yes/2016.svg?maxAge=2592000)]()

> NEARLY FIT FOR HUMAN CONSUMPTION ... (▀̿Ĺ̯▀̿ ̿) HOLD TIGHT !!!

[National Vulnerability Database](https://nvd.nist.gov/) dependency-checker
plugin for Leiningen. When run in your project, all the JARs on the classpath
will be checked for known security vulnerabilities. `lein-nvd` extracts project
dependencies and passes them to a library called [Dependency-Check](https://github.com/jeremylong/DependencyCheck) which does the vulnerability analysis. Quoting the README for that library:

> Dependency-Check is a utility that attempts to detect publicly disclosed
> vulnerabilities contained within project dependencies. It does this by
> determining if there is a Common Platform Enumeration (CPE) identifier for
> a given dependency. If found, it will generate a report linking to the
> associated CVE entries.

### Installation

To install globally, add `[lein-nvd "0.2.2"]` into the `:plugins` vector of
your `:user` profile in _~/.lein/profiles.clj_, or on a per-project basis, add
to the profiles section of your _project.clj_.

## Usage

Run `lein nvd check` in your project. The first time the plugin runs, it will
download (and cache) various databases from https://nvd.nist.gov. Subsequent
runs will periodically check and update the local database, but the initial run
could therefore be quite slow - of the order of several minutes, so give it time.

On completion, a summary table is output to the console, and a suite of reports
will be produced in the project's _./target/nvd/_ directory. If vulnerabilities
are detected, then the check process will exit abnormally, thereby
causing any CI build environment to error. (This behaviour can be overriden by
setting a `:fail-threshold` in the project [configuration](#configuration-options)).

### Example

There is an [example project](https://github.com/rm-hull/lein-nvd/blob/master/example/project.clj)
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

![summary-report](https://raw.githubusercontent.com/rm-hull/lein-nvd/master/example/img/summary-report.png)

Note that as there were some vulnerabilities detected, the process was aborted,
with error code -1 hence the reported _subprocess failed_ message.

More detailed reports (both HTML & XML) are written into the
_./example/target/nvd/_ directory as follows:

---
![detail-report](https://raw.githubusercontent.com/rm-hull/lein-nvd/master/example/img/detail-report.png)

## Upgrading dependencies

You may use the built-in (to Leiningen) dependency-tree reporter to find out what the
dependency relationships are:

    $ lein deps :tree

Note that this will show the project dependencies, and any plugins/injections
from your local user profile. `lein nvd` will only run against project dependencies.

[lein-ancient](https://github.com/xsc/lein-ancient) will traverse you
dependencies, and suggest upgraded versions, and can optionally be configured
to update your project file.

## Configuration options

> TODO

## Attribution

`lein-nvd` uses **Jeremy Long**'s [Dependency-Check](https://github.com/jeremylong/DependencyCheck)
library to do the heavy lifting.

## References

* https://nvd.nist.gov/
* https://github.com/jeremylong/DependencyCheck
* https://github.com/xsc/lein-ancient

## License

The MIT License (MIT)

Copyright (c) 2016 Richard Hull

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
