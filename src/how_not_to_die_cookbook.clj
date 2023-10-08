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

           {:xt/id           :ingredient.raw-almond-butter
            :ingredient/name "Raw Almond Butter"}

           {:xt/id           :ingredient.water
            :ingredient/name "Water"}

           {:xt/id              :recipe.date-syrup
            :recipe/name        "Date Syrup"
            :recipe/ingredients [{:ingredient/id :ingredient.pitted-dates
                                  :amount        {:quantity 1 :unit :cup}}
                                 {:ingredient/id :ingredient.boiling-water
                                  :amount        {:quantity 1 :unit :cup}}
                                 {:ingredient/id :ingredient.blended-peeled-lemon
                                  :amount        {:quantity 1 :unit :teaspoon}}]}

           {:xt/id              :recipe.savory-spice-blend
            :recipe/name        "Savory Spice Blend"
            :recipe/ingredients [{:ingredient/id :ingredient.nutritional-yeast
                                  :amount        {:quantity 2 :unit :tablespoon}}
                                 {:ingredient/id :ingredient.onion-powder
                                  :amount        {:quantity 1 :unit :tablespoon}}
                                 {:ingredient/id :ingredient.dried-parsley
                                  :amount        {:quantity 1 :unit :tablespoon}}
                                 {:ingredient/id :ingredient.dried-basil
                                  :amount        {:quantity 1 :unit :tablespoon}}
                                 {:ingredient/id :ingredient.dried-thyme
                                  :amount        {:quantity 2 :unit :teaspoon}}
                                 {:ingredient/id :ingredient.garlic-powder
                                  :amount        {:quantity 2 :unit :teaspoon}}
                                 {:ingredient/id :ingredient.dry-mustard
                                  :amount        {:quantity 2 :unit :teaspoon}}
                                 {:ingredient/id :ingredient.paprika
                                  :amount        {:quantity 2 :unit :teaspoon}}
                                 {:ingredient/id :ingredient.ground-turmeric
                                  :amount        {:quantity 0.5 :unit :teaspoon}}
                                 {:ingredient/id :ingredient.celery-seeds
                                  :amount        {:quantity 0.5 :unit :teaspoon}}]}

           {:xt/id              :recipe.nutty-parm
            :recipe/name        "Nutty Parm"
            :recipe/ingredients [{:ingredient/id :ingredient.almonds
                                  :amount        {:quantity 0.5 :unit :cup}}
                                 {:ingredient/id :ingredient.brazil-nuts
                                  :amount        {:quantity 0.5 :unit :cup}}
                                 {:ingredient/id :ingredient.nutritional-yeast
                                  :amount        {:quantity 0.5 :unit :cup}}
                                 {:ingredient/id :recipe.savory-spice-blend
                                  :amount        {:quantity 2 :unit :teaspoon}}]}

           {:xt/id              :recipe.umami-sauce
            :recipe/name        "Umami Sauce"
            :recipe/ingredients [{:ingredient/id :recipe.vegetable-broth
                                  :amount        {:quantity 1 :unit :cup}}
                                 {:ingredient/id :ingredient.minced-garlic
                                  :amount        {:quantity 1 :unit :teaspoon}}
                                 {:ingredient/id :ingredient.grated-fresh-ginger
                                  :amount        {:quantity 1 :unit :teaspoon}}
                                 {:ingredient/id :ingredient.blackstrap-molasses
                                  :amount        {:quantity 1 :unit :tablespoon}}
                                 {:ingredient/id :recipe.date-syrup
                                  :amount        {:quantity 1.5 :unit :teaspoon}}
                                 {:ingredient/id :ingredient.tomato-paste
                                  :amount        {:quantity 0.5 :unit :teaspoon}}
                                 {:ingredient/id :ingredient.ground-black-pepper
                                  :amount        {:quantity 0.5 :unit :teaspoon}}
                                 {:ingredient/id :ingredient.white-miso-paste-blended
                                  :amount        {:quantity 1.5 :unit :teaspoon}}
                                 {:ingredient/id :ingredient.water
                                  :amount        {:quantity 2 :unit :tablespoon}}
                                 {:ingredient/id :recipe.blended-peeled-lemon
                                  :amount        {:quantity 2 :unit :teaspoon}}
                                 {:ingredient/id :ingredient.rice-vinegar
                                  :amount        {:quantity 1 :unit :tablespoon}}]}

           {:xt/id              :recipe.roasted-garlic
            :recipe/name        "Roasted Garlic"
            :recipe/ingredients [{:ingredients/id :ingredient.garlic
                                  :amount         {:quantity 1 :unit :head}}]}
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
