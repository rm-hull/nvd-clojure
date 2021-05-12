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

(ns nvd.task.check-test
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer :all]
   [nvd.task.update-database :as update-db]
   [nvd.task.check :as check])
  (:import
   [java.util.zip GZIPInputStream]))

(defn gunzip
  ; Attribution: https://gist.github.com/bpsm/1858654
  "Writes the contents of input to output, decompressed.
  input: something which can be opened by io/input-stream.
      The bytes supplied by the resulting stream must be gzip compressed.
  output: something which can be copied to by io/copy."
  [input output & opts]
  (with-open [input (-> input io/file io/input-stream GZIPInputStream.)]
    (apply io/copy input (io/file output) opts)))

(deftest self-check
  (gunzip "test/resources/dc.h2.db.gz" "test/resources/dc.h2.db")
  (update-db/-main "test/resources/self-test.json")
  (let [project (check/-main "test/resources/self-test.json")]
    (is (== 11.0 (get-in project [:nvd :fail-threshold])))
    (is (== 0 (get-in project [:nvd :highest-score])))
    (is (false? (project :failed?)))))

(deftest classpath-test
  (let [clojure "clojure-1.10.3.jar"]
    (is (true? (.contains (pr-str (check/clojure-cli-classpath)) clojure)))
    (is (true? (.contains (pr-str (check/make-classpath)) clojure)))))
