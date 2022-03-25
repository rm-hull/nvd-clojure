(ns nvd.log
  "Uses the same logging pattern `dependency-check-core` does,
  keeping the dependency tree simple."
  (:import
   (org.slf4j LoggerFactory)
   (org.slf4j.simple SimpleLogger)))

(defrecord nvd-clojure [])

(def ^SimpleLogger logger (LoggerFactory/getLogger nvd-clojure))
