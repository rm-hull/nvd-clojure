(defproject nvd-clojure "0.4.2"
  :description "National Vulnerability Database [https://nvd.nist.gov/] dependency-checker"
  :url "https://github.com/rm-hull/lein-nvd"
  :license {
    :name "The MIT License (MIT)"
    :url "http://opensource.org/licenses/MIT"}
  :dependencies [
    [org.clojure/clojure "1.9.0"]
    [clansi "1.0.0"]
    [org.clojure/data.json "0.2.6"]
    [org.slf4j/slf4j-simple "1.7.25"]
    [org.owasp/dependency-check-core "3.0.2"]
    [rm-hull/table "0.6.4"]]
  :scm {:url "git@github.com:rm-hull/lein-nvd.git"}
  :source-paths ["src"]
  :jar-exclusions [#"(?:^|/).git"]
  :codox {
    :source-paths ["src"]
    :output-path "doc/api"
    :source-uri "http://github.com/rm-hull/lein-nvd/blob/master/{filepath}#L{line}"  }
  :min-lein-version "2.8.1"
  :profiles {
    :dev {
      :global-vars {*warn-on-reflection* true}
      :plugins [
        [lein-cljfmt "0.5.7"]
        [lein-codox "0.10.3"]
        [lein-cloverage "1.0.10"]]}})
