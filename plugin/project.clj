(defproject lein-nvd "1.6.0"
  :description "National Vulnerability Database [https://nvd.nist.gov/] dependency-checker leiningen plugin."
  :url "https://github.com/rm-hull/nvd-clojure"
  :license {:name "The MIT License (MIT)"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[clj-commons/pomegranate "1.2.1" :exclusions [org.clojure/clojure
                                                               org.apache.maven/maven-resolver-provider
                                                               org.apache.maven.resolver/maven-resolver-api
                                                               org.apache.maven.resolver/maven-resolver-spi
                                                               org.apache.maven.resolver/maven-resolver-util
                                                               commons-codec
                                                               org.apache.maven.resolver/maven-resolver-impl
                                                               org.apache.maven.resolver/maven-resolver-transport-file
                                                               org.apache.maven.resolver/maven-resolver-transport-http
                                                               org.apache.maven.resolver/maven-resolver-connector-basic
                                                               org.apache.httpcomponents/httpclient
                                                               org.apache.httpcomponents/httpcore
                                                               org.slf4j/slf4j-api
                                                               org.codehaus.plexus/plexus-utils]]
                 [org.slf4j/jcl-over-slf4j "1.7.30"]
                 [nvd-clojure "1.6.0"]]
  :scm {:url "git@github.com:rm-hull/nvd-clojure.git"}
  :source-paths ["src"]
  :jar-exclusions [#"(?:^|/).git"]
  :codox {:source-paths ["src"]
          :output-path "doc/api"
          :source-uri "https://github.com/rm-hull/nvd-clojure/blob/master/{filepath}#L{line}"}
  :target-path "target/%s"
  :min-lein-version "2.8.1"
  :profiles {:dev {:global-vars {*warn-on-reflection* true}
                   :dependencies [[org.clojure/clojure "1.10.3"]]
                   :plugins [[jonase/eastwood "0.9.9"]
                             [lein-codox "0.10.7"]
                             [lein-cloverage "1.1.1"]]}
             :ci {:pedantic? :abort}}
  :eval-in-leiningen true)
