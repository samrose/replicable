(ns replicable.simple-example
  (:require [clojure.tools.logging :as log]
            [clj-http.client :as http]
            [clojure.java.io :as io]
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

(defn download-file [{:keys [url workflow-id]}]
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
          {:status "failed"
           :url url
           :error error-msg})))))

;; Simple standalone example without Temporal SDK
(defn run-downloads
  "Directly run downloads without Temporal workflow"
  []
  (let [workflow-id (str "manual-" (System/currentTimeMillis))
        urls ["https://raw.githubusercontent.com/datasets/covid-19/main/data/countries-aggregated.csv"
              "https://raw.githubusercontent.com/datasets/gdp/master/data/gdp.csv"
              "https://raw.githubusercontent.com/datasets/population/master/data/population.csv"]]
    
    (log/info "Starting manual downloads for" (count urls) "files")
    
    (doseq [url urls]
      (try
        (log/info "Downloading:" url)
        ;; Call the download function directly
        (let [result (download-file {:url url :workflow-id workflow-id})]
          (if (= "success" (:status result))
            (log/info "Downloaded successfully:" (:filename result) "Size:" (:size result))
            (log/warn "Download failed:" url (:error result))))
        (catch Exception e
          (log/error e "Failed to download:" url))))
    
    (log/info "All downloads completed")
    {:workflow-id workflow-id
     :urls-processed (count urls)
     :status "completed"}))