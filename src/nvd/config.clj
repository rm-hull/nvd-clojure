;; The MIT License (MIT)
;;
;; Copyright (c) 2016- Richard Hull
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
   [clojure.data.json :as json]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.string :as string]
   [nvd.log :as log])
  (:import
   (java.io File)
   (org.owasp.dependencycheck Engine)
   (org.owasp.dependencycheck.utils Downloader Settings Settings$KEYS)))

(def ^:private string-mappings
  {Settings$KEYS/ANALYZER_NEXUS_URL        [:analyzer :nexus-url]
   Settings$KEYS/SUPPRESSION_FILE          [:suppression-file]
   Settings$KEYS/ADDITIONAL_ZIP_EXTENSIONS [:zip-extensions]
   Settings$KEYS/PROXY_SERVER              [:proxy :server]
   Settings$KEYS/PROXY_PORT                [:proxy :port]
   Settings$KEYS/PROXY_USERNAME            [:proxy :user]
   Settings$KEYS/PROXY_PASSWORD            [:proxy :password]
   Settings$KEYS/CONNECTION_TIMEOUT        [:database :connection-timeout]
   Settings$KEYS/DATA_DIRECTORY            [:data-directory]
   Settings$KEYS/DB_DRIVER_NAME            [:database :driver-name]
   Settings$KEYS/DB_DRIVER_PATH            [:database :driver-path]
   Settings$KEYS/DB_CONNECTION_STRING      [:database :connection-string]
   Settings$KEYS/DB_USER                   [:database :user]
   Settings$KEYS/DB_PASSWORD               [:database :password]
   Settings$KEYS/NVD_API_KEY               [:nvd-api :key]
   Settings$KEYS/NVD_API_ENDPOINT          [:nvd-api :endpoint]
   Settings$KEYS/NVD_API_DELAY             [:nvd-api :delay]
   Settings$KEYS/NVD_API_MAX_RETRY_COUNT   [:nvd-api :max-retry-count]
   Settings$KEYS/NVD_API_VALID_FOR_HOURS   [:nvd-api :valid-for-hours]
   Settings$KEYS/NVD_API_DATAFEED_URL      [:nvd-api :datafeed :url]
   Settings$KEYS/NVD_API_DATAFEED_USER     [:nvd-api :datafeed :user]
   Settings$KEYS/NVD_API_DATAFEED_PASSWORD [:nvd-api :datafeed :password]})

