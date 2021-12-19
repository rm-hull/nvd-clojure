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

(ns nvd.config-test
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is]]
   [nvd.config :refer [app-name with-config]]))

(def dependency-check-version "6.5.1")

(deftest check-app-name
  (is (= "stdin" (app-name {:nome "hello-world" :version "0.0.1"})))
  (is (= "hello-world" (app-name {:name "hello-world" :version "0.0.1"})))
  (is (= "hello-world" (app-name {:name "hello-world" :group "hello-world" :version "0.0.1"})))
  (is (= "fred/hello-world" (app-name {:name "hello-world" :group "fred" :version "0.0.1"}))))

(deftest check-with-config
  (with-config [project "test/resources/opts.json"]
    (let [path (-> project (get-in [:nvd :data-directory]) io/file .getAbsolutePath)]
      (is (-> path
              (.endsWith (str "/.m2/repository/org/owasp/dependency-check-utils/"
                              dependency-check-version
                              "/data")))
          path))
    (is (= (get-in project [:nvd :suppression-file]) "suppress.xml"))
    (is (false? (get-in project [:nvd :analyzer :assembly-enabled])))
    (is (true? (get-in project [:nvd :analyzer :cmake-enabled])))
    (is (not (nil? (get-in project [:engine]))))
    (is (= (get-in project [:title]) "barry-fungus/test-project 1.0.3-SNAPSHOT"))
    (is (= (get-in project [:config-file]) "test/resources/opts.json"))
    (is (= (get-in project [:cmd-args]) "12 -t hello"))
    (is (= (get-in project [:classpath]) ["file1.jar", "file2.jar"]))))
