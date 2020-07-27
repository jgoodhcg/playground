(ns simple-workout-log
  (:require [semantic-csv.core :as sc]
            [com.rpl.specter :as sp]
            [oz.core :as oz]
            [cljc.java-time.local-date :as ld]))

(def file "/home/justin/Nextcloud/projects/quantification-data/raw-data/simple-workout-log/2020-07-11-strength.csv")

(def formatter (java.time.format.DateTimeFormatter/ofPattern "M/d/yy"))

(def start-date "2020-06-20")

(def days (.between java.time.temporal.ChronoUnit/DAYS (ld/parse start-date)
                    (ld/parse "2020-09-22")))

(def pre-data (->> file
                   (sc/slurp-csv)
                   (map #(clojure.set/rename-keys % {(keyword "# of Reps") :Reps}))
                   (map #(merge % {:date-java (-> % :Date (ld/parse formatter))}))
                   (group-by #(-> % :date-java (.toString)))
                   (sp/transform [sp/MAP-VALS]
                                 (fn [entries]
                                   {:total-reps (->> entries
                                                     (map :Reps)
                                                     (map #(Integer/parseInt %))
                                                     (reduce + 0))
                                    :date       (-> entries first :date-java (.toString))}))
                   (sp/select [sp/MAP-VALS])
                   (sort-by :date)
                   (filter #(>= (ld/compare-to
                                  (ld/parse (:date %))
                                  (ld/parse start-date))
                                0))))

(def data (->> days
               (range)
               (map #(ld/plus-days (ld/parse start-date) %))
               (map (fn [date]
                      (let [ytd-reps          (->> pre-data
                                                   (filter #(>= (ld/compare-to date (ld/parse (:date %)))
                                                                0))
                                                   (map :total-reps)
                                                   (reduce + 0))
                            maybe-workout-day (->> pre-data (some #(if (= (.toString date) (:date %))
                                                                     % nil)))]
                        (merge {} maybe-workout-day {:ytd-reps ytd-reps
                                                     :date     (.toString date)}))))))

(def today (-> (ld/now)
               (ld/format
                 (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd"))))

(oz/start-server!)

(def reps-plot
  {:data  {:values data}
   :width 1500
   :layer [{:mark     "line"
            :encoding {:x     {:field "date"
                               :type  "temporal"
                               :axis  {:format "%d %b"}}
                       :y     {:field "ytd-reps"
                               :type  "quantitative"}
                       :color {:condition
                               {:test }}}}]})

(def viz
  [:div
   [:h1 "2020 summer 10k reps"]
   [:div
    [:vega-lite reps-plot]]])

(oz/view! viz)
