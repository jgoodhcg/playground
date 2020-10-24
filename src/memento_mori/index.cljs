(ns memento-mori.index
  (:require [reagent.core :as r]
            [reagent.dom :as rd]
            [potpuri.core :refer [map-of]]
            [tick.alpha.api :as t]
            [cljc.java-time.local-date :as ld]
            [com.rpl.specter :as sp]))

(def years 90)
(def weeks-per-year 52)
(def birthday (ld/parse "1991-09-16"))
(def events [{:tick/beginning (ld/of 1991 9 30)
              :name           "event 1"
              :color          "#ff00ff"}
             {:tick/beginning (ld/of 1991 10 31)
              :tick/end       (ld/of 1991 10 23)
              :name           "event 2"
              :color          "#fff000"}
             {:tick/beginning (ld/of 2020 1 1)
              :tick/end       (ld/of 2020 12 31)
              :name           "event 2"
              :color          "#fff000"}])

(defn event-overlap? [week-start week-end event]
  (let [week            {:tick/beginning week-start
                         :tick/end       week-end}
        event-corrected {:tick/beginning (:tick/beginning event)
                         :tick/end       (if-some [e (:tick/end event)] e (:tick/beginning event))}]
    (not (some? (some #{:precedes :preceded-by} [(t/relation event-corrected week)])))))

(defn create-week [year week-n]
  (let [week-start      (-> birthday (ld/with-year year) (ld/plus-weeks week-n))
        week-end        (-> birthday
                            (ld/with-year year)
                            (ld/plus-weeks week-n)
                            (ld/plus-days 6))
        included-events (->> events
                             (filter (partial event-overlap?
                                              week-start
                                              week-end)))]

    (map-of week-start week-end included-events)))

(defn create-year [year-n]
  (let [year  (-> birthday (ld/get-year) (+ year-n))
        weeks (->> weeks-per-year
                   range
                   (map (partial create-week year)))]

    (map-of year weeks)))

(def data {:years (->> years
                       range
                       (map create-year))})

(defn view []
  [:div {:style {:display        "flex"
                 :flex-direction "column"}}
   (->> data
        :years
        (map (fn [{:keys [year weeks]}]
               [:div {:style {:display        "flex"
                              :flex-direction "row"}}
                (->> weeks
                     (map (fn [{:keys [included-events week-end]}]
                            [:div {:style {:width            6
                                           :height           6
                                           :background-color (if (not-empty included-events)
                                                               (->> included-events
                                                                    (sort-by (fn [event]
                                                                               (->>
                                                                                 (if-some [d (:tick/end event)]
                                                                                   d
                                                                                   (:tick/beginning event))
                                                                                 (ld/to-epoch-day))))
                                                                    last
                                                                    :color)
                                                               (if (ld/is-after week-end (ld/now))
                                                                 "blue"
                                                                 "grey"))
                                           :margin           0.5}}])))])))])

(defn ^:dev/after-load start
  []
  (rd/render [view] (.getElementById js/document "app"))  )

(defn ^:export main
  []
  (start))
