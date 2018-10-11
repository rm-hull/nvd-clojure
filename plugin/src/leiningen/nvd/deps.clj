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

(ns leiningen.nvd.deps
  (:import
   [java.io File PushbackReader])
  (:require
   [clojure.walk :refer [prewalk]]
   [clojure.string :as s]
   [clojure.java.io :as io]
   [cemerick.pomegranate.aether :as aether]
   [leiningen.core.classpath :refer [managed-dependency-hierarchy]]
   [leiningen.core.project :refer [read-raw]]))

(defn flatten-tree [deps]
  (apply concat
         (when deps
           (for [[dep subdeps] deps]
             (cons dep (lazy-seq (flatten-tree subdeps)))))))

(defn- raw-project-attributes []
  (read-raw "project.clj"))

(defn dependency? [elem]
  (and
   (vector? elem)
   (#{:dependencies :managed-dependencies} (first elem))))

(defn- project-deps []
  (let [deps (atom [])
        f    (fn [elem]
               (if (dependency? elem)
                 (do (swap! deps conj elem) nil)
                 elem))]
    (prewalk f (raw-project-attributes))
    (->>
     @deps
     (map (partial apply hash-map))
     (apply merge-with concat)
     vals
     (apply concat))))

(defn- jars [dependency-tree]
  (->>
   (project-deps)
   (select-keys dependency-tree)
   flatten-tree
   (map #(vector % nil))
   (into {})
   (aether/dependency-files)
   (map (memfn ^File getAbsolutePath))))

(defn get-classpath [project]
  (jars (managed-dependency-hierarchy :dependencies :managed-dependencies project)))
