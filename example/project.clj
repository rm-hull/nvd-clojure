(defproject example-with-known-vulnerabilities "1.4.17"
  :description "Example project with dependencies that have known vulnerabilities"
  :license {
    :name "The MIT License (MIT)"
    :url "http://opensource.org/licenses/MIT"}
  :dependencies [
    ; No known vulnerabilities, but have dependencies
    [org.clojure/data.json "0.2.6"]
    [korma "0.4.3"]
    [com.amazonaws/aws-lambda-java-core "1.2.0"],

    ; Sub-dependency has MEDIUM rated-vulnerabilities
    [org.apache.maven.wagon/wagon-http "2.2"]

    ; Has HIGH severity vulnerabilities
    [com.fasterxml.jackson.core/jackson-databind "2.4.2"]
    [com.fasterxml.jackson.core/jackson-annotations "2.4.0"]

    [org.slf4j/slf4j-api "1.7.25"]
  ]
  :source-paths ["src"]
  :min-lein-version "2.6.1"
  :profiles {
    :dev {
      :dependencies [
        [org.clojure/clojure "1.9.0"]]
      :plugins [
        [lein-nvd "1.1.0"]]}})
