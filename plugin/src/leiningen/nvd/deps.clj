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
   (#{:dependencies :managed-dependencies} (first elem))
   (seq? (second elem))))

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


(defn deps-with-paths-flat-list
  "Walks the dependency tree data structure (expects format provided by `lein deps :tree-data`)
  in the depth-first walk manner and builds a flat sequence of dependencies
  _including the whole path to that dependency_.
  This is important for *transitive dependencies*.
  That is if you only have a single top-level depedency A which in turn depends on AA and AB,
  where AB depends on ABA, then you'll get following as a result:
  ```
  [
   [[A]]
   [[A] [AA]]
   [[A] [AB]]
   [[A] [AB] [ABA]]
  ]
  ```

  where each element is a vector of depedencies a full path from a top-level dependency
  (the first element of the vector) to the \"final\" dependency (the last element of the vector)."
  [deps-tree tree-root-path]
  (reduce-kv
   (fn [deps-list dependency transitive-deps]
     (apply conj
            deps-list
            (conj tree-root-path dependency)
            (deps-with-paths-flat-list transitive-deps (conj tree-root-path dependency))))
   []
   deps-tree))

(defn deps-flat-list
  "Walks the dependency tree data structure (expects format provided by `lein deps :tree-data`)
  in the depth-first walk manner and builds a flat sequence of dependencies
  where each element is a vector representing a single atomic depedency
  consisting of dependency name as the first element,
  dependency version as the second element,
  optional exclusions, etc - the same format as in leiningen."
  [deps-tree]
  (let [deps-with-paths (deps-with-paths-flat-list deps-tree [])]
    (mapv peek deps-with-paths)))

(defn deps-flat-list-for-project [project]
  (deps-with-paths-flat-list (managed-dependency-hierarchy :dependencies :managed-dependencies project)
                             []))

#_(deps-flat-list-for-project my-project)
