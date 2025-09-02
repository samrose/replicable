(defproject replicable "0.1.0-SNAPSHOT"
  :description "A framework for replicable data workflows with Temporal"
  :url "https://github.com/samrose/replicable"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/tools.logging "1.2.4"]
                 [ch.qos.logback/logback-classic "1.4.11"]
                 
                 ;; Temporal SDK
                 [io.github.manetu/temporal-sdk "1.3.2"]
                 
                 ;; Database
                 [org.postgresql/postgresql "42.7.3"]
                 [com.github.seancorfield/next.jdbc "1.3.909"]
                 [com.github.seancorfield/honeysql "2.5.1103"]
                 [migratus "1.5.4"]
                 
                 ;; HTTP client for downloading data
                 [clj-http "3.12.3"]
                 
                 ;; JSON handling
                 [cheshire "5.12.0"]
                 
                 ;; Configuration
                 [environ "1.2.0"]]
  
  :main ^:skip-aot replicable.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}
             :dev {:dependencies [[migratus-lein "0.7.3"]]
                   :plugins [[migratus-lein "0.7.3"]]}}
  
  :migratus {:store :database
             :migration-dir "migrations"
             :db {:dbtype "postgresql"
                  :dbname "app"
                  :host "localhost"
                  :port 5432
                  :user "admin"
                  :password "admin"}})