(ns nvd.config-test
  (:require
   [clojure.java.io :as io]
   [clojure.string :as string]
   [clojure.test :refer [deftest is testing]]
   [nvd.config :as sut])
  (:import
   (java.util UUID)))

(def dependency-check-version
  (let [dependencies (-> "project.clj" io/file slurp read-string (nth 10))
        _ (assert (vector? dependencies))
        _ (assert (vector? (first dependencies)))
        found (->> dependencies
                   (some (fn [[d v]]
                           (when (= d 'org.owasp/dependency-check-core)
                             v))))]
    (assert (string? found))
    found))

(deftest check-app-name
  (is (= "stdin" (sut/app-name {:nome "hello-world" :version "0.0.1"})))
  (is (= "hello-world" (sut/app-name {:name "hello-world" :version "0.0.1"})))
  (is (= "hello-world" (sut/app-name {:name "hello-world" :group "hello-world" :version "0.0.1"})))
  (is (= "fred/hello-world" (sut/app-name {:name "hello-world" :group "fred" :version "0.0.1"}))))

(deftest check-with-config
  (sut/with-config [project "test/resources/opts.json"]
    (let [path (-> project (get-in [:nvd :data-directory]) io/file .getAbsolutePath)
          suffix-1 (-> dependency-check-version
                       (string/split #"\.")
                       first
                       (str ".0"))
          suffix-2 (->> (string/split dependency-check-version #"\.")
                        (take 2)
                        (string/join "."))
          expected-1 (str "/.m2/repository/org/owasp/dependency-check-utils/"
                          dependency-check-version
                          "/data/"
                          suffix-1)
          expected-2 (str "/.m2/repository/org/owasp/dependency-check-utils/"
                          dependency-check-version
                          "/data/"
                          suffix-2)]
      (is (or (.endsWith path expected-1)
              (.endsWith path expected-2)
              (.endsWith path "7.0")) ;; In recent releases, there's e.g. .../org/owasp/dependency-check-utils/8.0.2/data/7.0 which breaks the traditional match between versions
          (pr-str {:expected-1 expected-1
                   :expected-2 expected-2
                   :actual path})))
    (is (= (get-in project [:nvd :suppression-file]) "suppress.xml"))
    (is (false? (get-in project [:nvd :analyzer :assembly-enabled])))
    (is (true? (get-in project [:nvd :analyzer :cmake-enabled])))
    (is (not (nil? (get-in project [:engine]))))
    (is (= (get project :title) "barry-fungus/test-project 1.0.3-SNAPSHOT"))
    (is (= (get project :config-file) "test/resources/opts.json"))
    (is (= (get project :cmd-args) "12 -t hello"))
    (is (= (get project :classpath) ["file1.jar", "file2.jar"]))))

(deftest maybe-create-edn-file!
  (assert (not (-> sut/default-edn-config-filename io/file .exists)))

  (try
    (testing "Does not overwrite a config file with a name different from the default filename"
      (let [other-filename (-> (UUID/randomUUID) (str ".edn"))
            content (-> (UUID/randomUUID) str)]
        (try
          (spit other-filename content)
          (sut/maybe-create-edn-file! other-filename)
          (is (= content
                 (slurp other-filename)))
          (finally
            (-> other-filename io/file .delete)))))

    (testing "Does not overwrite a config file that happened to have the default filename"
      (let [content (-> (UUID/randomUUID) str)]
        (spit sut/default-edn-config-filename content)
        (sut/maybe-create-edn-file! sut/default-edn-config-filename)
        (is (= content
               (slurp sut/default-edn-config-filename)))))

    (-> sut/default-edn-config-filename io/file .delete)

    (testing "Writes the default content when the file didn't exist"
      (sut/maybe-create-edn-file! sut/default-edn-config-filename)
      (is (= @sut/default-config-content
             (slurp sut/default-edn-config-filename))))

    (finally
      (-> sut/default-edn-config-filename io/file .delete))))