(def ^:private boolean-mappings
  {Settings$KEYS/ANALYZER_ARCHIVE_ENABLED                     [:analyzer :archive-enabled]
   Settings$KEYS/ANALYZER_ARTIFACTORY_ENABLED                 [:analyzer :artifactory-enabled]
   Settings$KEYS/ANALYZER_ASSEMBLY_ENABLED                    [:analyzer :assembly-enabled]
   Settings$KEYS/ANALYZER_AUTOCONF_ENABLED                    [:analyzer :autoconf-enabled]
   Settings$KEYS/ANALYZER_BUNDLE_AUDIT_ENABLED                [:analyzer :bundle-audit-enabled]
   Settings$KEYS/ANALYZER_CENTRAL_ENABLED                     [:analyzer :central-enabled]
   Settings$KEYS/ANALYZER_CMAKE_ENABLED                       [:analyzer :cmake-enabled]
   Settings$KEYS/ANALYZER_COCOAPODS_ENABLED                   [:analyzer :cocoapods-enabled]
   Settings$KEYS/ANALYZER_COMPOSER_LOCK_ENABLED               [:analyzer :composer-lock-enabled]
   Settings$KEYS/ANALYZER_CPANFILE_ENABLED                    [:analyzer :cpanfile-enabled]
   Settings$KEYS/ANALYZER_EXPERIMENTAL_ENABLED                [:analyzer :experimental-enabled]
   Settings$KEYS/ANALYZER_GOLANG_DEP_ENABLED                  [:analyzer :golang-dep-enabled]
   Settings$KEYS/ANALYZER_GOLANG_MOD_ENABLED                  [:analyzer :golang-mod-enabled]
   Settings$KEYS/ANALYZER_JAR_ENABLED                         [:analyzer :jar-enabled]
   Settings$KEYS/ANALYZER_MIX_AUDIT_ENABLED                   [:analyzer :mix-audit-enabled]
   Settings$KEYS/ANALYZER_MSBUILD_PROJECT_ENABLED             [:analyzer :msbuild-project-enabled]
   Settings$KEYS/ANALYZER_NEXUS_ENABLED                       [:analyzer :nexus-enabled]
   Settings$KEYS/ANALYZER_NEXUS_USES_PROXY                    [:analyzer :nexus-uses-proxy]
   Settings$KEYS/ANALYZER_NODE_AUDIT_ENABLED                  [:analyzer :node-audit-enabled]
   Settings$KEYS/ANALYZER_NODE_PACKAGE_ENABLED                [:analyzer :node-package-enabled]
   Settings$KEYS/ANALYZER_NPM_CPE_ENABLED                     [:analyzer :npm-cpe-enabled]
   Settings$KEYS/ANALYZER_NUGETCONF_ENABLED                   [:analyzer :nugetconf-enabled]
   Settings$KEYS/ANALYZER_NUSPEC_ENABLED                      [:analyzer :nuspec-enabled]
   Settings$KEYS/ANALYZER_OPENSSL_ENABLED                     [:analyzer :openssl-enabled]
   Settings$KEYS/ANALYZER_OSSINDEX_WARN_ONLY_ON_REMOTE_ERRORS [:analyzer :ossindex-warn-only-on-remote-errors]
   Settings$KEYS/ANALYZER_PIPFILE_ENABLED                     [:analyzer :pipfile-enabled]
   Settings$KEYS/ANALYZER_PIP_ENABLED                         [:analyzer :pip-enabled]
   Settings$KEYS/ANALYZER_PNPM_AUDIT_ENABLED                  [:analyzer :pnpm-package-enabled]
   Settings$KEYS/ANALYZER_PYTHON_DISTRIBUTION_ENABLED         [:analyzer :python-distribution-enabled]
   Settings$KEYS/ANALYZER_PYTHON_PACKAGE_ENABLED              [:analyzer :python-package-enabled]
   Settings$KEYS/ANALYZER_RETIREJS_ENABLED                    [:analyzer :retirejs-enabled]
   Settings$KEYS/ANALYZER_RUBY_GEMSPEC_ENABLED                [:analyzer :ruby-gemspec-enabled]
   Settings$KEYS/ANALYZER_SWIFT_PACKAGE_MANAGER_ENABLED       [:analyzer :swift-package-manager-enabled]
   Settings$KEYS/ANALYZER_SWIFT_PACKAGE_RESOLVED_ENABLED      [:analyzer :swift-package-resolved-enabled]
   Settings$KEYS/ANALYZER_YARN_AUDIT_ENABLED                  [:analyzer :yarn-audit-enabled]
   Settings$KEYS/AUTO_UPDATE                                  [:auto-update]})

(defn app-name [project]
  (let [name (get project :name "stdin")
        group (get project :group name)]
    (if (= group name)
      name
      (str group "/" name))))

(def default-settings
  {:exit-after-check true
   :delete-config?   true
   :verbose-summary  false
   :nvd              {:nvd-api {:delay 5000 ;; Value based on https://github.com/jeremylong/DependencyCheck/commit/be5c4a4f39d
                                :max-retry-count 10}
                      :analyzer {:assembly-enabled                    false
                                 :archive-enabled                     true
                                 :autoconf-enabled                    false
                                 :bundle-audit-enabled                false
                                 :central-enabled                     true
                                 :cmake-enabled                       false
                                 :cocoapods-enabled                   false
                                 :composer-lock-enabled               false
                                 :cpanfile-enabled                    false
                                 :experimental-enabled                false
                                 :golang-dep-enabled                  false
                                 :golang-mod-enabled                  false
                                 :jar-enabled                         true
                                 :mix-audit-enabled                   false
                                 :msbuild-project-enabled             false
                                 :nexus-enabled                       true
                                 :node-audit-enabled                  false
                                 :node-package-enabled                false
                                 :npm-cpe-enabled                     false
                                 :nugetconf-enabled                   false
                                 :nuspec-enabled                      false
                                 :openssl-enabled                     false
                                 :ossindex-warn-only-on-remote-errors false
                                 :pip-enabled                         false
                                 :pipfile-enabled                     false
                                 :pnpm-package-enabled                false
                                 :python-distribution-enabled         false
                                 :python-package-enabled              false
                                 :retirejs-enabled                    false
                                 :ruby-gemspec-enabled                false
                                 :swift-package-manager-enabled       false
                                 :swift-package-resolved-enabled      false
                                 :yarn-audit-enabled                  false}}})

