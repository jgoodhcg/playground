(ns xtdb-sandbox
  (:require [xtdb.api :as xt]
            [xtdb.jdbc]
            ))

(def node
  (xt/start-node
   {:xtdb/tx-log
    ;; TODO share a connection pool
    {:xtdb/module     'xtdb.jdbc/->tx-log
     :connection-pool {:dialect {:xtdb/module 'xtdb.jdbc.psql/->dialect}
                       ;; :pool-opts { ... }
                       :db-spec {;; :jdbcUrl  "..."
                                 ;; OR
                                 :host     (System/getenv "XTDB_PG_HOST")
                                 :dbname   "postgres"
                                 :user     "postgres"
                                 :password (System/getenv "XTDB_PG_PASSWORD")
                                 ;; ...
                                 }}
     ;; :poll-sleep-duration (Duration/ofSeconds 1)
     }
    :xtdb/document-store {:xtdb/module     'xtdb.jdbc/->document-store
                          :connection-pool {:dialect {:xtdb/module 'xtdb.jdbc.psql/->dialect}
                                            ;; :pool-opts { ... }
                                            :db-spec {;; :jdbcUrl  "..."
                                                      ;; OR
                                                      :host     (System/getenv "XTDB_PG_HOST")
                                                      :dbname   "postgres"
                                                      :user     "postgres"
                                                      :password (System/getenv "XTDB_PG_PASSWORD")
                                                      ;; ...
                                                      }}}
    }
   ))

(def manifest
  {:xt/id  :manifest
   :pilot-name  "Johanna"
   :id/rocket   "SB002-sol"
   :id/employee "22910x2"
   :badges      "SETUP"
   :cargo       ["stereo" "gold fish" "slippers" "secret note"]})

(xt/submit-tx node [[::xt/put manifest]])
