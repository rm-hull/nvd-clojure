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
   [clojure.data.json :as json]
   [clojure.edn :as edn]
   [clojure.java.classpath :as cp]
   [clojure.java.io :as io]
   [clojure.string :as s]
   [clojure.tools.deps.alpha :as deps]
   [clansi :refer [style]]
   [nvd.config :refer [with-config]]
   [nvd.report :refer [generate-report print-summary fail-build?]]
   [trptcolin.versioneer.core :refer [get-version]])
  (:import
   [java.io File]
   [org.owasp.dependencycheck Engine]
   [org.owasp.dependencycheck.exception ExceptionCollection]))

(defonce version
  {:nvd-clojure (get-version "rm-hull" "nvd-clojure")
   :dependency-check (.getImplementationVersion (.getPackage Engine))})

(defn jar? [^String filename]
  (.endsWith filename ".jar"))

(defn ^String absolute-path [file]
  (s/replace-first file #"^~" (System/getProperty "user.home")))

(defn- scan-and-analyze [project]
  (let [^Engine engine (:engine project)]
    (doseq [p (:classpath project)]
      (when (jar? p)
        (.scan engine (absolute-path p))))
    (try
      (.analyzeDependencies engine)
      (catch ExceptionCollection e
        (println "Encountered errors while analyzing:" (.getMessage e))
        (doseq [exc (.getExceptions e)]
          (println exc))
        (let [exception-info (ex-info (str `ExceptionCollection)
                                      {:exceptions (.getExceptions e)})]
          (throw exception-info))))
    project))

(defn conditional-exit [project]
  (if (:exit-after-check project)
    (System/exit (if (:failed? project) -1 0))
    project))

(defn jvm-version []
  (as-> (System/getProperty "java.version") $
    (s/split $ #"\.")
    (take 2 $)
    (s/join "." $)
    (Float/parseFloat $)))

(defn make-classpath []
  (let [{:keys [classpath fun]} (if (> (jvm-version) 1.8)
                                  {:classpath (cp/system-classpath)
                                   :fun (fn [jar] (.getPath jar))}
                                  {:classpath (.getURLs (ClassLoader/getSystemClassLoader))
                                   :fun       (fn [jar] (.getFile jar))})]
    (map fun classpath)))

(defn clojure-cli-classpath
  "Read deps.edn and derive the classpath from its artifacts."
  []
  (-> (slurp "deps.edn")
      edn/read-string
      (deps/resolve-deps nil)
      (deps/make-classpath nil nil)
      (s/split #":")))

(defn -main [& [config-filename classpath-string]]
  ;; specifically handle blank strings (in addition to nil)
  ;; so that CLI callers can skip the first argument by simply passing an empty string:
  (if-not (s/blank? config-filename)
    (with-config [project config-filename]
      (println "Checking dependencies for" (style (:title project) :bright :yellow) "...")
      (println "  using nvd-clojure:" (:nvd-clojure version) "and dependency-check:" (:dependency-check version))
      (-> project
          scan-and-analyze
          generate-report
          print-summary
          fail-build?
          conditional-exit))
    (let [passed-classpath-string? (not (s/blank? classpath-string))
          f (java.io.File/createTempFile ".clj-nvd_" ".json")
          classpath (cond
                      passed-classpath-string?       (s/split classpath-string #":")
                      (.exists (io/file "deps.edn")) (clojure-cli-classpath)
                      :else                          (make-classpath))
          json-str (json/write-str {"classpath" classpath})]

      ;; perform some sanity checks for ensuring the calculated classpath has the expected format,
      ;; regardless of whether it came from Lein, deps.edn or stdin:
      (assert (-> classpath first File. .exists)
              "The classpath variable should be a vector of simple strings denoting existing files")
      (assert (-> classpath last File. .exists)
              "The classpath variable should be a vector of simple strings denoting existing files")

      (spit f json-str)
      (-main (.getCanonicalPath f)))))
