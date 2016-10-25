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

(ns nvd.task.check
  (:require
   [clojure.string :as s]
   [clansi :refer [style]]
   [nvd.config :refer [with-config]]
   [nvd.report :refer [generate-report print-summary fail-build?]])
  (:import
   [org.owasp.dependencycheck Engine]))

(defn jar? [^String filename]
  (.endsWith filename ".jar"))

(defn absolute-path [file]
  (s/replace-first file #"^~" (System/getProperty "user.home")))

(defn- scan-and-analyze [project]
  (let [^Engine engine (:engine project)]
    (doseq [p (:classpath project)]
      (when (jar? p)
        (.scan engine (absolute-path p))))
    (.analyzeDependencies engine)
    project))

(defn conditional-exit [project]
  (if (:exit-after-check project)
    (System/exit (if (:failed? project) -1 0))
    project))

(defn -main [config-file]
  (with-config [project config-file]
    (println "Checking dependencies for" (style (:title project) :bright :yellow) "...")
    (-> project
        scan-and-analyze
        generate-report
        print-summary
        fail-build?
        conditional-exit)))
