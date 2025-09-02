(ns replicable.client
  (:require [temporal.client.core :as tc]
            [temporal.workflow :as tw]
            [replicable.workflows :as workflows]
            [clojure.tools.logging :as log]
            [clojure.pprint :as pp]))

(def task-queue "data-download-queue")

(defn start-workflow
  "Starts a data download workflow with the given URLs"
  [urls]
  (let [client (tc/create-client {:target "localhost:7233"})
        workflow-id (str "download-" (System/currentTimeMillis))
        ;; First create workflow stub, then start it
        workflow (tc/create-workflow client
                                     workflows/download-data-workflow
                                     {:task-queue task-queue
                                      :workflow-id workflow-id})
        ;; Use tc/start instead of tc/start-workflow
        _ (tc/start workflow {:urls urls :workflow-id workflow-id})]
    
    (log/info "Started workflow with ID:" workflow-id)
    {:workflow-id workflow-id
     :status "started"
     :workflow workflow}))

(defn run-example-workflow
  "Runs an example workflow that downloads sample data files"
  []
  (let [;; Example URLs - using public sample data files
        sample-urls ["https://raw.githubusercontent.com/datasets/covid-19/main/data/countries-aggregated.csv"
                     "https://raw.githubusercontent.com/datasets/gdp/master/data/gdp.csv"
                     "https://raw.githubusercontent.com/datasets/population/master/data/population.csv"]]
    
    (log/info "Starting example workflow with" (count sample-urls) "URLs")
    (let [result (start-workflow sample-urls)]
      (log/info "Workflow started successfully:")
      (pp/pprint (dissoc result :workflow)) ; Don't print the workflow object
      
      ;; Wait for workflow to complete and get result
      (log/info "Waiting for workflow to complete...")
      (try
        (let [workflow-result @(tc/get-result (:workflow result))]
          (log/info "Workflow completed with result:" workflow-result)
          (assoc result :result workflow-result))
        (catch Exception e
          (log/error e "Error waiting for workflow result")
          (assoc result :error (.getMessage e)))))))