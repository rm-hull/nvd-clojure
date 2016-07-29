(defproject rm-hull/lein-nvd "0.1.0"
  :description "National Vulnerability Database [https://nvd.nist.gov/] dependency-checker plugin for Leiningen."
  :url "https://github.com/rm-hull/lein-nvd-scan"
  :license {
    :name "The MIT License (MIT)"
    :url "http://opensource.org/licenses/MIT"}
  :dependencies [
    [org.clojure/clojure "1.8.0"]
    [org.owasp/dependency-check-core "1.4.0"]]
  :scm {:url "git@github.com:rm-hull/lein-nvd.git"}
  :source-paths ["src"]
  :jar-exclusions [#"(?:^|/).git"]
  :codox {
    :source-paths ["src"]
    :output-path "doc/api"
    :source-uri "http://github.com/rm-hull/lein-nvd/blob/master/{filepath}#L{line}"  }
  :min-lein-version "2.6.1"
  :profiles {
    :dev {
      :global-vars {*warn-on-reflection* true}
      :plugins [
        [lein-codox "0.9.5"]
        [lein-cloverage "1.0.6"]]}}
  :eval-in-leiningen true
  :nvd {
    :proxy {
      :server "bargle"
      :port 9090
    }
  })
