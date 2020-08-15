(ns simple-workout-log
  (:require [semantic-csv.core :as sc]
            [com.rpl.specter :as sp]
            [oz.core :as oz]
            [cljc.java-time.local-date :as ld]))

(def file "/home/justin/Nextcloud/projects/quantification-data/raw-data/simple-workout-log/2020-07-27-strength.csv")

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

(def today (-> (ld/now)
               (ld/format
                 (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd"))))

(def days-so-far (.between java.time.temporal.ChronoUnit/DAYS (ld/parse start-date)
                           (ld/parse today)))

(def data
  (let [total-total-reps (->> pre-data
                              (filter #(>= (ld/compare-to (ld/parse today)
                                                          (ld/parse (:date %)))
                                           0))
                              (map :total-reps)
                              (reduce + 0))
        average          (int (/ total-total-reps days-so-far))]

    (->> days
         (range)
         (map #(ld/plus-days (ld/parse start-date) %))
         (map (fn [date]
                (let [ytd-reps          (->> pre-data
                                             (filter #(>= (ld/compare-to date (ld/parse (:date %)))
                                                          0))
                                             (map :total-reps)
                                             (reduce + 0))
                      maybe-workout-day (->> pre-data (some #(if (= (.toString date) (:date %))
                                                               % nil)))
                      future            (ld/is-after date (ld/parse today))
                      days-past-today   (max 0 (.between java.time.temporal.ChronoUnit/DAYS
                                                         (ld/parse today)
                                                         date))]

                  (merge {} maybe-workout-day {:ytd-reps   ytd-reps
                                               :date       (.toString date)
                                               :future     future
                                               :projection (->> ytd-reps
                                                                (+ (->> average (* days-past-today))))})))))))

(oz/start-server!)

(def reps-plot
  {:data  {:values data}
   :width 900
   :layer [{:mark     "line"
            :encoding {:x          {:field "date"
                                    :type  "temporal"
                                    :axis  {:format "%d %b"}}
                       :y          {:field "projection"
                                    :type  "quantitative"
                                    :axis  {:title "reps"}}
                       :color      {:value "grey"}
                       :strokeDash {:field "future"
                                    :type  "nominal"}}}
           {:mark     "line"
            :encoding {:x          {:field "date"
                                    :type  "temporal"
                                    :axis  {:format "%d %b"}}
                       :y          {:field "ytd-reps"
                                    :type  "quantitative"
                                    :axis  {:title ""}}
                       :color      {:value "steelblue"}
                       :labels     "false"
                       :strokeDash {:field "future"
                                    :type  "nominal"}}}

           {:mark     "rule"
            :data     {:values [{:ref 10000}]}
            :encoding {:y     {:field "ref" :type "quantitative"}
                       :color {:value "black"}}}]})

(def viz
  [:div
   [:h1 "2020 summer 10k reps"]
   [:div
    [:vega-lite reps-plot]]])

(oz/view! viz)
