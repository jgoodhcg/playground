(ns airtable-ideas-port
  (:require
   [semantic-csv.core :as sc]
   [clojure.string :as s]))

(->> (sc/slurp-csv "resources/ideas-Grid view.csv")
     (map (fn [{:keys [labela description type]}]
            (str "\n\n- " (s/trim-newline labela) " #[[" (s/trim-newline type)"]]" "\n"
                 (-> description
                     (s/split #"\n")
                     (->> (map #(str "  - " % "\n")))
                     (->> (s/join ""))))))
     (println))



