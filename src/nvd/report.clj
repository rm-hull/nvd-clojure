;; The MIT License (MIT)
;;
;; Copyright (c) 2016 Richard Hull
;;
;; Permission is hereby granted, free of charge, to any person obtaining a copy
;; of this software and associated documentation files (the "Software"), to deal
;; in the Software without restriction, including without limitation the rights
;; to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
;; copies of the Software, and to permit persons to whom the Software is
;; furnished to do so, subject to the following conditions:
;;
;; The above copyright notice and this permission notice shall be included in all
;; copies or substantial portions of the Software.
;;
;; THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
;; IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
;; FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
;; AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
;; LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
;; OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
;; SOFTWARE.

(ns nvd.report
  (:require
   [clojure.string :as s]
   [clojure.java.io :as io]
   [clansi :refer [style]]
   [table.core :refer [table]]
   [nvd.config :as config])
  (:import
   [java.util Arrays]
   [org.owasp.dependencycheck Engine]
   [org.owasp.dependencycheck.dependency Dependency Vulnerability]
   [org.owasp.dependencycheck.reporting ReportGenerator]))

(def default-output-dir "target/nvd")

(defn  generate-report [project]
  (let [^Engine engine (:engine project)
        title (:title project)
        output-dir (get-in project [:nvd :output-dir] default-output-dir)
        output-fmt (get-in project [:nvd :output-format] "ALL")
        db-props (.getDatabaseProperties (.getDatabase engine))
        deps (Arrays/asList (.getDependencies engine))
        analyzers (.getAnalyzers engine)
        settings (.getSettings engine)
        rg (ReportGenerator. title deps analyzers db-props settings)]
    (.write rg ^String output-dir ^String output-fmt)
    project))

(defn- score [^Vulnerability vulnerability]
  (let [cvss2 (.getCvssV2 vulnerability)
        cvss3 (.getCvssV3 vulnerability)]
    (cond
      cvss2 (.getScore cvss2)
      cvss3 (.getBaseScore cvss3)
      :else 1)))

(defn- severity [cvssScore]
  (cond
    (= cvssScore 0) :none
    (< cvssScore 4) :low
    (>= cvssScore 7) :high
    :else :medium))

(defn- color [severity]
  (get {:none :green :low :cyan :medium :yellow :high :red} severity))

(defn- vulnerable? [^Dependency dep]
  (not-empty (.getVulnerabilities dep)))

(defn- vuln-status [^Dependency dep]
  (if-not (vulnerable? dep)
    (style "OK" :green :bright)
    (s/join ", "
            (for [^Vulnerability v (reverse (sort-by score (.getVulnerabilities dep)))
                  :let [color (-> v score severity color)]]
              (style (.getName v) color :bright)))))

(defn- dependency-name-and-version [dependency]
  (subvec dependency 0 2))

(defn- dep-path-as-string
  ([dep-path]
   (dep-path-as-string
    dependency-name-and-version
    dep-path))
  ([dep-transform-fn dep-path]
   (cond
     (nil? dep-path) "<NO PATH FOUND>"
     (= 1 (count  dep-path)) "<TOP_LEVEL DEPENDENCY>"
     :else (some->> dep-path
                    (mapv dep-transform-fn)
                    (s/join " -> ")))))

(defn- dependency-path
  [{:keys [classpath-deps-with-paths-list] :as _project}
   ;; Check https://github.com/jeremylong/DependencyCheck/blob/master/core/src/main/java/org/owasp/dependencycheck/dependency/Dependency.java
   dependency-name]
  (let [;; this is needed because dependency name is of format "groupId:artifactId"
        artifact-dependency-name
        (-> dependency-name (s/split #":") last)

        matching-deps
        (filterv
         (fn match-dependency [path-to-dependency]
           (let [[dep-name _dep-version :as _dependency] (peek path-to-dependency)]
             (= artifact-dependency-name dep-name)))
         classpath-deps-with-paths-list)]
    (when (= 1 (count  matching-deps))
      (first matching-deps))))

(defn- vulnerabilities [project ^Engine engine]
  (sort-by :dependency
           (for [^Dependency dep (.getDependencies engine)
                 :when (or (vulnerable? dep) (:verbose-summary project))]
             (let [dep-path (dependency-path project (.getName dep))]
               {:dependency (.getFileName dep)
                :status (vuln-status dep)
                :dependency-path-from-root (dep-path-as-string dep-path)}))))

(defn- scores [^Engine engine]
  (flatten (for [^Dependency dep (.getDependencies engine)
                 ^Vulnerability vuln (.getVulnerabilities dep)]
             (score vuln))))

(defn print-summary [project]
  (let [^Engine engine (:engine project)
        output-dir (get-in project [:nvd :output-dir] default-output-dir)
        summary (vulnerabilities project engine)
        scores  (scores engine)
        highest-score (apply max 0 scores)
        color (-> highest-score severity color)
        severity (-> highest-score severity name s/upper-case)]

    (when (or (:verbose-summary project) (pos? (count scores)))
      (table (map #(dissoc % :dependency-path-from-root)
                  summary))
      (println "(Transitive) paths from project's root to vulnerable dependencies:")
      (println "------------------------------------------------------------------")
      (doseq [{:keys [dependency dependency-path-from-root]} summary]
        (println " *" dependency ": " dependency-path-from-root))
      (println "------------------------------------------------------------------"))

    (println)
    (print (count scores) "vulnerabilities detected. Severity: ")
    (println (style severity color :bright))
    (println "Detailed reports saved in:" (style (.getAbsolutePath (io/file output-dir)) :bright))
    (println)
    (println (style "   *** THIS REPORT IS WITHOUT WARRANTY ***" :magenta :bright))
    project))

(defn fail-build? [project]
  (let [^Engine engine (:engine project)
        highest-score (apply max 0 (scores engine))
        fail-threshold (get-in project [:nvd :fail-threshold] 0)]
    (->
     project
     (assoc-in [:nvd :highest-score] highest-score)
     (assoc :failed? (> highest-score fail-threshold)))))
