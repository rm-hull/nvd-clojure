(ns nvd.task.check
  (:require
   [clansi :refer [style]]
   [clojure.java.io :as io]
   [clojure.string :as s]
   [nvd.config :refer [default-edn-config-filename with-config]]
   [nvd.report :refer [fail-build? generate-report print-summary]]
   [trptcolin.versioneer.core :refer [get-version]])
  (:import
   (java.io File)
   (org.owasp.dependencycheck Engine)
   (org.owasp.dependencycheck.exception ExceptionCollection)))

(def version
  (delay {:nvd-clojure (get-version "nvd-clojure" "nvd-clojure")
          :dependency-check (.getImplementationVersion (.getPackage Engine))}))

(defn jar? [^String filename]
  (.endsWith filename ".jar"))

(defn absolute-path ^String [file]
  (s/replace-first file #"^~" (System/getProperty "user.home")))

(defn- scan-and-analyze [project]
  (let [^Engine engine (:engine project)]
    (doseq [p (:classpath project)]
      (when (jar? p)
        (.scan engine (absolute-path p))))
    (try
      (.analyzeDependencies engine)
      (catch ExceptionCollection e
        (println "Encountered errors while analyzing:" (.getMessage e))
        (doseq [exc (.getExceptions e)]
          (println exc))
        (let [exception-info (ex-info (str `ExceptionCollection)
                                      {:exceptions (.getExceptions e)})]
          (throw exception-info))))
    project))

(defn conditional-exit [{:keys [exit-after-check failed?]
                         {:keys [throw-if-check-unsuccessful?]} :nvd
                         :as project}]
  (cond
    (and failed? throw-if-check-unsuccessful?)
    (throw (ex-info "nvd-clojure failed / found vulnerabilities" {}))

    exit-after-check
    (System/exit (if failed? -1 0))

    :else project))

(defn jvm-version []
  (as-> (System/getProperty "java.version") $
    (s/split $ #"\.")
    (take 2 $)
    (s/join "." $)
    (Double/parseDouble $)))

(defn impl [config-filename classpath]
  (with-config [project config-filename]
    (println "Checking dependencies for" (-> project
                                             :title
                                             (s/trim)
                                             (str "...")
                                             (style :bright :yellow)))
    (println "  using nvd-clojure:" (:nvd-clojure @version) "and dependency-check:" (:dependency-check @version))
    (-> project
        (assoc :classpath classpath)
        scan-and-analyze
        generate-report
        print-summary
        fail-build?
        conditional-exit)))

(defn -main [& [config-filename ^String classpath-string]]
  (when (s/blank? classpath-string)
    (throw (ex-info "nvd-clojure requires a classpath value to be explicitly passed as a CLI argument.
Older usages are deprecated." {})))

  (let [classpath (s/split classpath-string #":")
        classpath (into []
                        (remove (fn [^String s]
                                  ;; Only .jar (and perhaps .zip) files are relevant.
                                  ;; source paths such as `src`, while are part of the classpath,
                                  ;; won't be meaningfully analyzed by dependency-check-core.
                                  ;; Keeping only .jars facilitates various usage patterns.
                                  (let [file (io/file s)]
                                    (or (.isDirectory file)
                                        (not (.exists file))))))
                        classpath)]

    (when-not (System/getProperty "nvd-clojure.internal.skip-self-check")
      (when-let [bad-entry (->> classpath
                                (some (fn [^String entry]
                                        (and (-> entry (.endsWith ".jar"))
                                             (when (or (-> entry (.contains "dependency-check-core"))
                                                       (-> entry (.contains "nvd-clojure")))
                                               entry)))))]
        (throw (ex-info "nvd-clojure should not analyse itself. This typically indicates a badly setup integration.

Please refer to the project's README for recommended usages."
                        {:bad-entry bad-entry
                         :classpath classpath-string}))))

    ;; perform some sanity checks for ensuring the calculated classpath has the expected format:
    (let [f (-> classpath ^String (first) File.)]
      (when-not (.exists f)
        (throw (ex-info (str "The classpath variable should be a vector of simple strings denoting existing files: "
                             (pr-str f))
                        {}))))

    (let [f (-> classpath ^String (last) File.)]
      (when-not (.exists f)
        (throw (ex-info (str "The classpath variable should be a vector of simple strings denoting existing files: "
                             (pr-str f))
                        {}))))

    ;; specifically handle blank strings (in addition to nil)
    ;; so that CLI callers can skip the first argument by simply passing an empty string:
    (let [config-filename (if-not (s/blank? config-filename)
                            config-filename
                            default-edn-config-filename)]
      (impl config-filename classpath))))
