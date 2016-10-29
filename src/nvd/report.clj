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
   [org.owasp.dependencycheck Engine]
   [org.owasp.dependencycheck.dependency Dependency Vulnerability]
   [org.owasp.dependencycheck.reporting ReportGenerator]))

(def default-output-dir "target/nvd")

(defn  generate-report [project]
  (let [^Engine engine (:engine project)
        title (:title project)
        output-dir (get-in project [:nvd :output-dir] default-output-dir)
        output-fmt (get-in project [:nvd :output-format] "ALL")
        db-props (:db-props project)
        deps (.getDependencies engine)
        analyzers (.getAnalyzers engine)
        rg (ReportGenerator. title deps analyzers db-props)]
    (.generateReports rg output-dir output-fmt)
    project))

(defn- severity [cvssScore]
  (cond
    (= cvssScore 0) :none
    (< cvssScore 4) :low
    (>= cvssScore 7) :high
    :else :medium))

(defn- color [severity]
  (get {:none :green :low :cyan :medium :yellow :high :red} severity))

(defn- vuln-status [^Dependency dep]
  (let [vulns (.getVulnerabilities dep)]
    (if (empty? vulns)
      (style "OK" :green :bright)
      (s/join ", "
              (for [^Vulnerability v vulns
                    :let [color (-> (.getCvssScore v) severity color)]]
                (style (.getName v) color :bright))))))

(defn- vulnerabilities [^Engine engine]
  (sort-by :dependency
           (for [^Dependency dep (.getDependencies engine)]
             {:dependency (.getFileName dep) :status (vuln-status dep)})))

(defn- scores [^Engine engine]
  (flatten (for [^Dependency dep (.getDependencies engine)
                 ^Vulnerability vuln (.getVulnerabilities dep)]
             (.getCvssScore vuln))))

(defn print-summary [project]
  (let [^Engine engine (:engine project)
        output-dir (get-in project [:nvd :output-dir] default-output-dir)
        summary (vulnerabilities engine)
        scores  (scores engine)
        highest-score (apply max 0 scores)
        color (-> highest-score severity color)
        severity (-> highest-score severity name s/upper-case)]
    (table summary)
    (println)
    (print (count scores) "vulnerabilities detected. Severity: ")
    (println (style severity color :bright))
    (println "Detailed reports saved in:" (style (.getAbsolutePath (io/file output-dir)) :bright))
    (println)
    project))

(defn fail-build? [project]
  (let [^Engine engine (:engine project)
        highest-score (apply max 0 (scores engine))
        fail-threshold (get-in project [:nvd :fail-threshold] 0)]
    (->
     project
     (assoc-in [:nvd :highest-score] highest-score)
     (assoc :failed? (> highest-score fail-threshold)))))
