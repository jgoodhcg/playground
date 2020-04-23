(ns ynab
  (:require [semantic-csv.core :as sc]
            [com.rpl.specter :as sp]))

;; Playing around with optional recurring expenses
;; Software Subscriptions (renamed to "Subscriptions")
(comment
  (->> "/home/justin/Nextcloud/documents/financial/ynab/2020-03-23-register.csv"
       (sc/slurp-csv) ;; could not figure out how to get :cast-fns to work
       (filter #(= "Software Subscriptions" (:Category %)))
       (group-by :Payee)
       (sp/transform [sp/MAP-VALS] (fn [rows] (->> rows
                                                   (map (fn [row] (-> row
                                                                      (:Outflow)
                                                                      (subs 1)
                                                                      (Float/parseFloat))))
                                                   (reduce +))))
       (sc/spit-csv "./resources/software-subs.csv")))

;; Giving
(comment
  (->> "/home/justin/Nextcloud/documents/financial/ynab/2020-03-23-register.csv"
       (sc/slurp-csv) ;; could not figure out how to get :cast-fns to work
       (filter #(= "Giving" (:Category %)))
       (group-by :Payee)
       (sp/transform [sp/MAP-VALS] (fn [rows] (->> rows
                                                   (map (fn [row] (-> row
                                                                      (:Outflow)
                                                                      (subs 1)
                                                                      (Float/parseFloat))))
                                                   (reduce +))))
       (sc/spit-csv "./resources/giving.csv")))
