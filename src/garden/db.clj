(ns garden.db
  (:require [xtdb.api :as xt]
            [xtdb.jdbc]
            [clojure.spec.alpha :as s]
            [spec-tools.data-spec :as ds]
            [tick.core :as t]
            [tick.alpha.interval :as t.i]
            ))

(def jdbc-url "jdbc:sqlite:/home/justin/projects/playground/resources/garden.db")

(def node
  (xt/start-node
   {:xtdb/tx-log
    {:xtdb/module     'xtdb.jdbc/->tx-log
     :connection-pool {:dialect {:xtdb/module 'xtdb.jdbc.sqlite/->dialect}
                       :db-spec {:jdbcUrl jdbc-url}}}
    :xtdb/document-store {:xtdb/module     'xtdb.jdbc/->document-store
                          :connection-pool {:dialect {:xtdb/module 'xtdb.jdbc.sqlite/->dialect}
                                            :db-spec {:jdbcUrl jdbc-url}}}}))

#_(java.util.UUID/randomUUID);; => #uuid "1e4a3cba-b5f2-4587-a200-638457f8dc21"

(def me
  {:xt/id       #uuid "1e4a3cba-b5f2-4587-a200-638457f8dc21"
   :garden/type :garden/user
   :user/name   "Justin"})

(def user-ds
  {(ds/req :xt/id)       uuid?
   (ds/req :garden/type) :garden/user
   :user/name            string?})

(def user-spec
  (ds/spec {:spec         user-ds
            :name         ::workouts
            :keys-default ds/opt}))

(def seed-catalog-item-ds
  {(ds/req :xt/id)                                         uuid?
   (ds/req :garden/type)                                   :keyword
   :seed-catalog-item/name                                 string?
   :seed-catalog-item/indoor-start-last-frost-offset-early t/period?
   :seed-catalog-item/indoor-start-last-frost-offset-late  t/period?
   :seed-catalog-item/latin-name                           string?})

(def seed-catalog-item-spec
  (ds/spec {:spec         user-ds
            :name         ::workouts
            :keys-default ds/opt}))

(comment
  (xt/submit-tx node [[::xt/put me]])

  (-> node (xt/db) (xt/q '{:find [(pull ?user [*])]
                           :where [[?user :garden/type :garden/user]]}))

  (-> node (xt/db) (xt/entity-history #uuid "1e4a3cba-b5f2-4587-a200-638457f8dc21"
                                      :desc {:with-docs? true}))
  )
