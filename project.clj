(defproject nvd-clojure "5.1.0"
  :description "National Vulnerability Database dependency checker"
  :url "https://github.com/rm-hull/nvd-clojure"
  :license {:name "The MIT License (MIT)"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[clansi "1.0.0"]

                 ;; dependency-check-core transitively brings in two versions of
                 ;; these dependencies, so we explicitly depend on the latest
                 [com.google.errorprone/error_prone_annotations "2.42.0"]
                 [commons-logging/commons-logging "1.3.5"]

                 [org.clojure/clojure "1.12.3"]
                 [org.clojure/data.json "2.5.1"]
                 [org.slf4j/slf4j-simple "2.0.17"]
                 [org.owasp/dependency-check-core "12.1.6" :exclusions [commons-logging]]

                 [rm-hull/table "0.7.1"]

                 [trptcolin/versioneer "0.2.0"]]
  :managed-dependencies [[com.google.code.gson/gson "2.13.2"]]
  :scm {:url "git@github.com:rm-hull/nvd-clojure.git"}
  :source-paths ["src"]
  :jar-exclusions [#"(?:^|/).git"]
  :codox {:source-paths ["src"]
          :output-path "doc/api"
          :source-uri "https://github.com/rm-hull/nvd-clojure/blob/main/{filepath}#L{line}"}
  :min-lein-version "2.8.1"
  :target-path "target/%s"
  :jvm-opts ["-Dclojure.main.report=stderr"]
  :profiles {:dev {:plugins [[jonase/eastwood "1.4.0"]

                             [lein-ancient "0.7.0"]
                             [lein-cljfmt "0.7.0"]
                             [lein-cloverage "1.2.3"]
                             [lein-codox "0.10.7"]]
                   :eastwood {:add-linters [:boxed-math
                                            :performance]}
                   :dependencies [[clj-kondo "2025.09.22"]
                                  [commons-collections "20040616"]]}
             :ci {:pedantic? :abort}
             :clj-kondo {:dependencies [[clj-kondo "2025.09.22"]]}
             :skip-self-check {:jvm-opts ["-Dnvd-clojure.internal.skip-self-check=true"]}}
  :deploy-repositories [["clojars" {:url "https://clojars.org/repo"
                                    :username :env/clojars_username
                                    :password :env/clojars_password
                                    :sign-releases false}]])
