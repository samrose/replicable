(ns replicable.worker
  (:require [temporal.client.core :as tc]
            [temporal.client.worker :as tw]
            [replicable.workflows]  ; Load the namespace with defworkflow
            [replicable.activities] ; Load the namespace with defactivity
            [clojure.tools.logging :as log]))

(def task-queue "data-download-queue")

(defn create-worker
  "Creates and starts a Temporal worker"
  []
  ;; Use production client connecting to local Temporal server
  (let [client (tc/create-client {:target "localhost:7233"})
        worker-options {:task-queue task-queue
                       :ctx {:client client}}
        worker (tw/start client worker-options)]
    (log/info "Starting Temporal worker on task queue:" task-queue)
    {:client client
     :worker worker}))

(defn create-production-worker
  "Creates and starts a Temporal worker for production"
  []
  (let [client (tc/create-client {:target "localhost:7233"})
        worker-options {:task-queue task-queue
                       :ctx {:client client}}
        worker (tw/start client worker-options)]
    (log/info "Starting Temporal production worker on task queue:" task-queue)
    {:client client
     :worker worker}))

(defn shutdown-worker [worker-context]
  (log/info "Shutting down Temporal worker")
  (when-let [w (:worker worker-context)]
    (tw/stop w)))