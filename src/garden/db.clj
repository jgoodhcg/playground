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
   (ds/req :garden/type) keyword?
   :user/name            string?})

(def user-spec
  (ds/spec {:spec         user-ds
            :name         ::user
            :keys-default ds/opt}))

(s/valid? user-spec me) 

(def seed-catalog-item-ds
  {(ds/req :xt/id)                                         uuid?
   (ds/req :garden/type)                                   keyword?
   :seed-catalog-item/name                                 string?
   :seed-catalog-item/indoor-start-last-frost-offset-early t/period?
   :seed-catalog-item/indoor-start-last-frost-offset-late  t/period?
   :seed-catalog-item/latin-name                           string?})

(def seed-catalog-item-spec
  (ds/spec {:spec         seed-catalog-item-ds
            :name         ::seed-catalog-item
            :keys-default ds/opt}))

(comment
  (xt/submit-tx node [[::xt/put me]])

  (-> node (xt/db) (xt/q '{:find [(pull ?user [*])]
                           :where [[?user :garden/type :garden/user]]}))

  (-> node (xt/db) (xt/entity-history #uuid "1e4a3cba-b5f2-4587-a200-638457f8dc21"
                                      :desc {:with-docs? true}))
  )

;; - numbers are days to add to the frost date for season
;; - negative numbers will result in a date _before_ the frost date
;; - positive numbers will result in a date _after_ the frost date
;; - compiled from spring and fall starting spreadsheets here:
;; - https://www.ufseeds.com/crop-calculators.html
(def planting-data
  {:artichoke       {:spring {:start      -56
                              :sow        0
                              :transplant 0}
                     :fall   {:start      nil
                              :sow        nil
                              :transplant nil}}
   :beans           {:spring {:start      -56
                              :sow        0
                              :transplant nil}
                     :fall   {:start      nil
                              :sow        nil
                              :transplant nil}}
   :beets           {:spring {:start      -56
                              :sow        -14
                              :transplant -14}
                     :fall   {:start      nil
                              :sow        -70
                              :transplant nil}}
   :broccoli        {:spring {:start      -56
                              :sow        -14
                              :transplant -14}
                     :fall   {:start      -110
                              :sow        nil
                              :transplant -80}}
   :brussel-sprouts {:spring {:start      -56
                              :sow        -14
                              :transplant -14}
                     :fall   {:start      -150
                              :sow        nil
                              :transplant -120}}
   :cabbage         {:spring {:start      -70
                              :sow        -28
                              :transplant -28}
                     :fall   {:start      -118
                              :sow        nil
                              :transplant -90}}
   :carrots         {:spring {:start      nil
                              :sow        -21
                              :transplant nil}
                     :fall   {:start      nil
                              :sow        -96
                              :transplant nil}}
   :cauliflower     {:spring {:start      -56
                              :sow        -14
                              :transplant -14}
                     :fall   {:start      -110
                              :sow        nil
                              :transplant -80}}
   :celery          {:spring {:start      -77
                              :sow        7
                              :transplant 7}
                     :fall   {:start      -140
                              :sow        nil
                              :transplant -112}}
   :chard           {:spring {:start      -56
                              :sow        -14
                              :transplant -14}
                     :fall   {:start      nil
                              :sow        -65
                              :transplant nil}}
   :collards        {:spring {:start      -63
                              :sow        -28
                              :transplant -28}
                     :fall   {:start      -117
                              :sow        nil
                              :transplant -87}}
   :corn            {:spring {:start      nil
                              :sow        7
                              :transplant nil}
                     :fall   {:start      nil
                              :sow        nil
                              :transplant nil}}
   :cucumbers       {:spring {:start      nil
                              :sow        0
                              :transplant nil}
                     :fall   {:start      nil
                              :sow        nil
                              :transplant nil}}
   :eggplant        {:spring {:start      -42
                              :sow        14
                              :transplant 14}
                     :fall   {:start      nil
                              :sow        nil
                              :transplant nil}}
   :gourds          {:spring {:start      nil
                              :sow        14
                              :transplant nil}
                     :fall   {:start      nil
                              :sow        nil
                              :transplant nil}}
   :kale            {:spring {:start      -70
                              :sow        -28
                              :transplant -28}
                     :fall   {:start      nil
                              :sow        -85
                              :transplant nil}}
   :kohlrabi        {:spring {:start      -70
                              :sow        -28
                              :transplant -28}
                     :fall   {:start      nil
                              :sow        -60
                              :transplant nil}}
   :leek            {:spring {:start      -84
                              :sow        -14
                              :transplant -14}
                     :fall   {:start      -145
                              :sow        nil
                              :transplant -110}}
   :lettuce         {:spring {:start      -82
                              :sow        -28
                              :transplant -28}
                     :fall   {:start      nil
                              :sow        -70
                              :transplant nil}}
   :melons          {:spring {:start      nil
                              :sow        14
                              :transplant nil}
                     :fall   {:start      nil
                              :sow        nil
                              :transplant nil}}
   :okra            {:spring {:start      -42
                              :sow        14
                              :transplant 14}
                     :fall   {:start      nil
                              :sow        nil
                              :transplant nil}}
   :onions          {:spring {:start      -98
                              :sow        -28
                              :transplant -28}
                     :fall   {:start      nil
                              :sow        -80
                              :transplant nil}}
   :peas            {:spring {:start      nil
                              :sow        -56
                              :transplant nil}
                     :fall   {:start      nil
                              :sow        -103
                              :transplant nil}}
   :peppers         {:spring {:start      -56
                              :sow        7
                              :transplant 7}
                     :fall   {:start      nil
                              :sow        nil
                              :transplant nil}}
   :pumpkins        {:spring {:start      nil
                              :sow        14
                              :transplant nil}
                     :fall   {:start      nil
                              :sow        nil
                              :transplant nil}}
   :radish          {:spring {:start      nil
                              :sow        -35
                              :transplant nil}
                     :fall   {:start      nil
                              :sow        -40
                              :transplant nil}}
   :spinach         {:spring {:start      -77
                              :sow        -42
                              :transplant -42}
                     :fall   {:start      nil
                              :sow        -57
                              :transplant nil}}
   :squash          {:spring {:start      -14
                              :sow        14
                              :transplant 14}
                     :fall   {:start      nil
                              :sow        nil
                              :transplant nil}}
   :tomatoes        {:spring {:start      -49
                              :sow        7
                              :transplant 7}
                     :fall   {:start      nil
                              :sow        nil
                              :transplant nil}}
   :turnips         {:spring {:start      nil
                              :sow        -35
                              :transplant nil}
                     :fall   {:start      nil
                              :sow        -60
                              :transplant nil}}
   :watermelons     {:spring {:start      -14
                              :sow        14
                              :transplant nil}
                     :fall   {:start      nil
                              :sow        nil
                              :transplant nil}}
   :fennel          {:spring {:start      nil
                              :sow        nil
                              :transplant nil}
                     :fall   {:start      nil
                              :sow        -107
                              :transplant nil}}
   :greens          {:spring {:start      nil
                              :sow        nil
                              :transplant nil}
                     :fall   {:start      nil
                              :sow        -68
                              :transplant nil}}
   })