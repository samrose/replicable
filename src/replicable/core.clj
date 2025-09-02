(ns replicable.core
  (:require [migratus.core :as migratus]
            [replicable.worker :as worker]
            [replicable.client :as client]
            [clojure.tools.logging :as log])
  (:gen-class))

(def migration-config
  {:store :database
   :migration-dir "migrations"
   :db {:dbtype "postgresql"
        :dbname "app"
        :host "localhost"
        :port 5432
        :user "admin"
        :password "admin"}})

(defn run-migrations []
  (log/info "Running database migrations...")
  (migratus/migrate migration-config)
  (log/info "Migrations completed"))

(defn -main
  "Main entry point for the application"
  [& args]
  (let [command (first args)]
    (case command
      "migrate"
      (do
        (log/info "Running migrations...")
        (run-migrations)
        (log/info "Migrations complete"))
      
      "worker"
      (do
        (log/info "Starting Temporal worker...")
        (run-migrations)
        (worker/create-worker)
        (log/info "Worker started. Press Ctrl+C to stop.")
        ;; Keep the worker running
        (Thread/sleep Long/MAX_VALUE))
      
      "example"
      (do
        (log/info "Running example workflow...")
        (run-migrations)
        ;; Start worker in background thread
        (future (worker/create-worker))
        (Thread/sleep 2000) ;; Give worker time to start
        ;; Run example workflow
        (client/run-example-workflow)
        (System/exit 0))
      
      "simple"
      (do
        (log/info "Running simple example without Temporal...")
        (run-migrations)
        (require 'replicable.simple-example)
        ((resolve 'replicable.simple-example/run-downloads))
        (System/exit 0))
      
      ;; Default
      (do
        (println "Usage: lein run [command]")
        (println "Commands:")
        (println "  migrate - Run database migrations")
        (println "  worker  - Start the Temporal worker")
        (println "  example - Run an example workflow (with Temporal)")
        (println "  simple  - Run simple download example (without Temporal)")
        (System/exit 1)))))