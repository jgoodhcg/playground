(ns simple-workout-log
  (:require [semantic-csv.core :as sc]
            [com.rpl.specter :as sp]
            [oz.core :as oz]
            [cljc.java-time.local-date :as ld]))

(def file "/home/justin/Nextcloud/projects/quantification-data/raw-data/simple-workout-log/2020-07-11-strength.csv")

(def formatter (java.time.format.DateTimeFormatter/ofPattern "M/d/yy"))

(def data (->> file
               (sc/slurp-csv)
               (map #(clojure.set/rename-keys
                       %
                       {(keyword "# of Reps") :Reps}))
               (map #(merge % {:date-java (-> % :Date (ld/parse formatter))}))))

(oz/start-server!)

(def reps-plot
  {:data  {:values data}
   :width 1500
   :layer [{:mark     "line"
            :encoding {:x {:field    "Date"
                           :type     "temporal"
                           :timeUnit "yearmonthdate"}
                       :y {:field "cumulative"
                           :type  "quantitative"}}}]})

(def viz
  [:div
   [:h1 "2020 summer 10k reps"]
   [:div
    [:vega-lite reps-plot]]])


(oz/view! viz)
