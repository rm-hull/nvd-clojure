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

(ns nvd.config
  (:require
   [clojure.string :as s]
   [clojure.java.io :as io]
   [clojure.data.json :as json])
  (:import
   [org.owasp.dependencycheck Engine]
   [org.owasp.dependencycheck.data.nvdcve CveDB DatabaseProperties]
   [org.owasp.dependencycheck.utils Settings Settings$KEYS]))

(def ^:private string-mappings
  {Settings$KEYS/ANALYZER_NEXUS_URL [:analyzer :nexus-url]
   Settings$KEYS/ANALYZER_ASSEMBLY_MONO_PATH [:analyzer :path-to-mono]
   Settings$KEYS/SUPPRESSION_FILE [:suppression-file]
   Settings$KEYS/ADDITIONAL_ZIP_EXTENSIONS [:zip-extensions]
   Settings$KEYS/PROXY_SERVER [:proxy :server]
   Settings$KEYS/PROXY_PORT [:proxy :port]
   Settings$KEYS/PROXY_USERNAME [:proxy :user]
   Settings$KEYS/PROXY_PASSWORD [:proxy :password]
   Settings$KEYS/CONNECTION_TIMEOUT [:database :connection-timeout]
   Settings$KEYS/DATA_DIRECTORY [:data-directory]
   Settings$KEYS/DB_DRIVER_NAME [:database :driver-name]
   Settings$KEYS/DB_DRIVER_PATH [:database :driver-path]
   Settings$KEYS/DB_CONNECTION_STRING [:database :connection-string]
   Settings$KEYS/DB_USER [:database :user]
   Settings$KEYS/DB_PASSWORD [:database :password]
   Settings$KEYS/CVE_MODIFIED_12_URL [:cve :url-1.2-modified]
   Settings$KEYS/CVE_MODIFIED_20_URL [:cve :url-2.0-modified]
   Settings$KEYS/CVE_SCHEMA_1_2 [:cve :url-1.2-base]
   Settings$KEYS/CVE_SCHEMA_2_0 [:cve :url-2.0-base]})

(def ^:private boolean-mappings
  {Settings$KEYS/AUTO_UPDATE [:auto-update]
;  Settings$KEYS/ANALYZER_EXPERIMENTAL_ENABLED [:analyzer :experimental-enabled]
   Settings$KEYS/ANALYZER_JAR_ENABLED [:analyzer :jar-enabled]
   Settings$KEYS/ANALYZER_PYTHON_DISTRIBUTION_ENABLED [:analyzer :python-distribution-enabled]
   Settings$KEYS/ANALYZER_PYTHON_PACKAGE_ENABLED [:analyzer :python-package-enabled]
   Settings$KEYS/ANALYZER_RUBY_GEMSPEC_ENABLED [:analyzer :ruby-gemspec-enabled]
   Settings$KEYS/ANALYZER_OPENSSL_ENABLED [:analyzer :openssl-enabled]
   Settings$KEYS/ANALYZER_CMAKE_ENABLED [:analyzer :cmake-enabled]
   Settings$KEYS/ANALYZER_AUTOCONF_ENABLED [:analyzer :autoconf-enabled]
   Settings$KEYS/ANALYZER_COMPOSER_LOCK_ENABLED [:analyzer :composer-lock-enabled]
   Settings$KEYS/ANALYZER_NODE_PACKAGE_ENABLED [:analyzer :node-package-enabled]
   Settings$KEYS/ANALYZER_NUSPEC_ENABLED [:analyzer :nuspec-enabled]
   Settings$KEYS/ANALYZER_CENTRAL_ENABLED [:analyzer :central-enabled]
   Settings$KEYS/ANALYZER_NEXUS_ENABLED [:analyzer :nexus-enabled]
   Settings$KEYS/ANALYZER_ARCHIVE_ENABLED [:analyzer :archive-enabled]
   Settings$KEYS/ANALYZER_ASSEMBLY_ENABLED [:analyzer :assembly-enabled]
   Settings$KEYS/ANALYZER_NEXUS_USES_PROXY [:analyzer :nexus-uses-proxy]})

(defn app-name [project]
  (let [name (get project :name "unknown")
        group (get project :group name)]
    (if (= group name)
      name
      (str group "/" name))))

(defn- read-opts [config-file]
  (json/read-str (slurp config-file) :key-fn keyword))

(def default-settings
  {:exit-after-check true
   :delete-config? true
   :nvd {:analyzer {:assembly-enabled false}}})

(defn- deep-merge [a b]
  (merge-with (fn [x y]
                (cond (map? y) (deep-merge x y)
                      (vector? y) (concat x y)
                      :else y))
              a b))

(defn populate-settings! [config-file]
  (let [project (deep-merge default-settings (read-opts config-file))
        plugin-settings (:nvd project)]
    (Settings/initialize)
    (when-let [cve-valid-for-hours (get-in plugin-settings [:cve :valid-for-hours])]
      (Settings/setInt Settings$KEYS/CVE_CHECK_VALID_FOR_HOURS cve-valid-for-hours))
    (doseq [[prop path] boolean-mappings]
      (Settings/setBooleanIfNotNull prop (get-in plugin-settings path)))
    (doseq [[prop path] string-mappings]
      (Settings/setStringIfNotEmpty prop (str (get-in plugin-settings path))))
    (->
     project
     (assoc-in [:nvd :data-directory] (Settings/getDataDirectory))
     (assoc
      :engine (Engine.)
      :title (str (app-name project) " " (:version project))
      :start-time (System/currentTimeMillis)
      :config-file config-file))))

(defn- ^DatabaseProperties db-props []
  (let [cve (CveDB.)]
    (try
      (.open cve)
      (.getDatabaseProperties cve)
      (finally
        (.close cve)))))

(defn cleanup [project]
  (.cleanup ^Engine (:engine project))
  (Settings/cleanup true)
  (when (:delete-config? project)
    (.deleteOnExit (io/file (:config-file project)))))

(defmacro with-config
  [[binding config-file] & body]
  (cond
    (symbol? binding)
    `(let [~binding (populate-settings! ~config-file)]
       (try
         ~@body
         (finally
           (cleanup ~binding))))

    :else
    (throw (IllegalArgumentException. "with-config only allows Symbols in binding"))))
