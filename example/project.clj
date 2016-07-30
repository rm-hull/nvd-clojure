(defproject example-with-known-vulnerabilities "1.4.17"
  :description "Example project with dependencies that have known vulnerabilities"
  :license {
    :name "The MIT License (MIT)"
    :url "http://opensource.org/licenses/MIT"}
  :dependencies [
    [com.fasterxml.jackson.core/jackson-databind "2.4.2"]
    [com.fasterxml.jackson.core/jackson-annotations "2.4.0"]]
  :source-paths ["src"]
  :min-lein-version "2.6.1"
  :profiles {
    :dev {
      :plugins [
        [lein-nvd "0.1.1-SNAPSHOT"]]}})