(defn- deep-merge [a b]
  (merge-with (fn [x y]
                (cond (map? y) (deep-merge x y)
                      (vector? y) (concat x y)
                      :else y))
              a b))

(defn- parse-json-file [config-filename]
  (json/read-str (slurp config-filename) :key-fn keyword))

(def default-edn-config-filename "nvd-clojure.edn")

(def default-config-content (delay (slurp (io/resource "nvd_clojure/default_config_content.edn"))))

(def default-suppression-content (delay (slurp (io/resource "nvd_clojure/default_suppression_content.xml"))))

(defn maybe-create-edn-file! [config-filename]
  (when (and (= config-filename
                default-edn-config-filename)
             (let [^File file (io/file config-filename)]
               (or (not (-> file .exists))
                   (-> file slurp string/blank?))))
    (spit config-filename @default-config-content)))

(defn- parse-edn-file [config-filename]
  {:nvd            (edn/read-string (slurp config-filename))
   ;; This key was introduced for dealing for config expressed as .json files (which wasn't always a feature) without deleting them.
   ;; It remains as a concept (although hidden from .edn users) so that .json usage can keep working:
   :delete-config? false})

(defn maybe-create-suppression-file! [{suppression-filename :suppression-file}]
  (when-let [file (some-> suppression-filename io/file)]
    (when-not (-> file .exists)
      (some-> file .getParentFile .mkdirs)
      (spit suppression-filename @default-suppression-content))))

(defn populate-settings! [config-filename]
  (let [config (case (-> config-filename (string/split #"\.") last)
                 "json" (parse-json-file config-filename)
                 "edn"  (-> config-filename
                            (doto maybe-create-edn-file!)
                            parse-edn-file)
                 (throw (ex-info "Only .edn and .json config file extensions are supported.
You can pass an empty string for an .edn file to be automatically created."
                                 {:config-filename config-filename})))
        project (deep-merge default-settings config)
        nvd-settings (:nvd project)
        settings (Settings.)]

    (.info log/logger (str "User-provided config: " (pr-str config)))

    (maybe-create-suppression-file! nvd-settings)

    (doseq [[prop path] boolean-mappings]
      (.setBooleanIfNotNull settings prop (get-in nvd-settings path)))

    (doseq [[prop path] string-mappings]
      (.setStringIfNotEmpty settings prop (str (get-in nvd-settings path))))

    (when (= ::not-found (get-in nvd-settings [:nvd-api :key] ::not-found))
      (let [api-key (System/getenv "NVD_API_TOKEN")]

        (when (or (not api-key)
                  (string/blank? api-key))
          (throw (ex-info "No NVD API key supplied as config settings or env var." {})))

        (.setString settings Settings$KEYS/NVD_API_KEY api-key)))

    (.configure (Downloader/getInstance) settings)

    (-> project
        (assoc-in [:nvd :data-directory] (.getDataDirectory settings))
        (assoc :engine      (Engine. settings)
               :title       (str (app-name project) " " (:version project))
               :start-time  (System/currentTimeMillis)
               :config-file config-filename))))

(defn cleanup [project]
  (let [engine ^Engine (:engine project)
        settings (.getSettings engine)]
    (.close engine)
    (.cleanup settings true)
    (when (:delete-config? project)
      (.deleteOnExit (io/file (:config-file project))))))

(defmacro with-config
  {:style/indent 1}
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
