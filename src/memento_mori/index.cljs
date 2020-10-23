(ns memento-mori.index
  (:require [reagent.core :as r]
            [reagent.dom :as rd]
            [potpuri.core :refer [map-of]]
            [tick.alpha.api :as t]
            [cljc.java-time.local-date :as ld]))

(def years 90)
(def weeks-per-year 52)
(def birthday (ld/parse "1991-09-16"))

(def events [{:date-start (ld/of 1991 9 30)
              :name       "event 1"
              :color      "#ff00ff"}
             {:date-start (ld/of 1991 10 31)
              :date-end   (ld/of 1991 10 23)
              :name       "event 2"
              :color      "#fff000"}])

(defn event-overlap? [week-start week-end {:keys [date-start date-end]}]
  (let [week  {:tick/beginning week-start
               :tick/end       week-end}
        event {:tick/beginning date-start
               :tick/end       (if-some [e date-end] e date-start)}]
    (some? (some #{:precedes :preceded-by} [(t/relation event week)]))))

(defn create-week [year week-n]
  (let [week-start      (-> birthday (ld/plus-weeks week-n))
        week-end        (-> birthday
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

(->> years
     range
     (map create-year))

(defn view []
  [:div [:h1 "It works!"]])

(defn ^:dev/after-load start
  []
  (rd/render [view] (.getElementById js/document "app"))  )

(defn ^:export main
  []
  (start))
