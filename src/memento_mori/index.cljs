(ns memento-mori.index
  (:require [reagent.core :as r]
            [reagent.dom :as rd]
            [potpuri.core :refer [map-of]]
            [tick.alpha.api :as t]
            [cljc.java-time.local-date :as ld]
            [com.rpl.specter :as sp]))

(def grey-50 "#fafafa")
(def grey-100 "#f5f5f5")
(def grey-300 "#e0e0e0")
(def grey-400 "#bdbdbd")
(def grey-500 "#9e9e9e")
(def grey-900 "#212121")

(def brown-300 "#a1887f")
(def blue-200 "#90caf9")
(def light-green-400 "#9ccc65")
(def red-400 "#ef5350")
(def red-600 "#e53935")
(def red-800 "#c62828")
(def indigo-200 "#9fa8da")
(def indigo-300 "#7986cb")
(def indigo-500 "#3f51b5")
(def deep-purple-100 "#d1c4e9")
(def deep-purple-300 "#9575cd")
(def deep-purple-500 "#673ab7")
(def deep-purple-700 "#512da8")
(def deep-purple-800 "#4527a0")

(def years 90)
(def weeks-per-year 52)
(def birthday (ld/parse "1991-09-16"))
(def events [
             {:name           "elementary and middle school"
              ;; :color          indigo-200
              :color          indigo-200
              :tick/beginning (ld/of 1996 9 5)
              :tick/end       (ld/of 2006 6 6)}
             {:name           "high school"
              ;; :color          brown-300
              :color          indigo-300
              :tick/beginning (ld/of 2006 9 5)
              :tick/end       (ld/of 2010 6 6)}
             {:name           "college"
              ;; :color          blue-200
              :color          indigo-500
              :tick/beginning (ld/of 2010 9 5)
              :tick/end       (ld/of 2015 8 6)}
             {:name           "Wyoming IT intern"
              ;; :color          light-green-400
              :color          deep-purple-300
              :tick/beginning (ld/of 2015 5 19)
              :tick/end       (ld/of 2015 11 13)}
             {:name           "One stop web developer"
              ;; :color          grey-900
              :color          deep-purple-500
              :tick/beginning (ld/of 2016 2 18)
              :tick/end       (ld/of 2017 5 11)}
             {:name           "Tek Systems (GFS) software developer "
              ;; :color          red-600
              :color          deep-purple-700
              :tick/beginning (ld/of 2017 5 11)
              :tick/end       (ld/of 2018 5 14)}
             {:name           "GFS full stack developer"
              ;; :color          red-800
              :color          deep-purple-800
              :tick/beginning (ld/of 2018 5 14)
              :tick/end       (ld/of 2020 10 23)}
             {:name           "Archemedx senior backend developer"
              ;; :color          red-800
              :color          deep-purple-100
              :tick/beginning (ld/of 2020 11 9) ;; why does it only show one square between 2020-10-23 instead of two?
              :tick/end       (ld/of 2022 12 31)}
             ])

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

(defn determine-color [included-events week-end]
  (if (not-empty included-events)
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
      "grey")))

(defn render-weeks [weeks]
  (->> weeks
       (map (fn [{:keys [included-events week-end]}]
              [:div {:style {:width            6
                             :height           6
                             :background-color (determine-color included-events week-end)
                             :margin           0.5}}]))))

(defn render-years [years]
  (->> years
       (map (fn [{:keys [year weeks]}]
              [:div {:style {:display        "flex"
                             :flex-direction "row"}}
               (->> weeks
                    (render-weeks))]))))

(defn view []
  [:div {:style {:display        "flex"
                 :flex-direction "column"}}
   (->> data
        :years
        (render-years))])

(defn ^:dev/after-load start
  []
  (rd/render [view] (.getElementById js/document "app"))  )

(defn ^:export main
  []
  (start))
