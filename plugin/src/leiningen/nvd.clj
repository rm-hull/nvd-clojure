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

(ns leiningen.nvd
  (:require
   [clojure.string :as s]
   [clojure.data.json :as json]
   [leiningen.core.main :as main]
   [leiningen.core.eval :refer [eval-in-project]]
   [leiningen.core.project :as p :refer [merge-profiles]]
   [leiningen.nvd.deps :refer [get-classpath]]))

(defn get-lib-version []
  (or (System/getenv "NVD_VERSION") "RELEASE"))

(defn nvd-profile []
  {:dependencies [['org.clojure/clojure "1.8.0"]
                  ['nvd-clojure (get-lib-version)]]})

(def temp-file (java.io.File/createTempFile ".lein-nvd_" ".json"))

(defn nvd "
  Scans project dependencies, attempting to detect publicly disclosed
  vulnerabilities contained within dependent JAR files. It does this by
  determining if there is a Common Platform Enumeration (CPE) identifier
  for a given dependency. On completion, a summary table is displayed on
  the console (showing the status for each dependency), and detailed report
  linking to the associated CVE entries.

  This task should be invoked with one of three commands:

      check  - will optionally download the latest database update files,
               and then run the analyze and report stages. Typically, if
               the database has been updated recently, then the update
               stage will be skipped.

      purge  - will remove the local database files. Subsequently running
               the 'check' command will force downloading the files again,
               which could take a long time.

      update - will attempt to download the latest database updates, and
               incorporate them into the local store. Usually not necessary,
               as this is incorporated into the 'check' command.

  Any text after the command are treated as arguments and are passed directly
  directly to the command for further processing."
  [project command & args]
  (let [profile (merge (:nvd (:profiles project)) (nvd-profile))
        project (merge-profiles project [profile])
        path (.getAbsolutePath temp-file)
        opts    (merge
                 (select-keys project [:name :group :version :nvd])
                 {:classpath (get-classpath project) :cmd-args args})]

    (spit path (json/write-str opts))

    (case command
      "check"  (eval-in-project project `(nvd.task.check/-main ~path) '(require 'nvd.core))
      "purge"  (eval-in-project project `(nvd.task.purge-database/-main ~path) '(require 'nvd.core))
      "update" (eval-in-project project `(nvd.task.update-database/-main ~path) '(require 'nvd.core))
      (main/abort "No such command:" command))))
