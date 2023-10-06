(ns how-not-to-die-cookbook
  (:require [xtdb.api :as xt]
            [clojure.java.io :as io]
            ))

(defn start-xtdb! []
  (letfn [(kv-store [dir]
            {:kv-store {:xtdb/module 'xtdb.rocksdb/->kv-store
                        :db-dir (io/file dir)
                        :sync? true}})]
    (xt/start-node
     {:xtdb/tx-log (kv-store "/tmp/dev/tx-log")
      :xtdb/document-store (kv-store "/tmp/dev/doc-store")
      :xtdb/index-store (kv-store "/tmp/dev/index-store")})))

(def xtdb-node (start-xtdb!))
;; note that attempting to eval this expression more than once before first calling `stop-xtdb!` will throw a RocksDB locking error
;; this is because a node that depends on native libraries must be `.close`'d explicitly

(defn stop-xtdb! []
  (.close xtdb-node))

(def data [
           {:xt/id              :recipe.almond-milk
            :recipe/name        "Almond Milk"
            :recipe/ingredients [{:ingredient/id :ingredient.raw-almond-butter
                                  :amount        {:quantity 2 :unit :tablespoon}}
                                 {:ingredient/id :ingredient.water
                                  :amount        {:quantity 2 :unit :cup}}]}

           {:xt/id :ingredient.raw-almond-butter
            :ingredient/name "Raw Almond Butter"}

           {:xt/id :ingredient.water
            :ingredient/name "Water"}
           ])

(xt/submit-tx xtdb-node (->> data (mapv (fn [m] [::xt/put m]))))

(xt/q (xt/db xtdb-node) '{:find  [(pull ?entity [*])]
                          :where [[?entity :xt/id]]})

(xt/q (xt/db xtdb-node)
      '{:find  [(pull ?recipe [:recipe/name])]
        :where [[?recipe :recipe/ingredients ?ingredients]
                [(some? (some #(= ingredient-id (:ingredient/id %)) ?ingredients))]]
        :in    [ingredient-id]}
      :ingredient.raw-almond-butter)
