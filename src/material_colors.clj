(ns material-colors
  (:require
   [clj-http.client :as client]
   [clojure-csv.core :as csv]
   [semantic-csv.core :as sc]
   [camel-snake-kebab.core :as csk]
   [com.rpl.specter :as sp]))

;; file with each csv line:
;; #ff00ff,Color Name
(-> "https://raw.githubusercontent.com/someuser/material-colors.csv"
    client/get
    :body
    csv/parse-csv
    (conj ["color" "label"]) ;; add headers for sc
    sc/mappify
    (->> (sp/transform [sp/ALL :label] csk/->kebab-case-keyword))
    (->> (sp/transform [sp/ALL] (fn [{:keys [color label]}]
                                  {label color})))
    (->> (apply merge))
    (->> (into (sorted-map)))
    (->> (spit "colors.cljs")))
