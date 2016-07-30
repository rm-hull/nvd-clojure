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
    [clojure.java.io :as io]
    [leiningen.run :as run]
    [leiningen.core.main :as main]
    [leiningen.core.classpath :refer [get-classpath]])
  (:import
    [org.slf4j.impl StaticLoggerBinder]
    [org.owasp.dependencycheck Engine]
    [org.owasp.dependencycheck.data.nvdcve CveDB DatabaseException DatabaseProperties]
    [org.owasp.dependencycheck.reporting ReportGenerator]
    [org.owasp.dependencycheck.utils Settings Settings$KEYS]))

; Call this before Dependency Check Core starts logging anything -
; this way, all SLF4J messages from core end up coming through this tasks logger
;(.setTask (StaticLoggerBinder/getSingleton) this)

(def ^:private string-mappings {
  Settings$KEYS/ANALYZER_NEXUS_URL [:analyzer :nexus-url]
  Settings$KEYS/ANALYZER_ASSEMBLY_MONO_PATH [:analyzer :path-to-mono]
  Settings$KEYS/SUPPRESSION_FILE [:suppression-file]
  Settings$KEYS/ADDITIONAL_ZIP_EXTENSIONS [:zip-extensions]
  Settings$KEYS/PROXY_SERVER [:proxy :server]
  Settings$KEYS/PROXY_PORT [:proxy :port]
  Settings$KEYS/PROXY_USERNAME [:proxy :user]
  Settings$KEYS/PROXY_PASSWORD [:proxy :password]
  Settings$KEYS/CONNECTION_TIMEOUT [:database :connection-timeout]
  Settings$KEYS/DB_DRIVER_NAME [:database :driver-name]
  Settings$KEYS/DB_DRIVER_PATH [:database :driver-path]
  Settings$KEYS/DB_CONNECTION_STRING [:database :connection-string]
  Settings$KEYS/DB_USER [:database :user]
  Settings$KEYS/DB_PASSWORD [:database :password]
  Settings$KEYS/CVE_MODIFIED_12_URL [:cve :url-1.2-modified]
  Settings$KEYS/CVE_MODIFIED_20_URL[:cve :url-2.0-modified]
  Settings$KEYS/CVE_SCHEMA_1_2 [:cve :url-1.2-base]
  Settings$KEYS/CVE_SCHEMA_2_0 [:cve :url-2.0-base]})

(def ^:private boolean-mappings {
  Settings$KEYS/AUTO_UPDATE [:auto-update]
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

(defn- app-name [project]
  (let [name (:name project)
        group (:group project)]
    (if (= group name)
      (:name project)
      (str group "/" name))))

(defn- populate-settings! [project]
  (Settings/initialize)
  (let [plugin-settings (:nvd project)]
    (when-let [cve-valid-for-hours (get-in plugin-settings [:cve :valid-for-hours])]
      (Settings/setInt Settings$KEYS/CVE_CHECK_VALID_FOR_HOURS cve-valid-for-hours))
    (if-let [data-directory (get-in plugin-settings [:data-directory])]
      (Settings/setString Settings$KEYS/DATA_DIRECTORY data-directory)
      (Settings/setString Settings$KEYS/DATA_DIRECTORY (str (System/getProperty "user.home") "/.lein/.nvd")))
    (doseq [[prop path] boolean-mappings]
      (Settings/setBooleanIfNotNull prop (get-in plugin-settings path)))
    (doseq [[prop path] string-mappings]
      (Settings/setStringIfNotEmpty prop (str (get-in plugin-settings path))))))

(defn- ^Engine create-engine []
  (Engine.))

(defn update-database!
  "Download the latest data from the National Vulnerability Database
  (NVD) and store a copy in the local database."

  [project & args]
  (let [engine (create-engine)]
    (try
      (.doUpdates engine)
      (finally
        (.cleanup engine)))))

(defn purge-database!
  [project & args]
  (let [db (io/file (Settings/getDataDirectory) "dc.h2.db")]
    (when (.exists db)
      (.delete db)
      (main/info "Database file purged; local copy of the NVD has been removed"))))

(defn- scan-and-analyze [^Engine engine project]
  (doseq [p (get-classpath project)]
    (when (s/ends-with? p ".jar")
      (.scan engine (str p))))
  (.analyzeDependencies engine))

(defn- ^DatabaseProperties db-props []
  (let [cve (CveDB.)]
    (try
      (.open cve)
      (.getDatabaseProperties cve)
      (finally
        (.close cve)))))

(defn- generate-report [^Engine engine project]
  (let [app-name (str (app-name project) " " (:version project))
        output-dir (get-in project [:nvd :output-dir] "target/nvd")
        output-fmt (get-in project [:nvd :output-format] "ALL")
        deps (.getDependencies engine)
        analyzers (.getAnalyzers engine)
        db-props (db-props)
        r (ReportGenerator. app-name deps analyzers db-props)]
    (.generateReports r output-dir output-fmt)))

(defn check
  [project & args]
  (let [engine (create-engine)]
    (try
      (scan-and-analyze engine project)
      (generate-report engine project)
      (finally
        (.cleanup engine)))))

(defn nvd
  "Scan project dependencies and report known vulnerabilities."
  [project & args]
  (populate-settings! project)
  (try
    (let [subtask (first args)
          run #(apply % project (rest args))]
      (case subtask
        "check" (run check)
        "purge" (run purge-database!)
        "update" (run update-database!)
        (main/abort "No such subtask:" subtask)))
    (finally
      (Settings/cleanup true))))
