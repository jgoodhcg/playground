(ns chorechart
  (:require [oz.core :as oz]
            [clojure.data.json :as json]
            [clj-time.core :as time]
            [clj-time.format :as time-format]
            [clj-time.coerce :as time-coerce]
            [clj-time.predicates :as time-pred]
            [com.rpl.specter :as sp]
            [clj-http.client :as http]))

(oz/start-server!)

(defn play-data [& names]
  (for [n names
        i (range 20)]
    {:time i :item n :quantity (+ (Math/pow (* i (count n)) 0.8) (rand-int (count n)))}))

(def line-plot
  {:data {:values (play-data "monkey" "slipper" "broom")}
   :encoding {:x {:field "time" :type "quantitative"}
              :y {:field "quantity" :type "quantitative"}
              :color {:field "item" :type "nominal"}}
   :mark "line"})

;; Render the plot
(oz/view! line-plot)

(defn get-chores! [db-id key]
  (loop [response (-> (http/get
                       (str
                        "https://api.airtable.com/v0/"
                        db-id
                        "/Chores")
                       {:headers {:authorization (str "Bearer " key)}})
                      (:body)
                      (json/read-str :key-fn keyword))
         chores    []]
    (let [offset (->> response :offset)]
      (if (nil? offset)
        (concat chores (->> response :records))
        (recur
         (-> (http/get
              (str
               "https://api.airtable.com/v0/"
               db-id
               "/Chores?offset="offset)
              {:headers {:authorization (str "Bearer " key)}})
             (:body)
             (json/read-str :key-fn keyword))
         (concat chores (->> response :records)))))))

(def chores-raw (get-chores! "app0ASuEp9abRqV2v" "keyuu0VR2QuX2RMJf"))

(def chores-by-day-data
  (->> chores-raw
       (sp/transform [sp/ALL] (fn [{:keys [id fields]}]
                                {:id     id
                                 :date   (:Date fields)
                                 :chore  (:Chore fields)
                                 :person (:Person fields)}))
       (group-by :date)
       (map (fn [[date chores]]
              [{:date   date
                :count  (->> chores
                             (filter #(= "Justin" (:person %)))
                             (count))
                :person "Justin"}
               {:date   date
                :count  (->> chores
                             (filter #(= "Kaiti" (:person %)))
                             (count))
                :person "Kaiti"}]))
       (flatten)
       (sort-by :date)))

(def chores-by-day {:data     {:values chores-by-day-data}
                    :encoding {:x     {:field "date" :type "nominal"}
                               :y     {:field "count" :type "quantitative"}
                               :color {:field "person" :type "nominal"}}
                    :mark     "line"})

(oz/view! chores-by-day)

(def chores-by-month-data
  (->> chores-raw
       (sp/transform [sp/ALL] (fn [{:keys [id fields]}]
                                {:id     id
                                 :date   (:Date fields)
                                 :chore  (:Chore fields)
                                 :person (:Person fields)}))
       (group-by (fn [item]
                   (-> item (:date)
                       (clojure.string/split #"-")
                       ((fn [[y m d]] (str y "-" m))))))
       (map (fn [[date chores]]
              [{:date   date
                :count  (->> chores
                             (filter #(= "Justin" (:person %)))
                             (count))
                :person "Justin"}
               {:date   date
                :count  (->> chores
                             (filter #(= "Kaiti" (:person %)))
                             (count))
                :person "Kaiti"}]))
       (flatten)
       (sort-by :date)))

(def chores-by-month {:data   {:values chores-by-month-data}
                      :encoding {:x     {:field "date" :type "nominal"}
                                 :y     {:field "count" :type "quantitative"}
                                 :color {:field "person" :type "nominal"}}
                      :mark     "bar"})

(oz/view! chores-by-month)

(def chores-cumulative-data
  (let [sorted (->> chores-raw
                    (sp/transform
                     [sp/ALL]
                     (fn [{:keys [id fields]}]
                       {:id     id
                        :date   (:Date fields)
                        :chore  (:Chore fields)
                        :person (:Person fields)}))
                    ;; (remove #(-> % (:chore) (= "Dishes")))
                    (sort-by #(time-coerce/to-long (:date %))))]
    (->> sorted
         (map-indexed (fn [index {:keys [person] :as entry}]
                        (let [cumulative
                              (->> sorted
                                   (map-indexed (fn [i e] {:i i :e e}))
                                   (filter #(-> % :i (< index)))
                                   (filter #(-> % :e :person (= person)))
                                   (count)
                                   (+ 1))]
                          (merge entry {:cumulative cumulative}))))
         (map #(dissoc % :chore :id))
         ;; (filter #(-> %
         ;;              (:date)
         ;;              (time-format/parse)
         ;;              (time-pred/weekend?)))
         )))

(def chores-cumulative {:data     {:values chores-cumulative-data}
                        :encoding {:x     {:field "date" :type "nominal"}
                                   :y     {:field "cumulative" :type "quantitative"}
                                   :color {:field "person" :type "nominal"}}
                        :mark     "line"})

(oz/view! chores-cumulative)
