(ns replicable.workflows
  (:require [temporal.workflow :refer [defworkflow]]
            [temporal.activity :as a]
            [replicable.activities :as activities]
            [clojure.tools.logging :as log]))

;; Define the workflow using defworkflow macro
(defworkflow download-data-workflow
  [{:keys [urls workflow-id] :as args}]
  (log/info "download-data-workflow:" args)
  (log/info "Processing" (count urls) "URLs")
  
  ;; Process each URL sequentially
  (let [results (atom [])]
    (doseq [url urls]
      (try
        (log/info "Processing URL:" url)
        ;; Invoke the activity and wait for result
        (let [result @(a/invoke activities/download-file-activity 
                               {:url url :workflow-id workflow-id})]
          (swap! results conj result))
        (catch Exception e
          (log/error e "Failed to process URL:" url)
          (swap! results conj {:url url :status "failed" :error (.getMessage e)}))))
    
    ;; Return workflow result
    {:status "completed"
     :urls-processed (count urls)
     :workflow-id workflow-id
     :results @results
     :timestamp (str (java.time.Instant/now))}))