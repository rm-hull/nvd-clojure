(defproject lein-nvd "2.0.0"
  :description "National Vulnerability Database [https://nvd.nist.gov/] dependency-checker leiningen plugin."
  :url "https://github.com/rm-hull/nvd-clojure"
  :license {:name "The MIT License (MIT)"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies []
  :scm {:url "git@github.com:rm-hull/nvd-clojure.git"}
  :source-paths ["src"]
  :jar-exclusions [#"(?:^|/).git"]
  :codox {:source-paths ["src"]
          :output-path "doc/api"
          :source-uri "https://github.com/rm-hull/nvd-clojure/blob/master/{filepath}#L{line}"}
  :target-path "target/%s"
  :min-lein-version "2.8.1"
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.10.3"]]
                   :plugins [[jonase/eastwood "1.0.0"]
                             [lein-codox "0.10.7"]
                             [lein-cloverage "1.2.3"]]
                   :eastwood {:add-linters [:boxed-math
                                            :performance]}}
             :ci {:pedantic? :abort}}
  :eval-in-leiningen true
  :deploy-repositories [["clojars" {:url "https://clojars.org/repo"
                                    :username :env/clojars_username
                                    :password :env/clojars_password
                                    :sign-releases false}]])
