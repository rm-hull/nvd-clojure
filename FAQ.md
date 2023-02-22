## FAQ

### What versioning scheme does nvd-clojure use?

nvd-clojure simply uses Semver. Breaking changes may happen because it is a security-sensitive tool: sometimes it is in best interest of security to leave unsafe details behind.

It is part of nvd-clojure's API contract that its output will 'break' your CI build on the regular: it is precisely intended to do that (exit with a non-zero code whenever something needs attention).

So breaking API changes aren't drastically different from the other changes that regular nvd-clojure use would imply.

With that said, there is active effort to avoid superflous API changes. We do intend to keep whatever can be reasonably kept.

### How do I migrate from the .json config format to .edn?

The .json config file format is considered legacy now, although it will remain supported.

If you wish to use .edn instead, please retain the map associated to the `"nvd"` key, discarding the surrounding data, and perform a simple conversion where string keys become keywords.

Example:

```diff
-{"delete-config?": false,
- "nvd": {"suppression-file": "nvd_suppressions.xml"}}
+{:suppression-file "nvd_suppressions.xml"}
```

...rename this file to nvd-clojure.edn and make sure to specify this new filename in any CLI/CI invocations.

If the file is named nvd-clojure.edn (i.e. the default filename), you can specify the empty string `""` instead of typing the filename.

This shorthand form has the advantage of producing a reusable command that if executed in some other project, will automatically create this .edn file when missing, including some useful contents and comments.

### Why are there false positives?

nvd-clojure uses [DependencyCheck](https://github.com/jeremylong/DependencyCheck) for doing most of its job. DependencyCheck checks dependencies against the NVD database.

A given CVE is not expressed in a format that can be unmistakably matched with a single specific Maven artifact. That is outside of DependencyCheck's control, so it does a best effort at preventing false positives.

### Why is it suggested that I install nvd-clojure by creating a project directory?

For all usages other than Clojure Tools (e.g. Leiningen, or the Clojure CLI), the instructions suggest to create a directory containing a `project.clj` or `deps.edn` file.

This would be technically a 'subproject' within your project. Some people might find this strange, however it's a drastically simpler approach than the alternatives, that is easy to understand and which has proven to rule out all sorts of issues that were historically present.

You can think of this directory as not drastically different from a `.github` or `.circleci` directory. It's just a single directory with a single, "just data" piece of information.

There are other approaches that can work, however our assessment is that they're not as robust and may break at some point. 

### Why not distribute a .jar that can be invoked on any project?

That .jar would be a uberjar, i.e. it not only would bundle nvd-clojure, but also all its dependencies.

We believe bundling such a large, opaque blob would not be up to the standards of a security-sensitive project.

### OSS Index flakiness

If you experience HTTP errors from OSS Index while running nvd-clojure, setting `:ossindex-warn-only-on-remote-errors true` will prevent those from failing that run.

(You can grep for that code in this repo for a precise example)

However please be advised that doing that can cause false negatives.

OSS Index is not expected to always be flaky, so if setting this, we would advise to schedule removing that setting, while keeping an eye on any issue report that might be publicly available.

### How to remediate a CVE? Is it a good idea to automate remediation?

CVEs can be remediated in a variety of ways:

* Upgrading a direct dependency
* Replacing a dependency for another one
  * e.g. change the choice of JSON parser
* Upgrading a managed dependency
* Declaring a transitive dependency, setting it to a newer version
* Upgrading a transitive dependency
* Removing a direct dependency
* Adding `:exclusions` such that a transitive dependency will be removed
* Adding an entry to `nvd-suppressions.xml`
  * Fine-grained (exact)
  * Coarse-grained (wildcard)
  * Temporary (with an expiration date)

Devising a sensible remediation for a specific scenario will depend on your project and its needs. Completing one of the choices outlined above might need application-level code changes.

No automated program can possibly assess and apply the best possible choice. We strongly believe the simplest and safest remediation can only be devised by you!

In general, we do try to encourage a thoughtful approach to security - CVE reports aren't simply a chore to be silenced in 'whatever way works'.
