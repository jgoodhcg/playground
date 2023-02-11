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

(def planting-data
  {:artichoke       {:spring {:start      56
                              :sow        0
                              :transplant 0}
                     :fall   {:start      nil
                              :sow        0
                              :transplant nil}}
   :beans           {:spring {:start      nil
                              :sow        0
                              :transplant nil}
                     :fall   {:start      nil
                              :sow        nil
                              :transplant nil}} 
   :beets           {:spring {:start      nil
                              :sow        nil
                              :transplant nil}
                     :fall   {:start      nil
                              :sow        nil
                              :transplant nil}} 
   :broccoli        {:spring {:start      nil
                              :sow        nil
                              :transplant nil}
                     :fall   {:start      nil
                              :sow        nil
                              :transplant nil}} 
   :brussel-sprouts {:spring {:start      nil
                              :sow        nil
                              :transplant nil}
                     :fall   {:start      nil
                              :sow        nil
                              :transplant nil}}
   :cabbage         {:spring {:start      nil
                              :sow        nil
                              :transplant nil}
                     :fall   {:start      nil
                              :sow        nil
                              :transplant nil}}
   :carrots         {:spring {:start      nil
                              :sow        nil
                              :transplant nil}
                     :fall   {:start      nil
                              :sow        nil
                              :transplant nil}}
   :cauliflower     {:spring {:start      nil
                              :sow        nil
                              :transplant nil}
                     :fall   {:start      nil
                              :sow        nil
                              :transplant nil}}
   :celery          {:spring {:start      nil
                              :sow        nil
                              :transplant nil}
                     :fall   {:start      nil
                              :sow        nil
                              :transplant nil}}
   :chard           {:spring {:start      nil
                              :sow        nil
                              :transplant nil}
                     :fall   {:start      nil
                              :sow        nil
                              :transplant nil}}
   :collards        {:spring {:start      nil
                              :sow        nil
                              :transplant nil}
                     :fall   {:start      nil
                              :sow        nil
                              :transplant nil}}
   :corn            {:spring {:start      nil
                              :sow        nil
                              :transplant nil}
                     :fall   {:start      nil
                              :sow        nil
                              :transplant nil}}
   :cucumbers       {:spring {:start      nil
                              :sow        nil
                              :transplant nil}
                     :fall   {:start      nil
                              :sow        nil
                              :transplant nil}}
   :eggplant        {:spring {:start      nil
                              :sow        nil
                              :transplant nil}
                     :fall   {:start      nil
                              :sow        nil
                              :transplant nil}}
   :gourds          {:spring {:start      nil
                              :sow        nil
                              :transplant nil}
                     :fall   {:start      nil
                              :sow        nil
                              :transplant nil}}
   :kale            {:spring {:start      nil
                              :sow        nil
                              :transplant nil}
                     :fall   {:start      nil
                              :sow        nil
                              :transplant nil}}
   :kohlrabi        {:spring {:start      nil
                              :sow        nil
                              :transplant nil}
                     :fall   {:start      nil
                              :sow        nil
                              :transplant nil}}
   :leek            {:spring {:start      nil
                              :sow        nil
                              :transplant nil}
                     :fall   {:start      nil
                              :sow        nil
                              :transplant nil}}
   :lettuce         {:spring {:start      nil
                              :sow        nil
                              :transplant nil}
                     :fall   {:start      nil
                              :sow        nil
                              :transplant nil}}
   :melons          {:spring {:start      nil
                              :sow        nil
                              :transplant nil}
                     :fall   {:start      nil
                              :sow        nil
                              :transplant nil}}
   :okra            {:spring {:start      nil
                              :sow        nil
                              :transplant nil}
                     :fall   {:start      nil
                              :sow        nil
                              :transplant nil}}
   :onions          {:spring {:start      nil
                              :sow        nil
                              :transplant nil}
                     :fall   {:start      nil
                              :sow        nil
                              :transplant nil}}
   :peas            {:spring {:start      nil
                              :sow        nil
                              :transplant nil}
                     :fall   {:start      nil
                              :sow        nil
                              :transplant nil}}
   :peppers         {:spring {:start      nil
                              :sow        nil
                              :transplant nil}
                     :fall   {:start      nil
                              :sow        nil
                              :transplant nil}}
   :pumpkins        {:spring {:start      nil
                              :sow        nil
                              :transplant nil}
                     :fall   {:start      nil
                              :sow        nil
                              :transplant nil}}
   :radish          {:spring {:start      nil
                              :sow        nil
                              :transplant nil}
                     :fall   {:start      nil
                              :sow        nil
                              :transplant nil}}
   :spinach         {:spring {:start      nil
                              :sow        nil
                              :transplant nil}
                     :fall   {:start      nil
                              :sow        nil
                              :transplant nil}}
   :squash          {:spring {:start      nil
                              :sow        nil
                              :transplant nil}
                     :fall   {:start      nil
                              :sow        nil
                              :transplant nil}}
   :tomatoes        {:spring {:start      nil
                              :sow        nil
                              :transplant nil}
                     :fall   {:start      nil
                              :sow        nil
                              :transplant nil}}
   :turnips         {:spring {:start      nil
                              :sow        nil
                              :transplant nil}
                     :fall   {:start      nil
                              :sow        nil
                              :transplant nil}}
   :watermelons     {:spring {:start      nil
                              :sow        nil
                              :transplant nil}
                     :fall   {:start      nil
                              :sow        nil
                              :transplant nil}}})