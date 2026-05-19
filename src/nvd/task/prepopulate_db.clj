;; The MIT License (MIT)
;;
;; Copyright (c) 2026- bevuta IT GmbH
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

(ns nvd.task.prepopulate-db
  (:require
   [clansi :refer [style]]
   [clojure.string :as s]
   [nvd.config :refer [default-edn-config-filename with-config]]
   [trptcolin.versioneer.core :refer [get-version]])
  (:import
   (org.owasp.dependencycheck Engine)
   (org.owasp.dependencycheck.exception ExceptionCollection)))

(def version
  (delay {:nvd-clojure (get-version "nvd-clojure" "nvd-clojure")
          :dependency-check (.getImplementationVersion (.getPackage Engine))}))

(defn do-updates [project]
  (let [^Engine engine (:engine project)]
    (try
      (.doUpdates engine)
      (catch ExceptionCollection e
        (println "Encountered errors while analyzing:" (.getMessage e))
        (doseq [exc (.getExceptions e)]
          (println exc))
        (let [exception-info (ex-info (str `ExceptionCollection)
                                      {:exceptions (.getExceptions e)})]
          (throw exception-info))))
    project))

(defn impl [config-filename]
  (with-config [project config-filename]
    (println "Prepopulating database for" (-> project
                                              :title
                                              (s/trim)
                                              (str "...")
                                              (style :bright :yellow)))
    (println "  using nvd-clojure:" (:nvd-clojure @version) "and dependency-check:" (:dependency-check @version))
    (-> project
        do-updates)
    ;; If we got here, it's all good. Otherwise, we'd be throwing an exception
    (System/exit 0)))

(defn -main [& [config-filename]]
  ;; specifically handle blank strings (in addition to nil)
  ;; so that CLI callers can skip the first argument by simply passing an empty string:
  (let [config-filename (if (s/blank? config-filename)
                          default-edn-config-filename
                          config-filename)]
    (impl config-filename)))

