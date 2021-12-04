(ns occurrences
  (:require
   [secrets :refer [airtable-chores-api-key]]
   [oz.core :as oz]
   [clojure.data.json :as json]
   [clj-time.core :as time]
   [clj-time.format :as time-format]
   [clj-time.coerce :as time-coerce]
   [clj-time.predicates :as time-pred]
   [com.rpl.specter :as sp]
   [clj-http.client :as http]
   [semantic-csv.core :as sc]
   [clojure.string :as s]
   [tick.alpha.api :as t]))

(defn date-col->frequencies
  [c]
  (->> c
       (map t/date)
       (map t/year-month)
       (map str)
       frequencies))

(def daylio-data-a
  (->> "/home/justin/Nextcloud/projects/quantification-data/raw-data/daylio/daylio_export_2021_06_03.csv"
       (sc/slurp-csv)
       (filter #(or (s/includes? (:activities %) "")
                    (s/includes? (:activities %) "")
                    (s/includes? (:activities %) "")
                    ))
       (remove #(or (s/includes? (:activities %) "")
                    (s/includes? (:activities %) "")
                    ))
       (map :﻿full_date)
       date-col->frequencies))

(def daylio-data-b
  (->> "/home/justin/Nextcloud/projects/quantification-data/raw-data/daylio/daylio_export_2021_06_03.csv"
       (sc/slurp-csv)
       (filter #(or (s/includes? (:activities %) "")))
       (remove #(or (s/includes? (:activities %) "")))
       (map :﻿full_date)
       date-col->frequencies))

(def airtable-data-a
  (->> "/home/justin/Nextcloud/projects/quantification-data/raw-data/airtable/kpi/activity-log-2021-12-04.csv"
       (sc/slurp-csv)
       (filter #(and (some? (:day %)) (not= "#ERROR!" (:day %))))
       (filter #(s/includes? (:activity %) ""))
       (remove #(or (s/includes? (:activity %) "")
                    (s/includes? (:activity %) "")
                    ))
       (map :day)
       date-col->frequencies))

(def airtable-data-b
  (->> "/home/justin/Nextcloud/projects/quantification-data/raw-data/airtable/kpi/activity-log-2021-12-04.csv"
       (sc/slurp-csv)
       (filter #(and (some? (:day %)) (not= "#ERROR!" (:day %))))
       (filter #(s/includes? (:activity %) ""))
       (map :day)
       date-col->frequencies))

(def all-data (concat (->> (concat daylio-data-a airtable-data-a)
                           (map (fn [[k v]] {:date k :count v :category "a"}))
                           (sort-by :date))
                      (->> (concat daylio-data-b airtable-data-b)
                           (map (fn [[k v]] {:date k :count v :category "b"}))
                           (sort-by :date))))

(def occurrences-by-month {:data     {:values all-data}
                           :encoding {:x     {:field "date" :type "nominal"}
                                      :y     {:field "count" :type "quantitative"}
                                      :color {:field "category"}}
                           :mark     "bar"})

(oz/start-server!)

(oz/view! occurrences-by-month)
