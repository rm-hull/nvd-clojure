;; The MIT License (MIT)
;;
;; Copyright (c) 2016- Richard Hull
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
   [clojure.java.io :as io]
   [clojure.string :as s]
   [clansi :refer [style]]
   [nvd.config :refer [with-config]]
   [nvd.report :refer [generate-report print-summary fail-build?]]
   [trptcolin.versioneer.core :refer [get-version]])
  (:import
   [java.io File]
   [org.owasp.dependencycheck Engine]
   [org.owasp.dependencycheck.exception ExceptionCollection]))

(def version
  (delay {:nvd-clojure (get-version "nvd-clojure" "nvd-clojure")
          :dependency-check (.getImplementationVersion (.getPackage Engine))}))

(defn jar? [^String filename]
  (.endsWith filename ".jar"))

(defn absolute-path ^String [file]
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

(defn conditional-exit [{:keys [exit-after-check failed?]
                         {:keys [throw-if-check-unsuccessful?]} :nvd
                         :as project}]
  (cond
    (and failed? throw-if-check-unsuccessful?)
    (throw (ex-info "nvd-clojure failed / found vulnerabilities" {}))

    exit-after-check
    (System/exit (if failed? -1 0))

    :else project))

(defn jvm-version []
  (as-> (System/getProperty "java.version") $
    (s/split $ #"\.")
    (take 2 $)
    (s/join "." $)
    (Double/parseDouble $)))

(defn impl [config-filename classpath]
  (with-config [project config-filename]
    (println "Checking dependencies for" (-> project
                                             :title
                                             (s/trim)
                                             (str "...")
                                             (style :bright :yellow)))
    (println "  using nvd-clojure:" (:nvd-clojure @version) "and dependency-check:" (:dependency-check @version))
    (-> project
        (assoc :classpath classpath)
        scan-and-analyze
        generate-report
        print-summary
        fail-build?
        conditional-exit)))

(defn -main [& [config-filename ^String classpath-string]]
  (when (s/blank? classpath-string)
    (throw (ex-info "nvd-clojure requires a classpath value to be explicitly passed as a CLI argument.
Older usages are deprecated." {})))

  (let [classpath (s/split classpath-string #":")
        classpath (into []
                        (remove (fn [^String s]
                                  ;; Only .jar (and perhaps .zip) files are relevant.
                                  ;; source paths such as `src`, while are part of the classpath,
                                  ;; won't be meaningfully analyzed by dependency-check-core.
                                  ;; Keeping only .jars facilitates various usage patterns.
                                  (let [file (io/file s)]
                                    (or (.isDirectory file)
                                        (not (.exists file))))))
                        classpath)]

    (when-not (System/getProperty "nvd-clojure.internal.skip-self-check")
      (when-let [bad-entry (->> classpath
                                (some (fn [^String entry]
                                        (and (-> entry (.endsWith ".jar"))
                                             (when (or (-> entry (.contains "dependency-check-core"))
                                                       (-> entry (.contains "nvd-clojure")))
                                               entry)))))]
        (throw (ex-info "nvd-clojure should not analyse itself. This typically indicates a badly setup integration.

Please refer to the project's README for recommended usages."
                        {:bad-entry bad-entry
                         :classpath classpath-string}))))

    ;; perform some sanity checks for ensuring the calculated classpath has the expected format:
    (let [f (-> classpath ^String (first) File.)]
      (when-not (.exists f)
        (throw (ex-info (str "The classpath variable should be a vector of simple strings denoting existing files: "
                             (pr-str f))
                        {}))))

    (let [f (-> classpath ^String (last) File.)]
      (when-not (.exists f)
        (throw (ex-info (str "The classpath variable should be a vector of simple strings denoting existing files: "
                             (pr-str f))
                        {}))))

    ;; specifically handle blank strings (in addition to nil)
    ;; so that CLI callers can skip the first argument by simply passing an empty string:
    (let [config-filename (if-not (s/blank? config-filename)
                            config-filename
                            (let [f (java.io.File/createTempFile ".clj-nvd_" ".json")]
                              (spit f (json/write-str {"classpath" classpath}))
                              (.getCanonicalPath f)))]
      (impl config-filename classpath))))
