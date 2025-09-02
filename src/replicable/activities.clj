(ns replicable.activities
  (:require [temporal.activity :refer [defactivity]]
            [clj-http.client :as http]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [next.jdbc :as jdbc]
            [honey.sql :as sql])
  (:import [java.time Instant]))

(def db-spec
  {:dbtype "postgresql"
   :dbname "app"
   :host "localhost"
   :port 5432
   :user "admin"
   :password "admin"})

(defn get-db-connection []
  (jdbc/get-datasource db-spec))

;; Define activity using defactivity macro with proper signature
(defactivity download-file-activity
  [ctx {:keys [url workflow-id] :as args}]
  (log/info "download-file-activity:" args)
  (let [filename (str "data/" (last (clojure.string/split url #"/")))
        _ (io/make-parents filename)
        ds (get-db-connection)]
    (try
      (log/info "Downloading" url "to" filename)
      
      ;; Record start in database
      (jdbc/execute! ds
                    (sql/format {:insert-into :data_downloads
                                :values [{:workflow_id workflow-id
                                         :url url
                                         :filename filename
                                         :status "downloading"
                                         :download_started_at (java.sql.Timestamp/from (Instant/now))}]}))
      
      ;; Download the file
      (let [response (http/get url {:as :stream})
            body (:body response)]
        (with-open [output (io/output-stream filename)]
          (io/copy body output))
        
        (let [file (io/file filename)
              file-size (.length file)]
          ;; Record completion
          (jdbc/execute! ds
                        (sql/format {:update :data_downloads
                                    :set {:status "completed"
                                         :file_size file-size
                                         :download_completed_at (java.sql.Timestamp/from (Instant/now))
                                         :updated_at (java.sql.Timestamp/from (Instant/now))}
                                    :where [:and
                                           [:= :workflow_id workflow-id]
                                           [:= :url url]]}))
          (log/info "Successfully downloaded" url "Size:" file-size "bytes")
          {:status "success"
           :url url
           :filename filename
           :size file-size}))
      
      (catch Exception e
        (let [error-msg (.getMessage e)]
          (log/error e "Failed to download" url)
          ;; Record error
          (jdbc/execute! ds
                        (sql/format {:update :data_downloads
                                    :set {:status "failed"
                                         :error_message error-msg
                                         :updated_at (java.sql.Timestamp/from (Instant/now))}
                                    :where [:and
                                           [:= :workflow_id workflow-id]
                                           [:= :url url]]}))
          (throw (ex-info "Download failed" {:url url :error error-msg})))))))