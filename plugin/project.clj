(defproject lein-nvd "0.2.7"
  :description "National Vulnerability Database [https://nvd.nist.gov/] dependency-checker leiningen plugin."
  :url "https://github.com/rm-hull/lein-nvd"
  :license {
    :name "The MIT License (MIT)"
    :url "http://opensource.org/licenses/MIT"}
  :dependencies [
    [org.clojure/data.json "0.2.6"]
    [com.cemerick/pomegranate "0.3.1"]
    ]
  :scm {:url "git@github.com:rm-hull/lein-nvd.git"}
  :source-paths ["src"]
  :jar-exclusions [#"(?:^|/).git"]
  :codox {
    :source-paths ["src"]
    :output-path "doc/api"
    :source-uri "http://github.com/rm-hull/lein-nvd/blob/master/{filepath}#L{line}"  }
  :min-lein-version "2.7.1"
  :profiles {
    :dev {
      :global-vars {*warn-on-reflection* true}
      :dependencies [
        [org.clojure/clojure "1.8.0"] ]
      :plugins [
        [lein-codox "0.10.1"]
        [lein-cloverage "1.0.8"]]}}
  :eval-in-leiningen true)
