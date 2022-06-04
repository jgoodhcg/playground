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
                                 :host     "db.zsmxqcdaontyokyayscg.supabase.co"
                                 :dbname   "postgres"
                                 :user     "postgres"
                                 :password "7b2k#Q9m6A^" ;; TODO remove secret
                                 ;; ...
                                 }}
     ;; :poll-sleep-duration (Duration/ofSeconds 1)
     }
    :xtdb/document-store {:xtdb/module     'xtdb.jdbc/->document-store
                          :connection-pool {:dialect {:xtdb/module 'xtdb.jdbc.psql/->dialect}
                                            ;; :pool-opts { ... }
                                            :db-spec {;; :jdbcUrl  "..."
                                                      ;; OR
                                                      :host     "db.zsmxqcdaontyokyayscg.supabase.co"
                                                      :dbname   "postgres"
                                                      :user     "postgres"
                                                      :password "7b2k#Q9m6A^" ;; TODO remove secret
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
