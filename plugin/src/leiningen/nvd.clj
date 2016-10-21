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

(defn nvd
  "Scan project dependencies and report known vulnerabilities."
  [project & args]
  (let [profile (merge (:nvd (:profiles project)) (nvd-profile))
        project (merge-profiles project [profile])
        path (.getAbsolutePath temp-file)
        subtask (first args)
        opts    (merge
                 (select-keys project [:name :group :version :nvd])
                 {:classpath (get-classpath project) :cmd-args (next args)})]

    (spit path (json/write-str opts))

    (case subtask
      "check"  (eval-in-project project `(nvd.task.check/-main ~path) '(require 'nvd.core))
      "purge"  (eval-in-project project `(nvd.task.purge/-main ~path) '(require 'nvd.core))
      "update" (eval-in-project project `(nvd.task.update/-main ~path) '(require 'nvd.core))
      (main/abort "No such subtask:" subtask))))
