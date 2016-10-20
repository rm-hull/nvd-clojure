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
   [clansi :refer [style]]
   [nvd.config :as config])
  (:import
   [org.owasp.dependencycheck Engine]
   [org.owasp.dependencycheck.reporting ReportGenerator]))

(defn  generate-report [project]
  (let [^Engine engine (:engine project)
        title (:title project)
        output-dir (get-in project [:nvd :output-dir] "target/nvd")
        output-fmt (get-in project [:nvd :output-format] "ALL")
        db-props (:db-props project)
        deps (.getDependencies engine)
        analyzers (.getAnalyzers engine)
        rg (ReportGenerator. title deps analyzers db-props)]
    (.generateReports rg output-dir output-fmt)
    project))

(defn- vulnerabilities [engine]
  (apply concat
         (for [dep (.getDependencies engine)]
           (set (.getVulnerabilities dep)))))

(defn print-summary [project]
  (let [^Engine engine (:engine project)]
    (doseq [vuln (vulnerabilities engine)]
      (println (style vuln :red :bright)))
    project))
