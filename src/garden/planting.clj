(ns garden.planting
  (:require [nextjournal.clerk :as clerk]
            [tick.core :as t]
            [tick.alpha.interval :as t.i]
            [hiccup.core :refer [html]])) 

{::clerk/visibility {:code :hide :result :hide}}
(comment
  (clerk/serve! {:watch-paths ["src" "resources"] :browse? true}) 
  (clerk/show! "src/garden/planting.clj") 
)
{::clerk/visibility {:code :show :result :show}}

(def days-of-this-year
  (let [intvl (t.i/bounds (t/year))]
    (t/range
     (t/beginning intvl)
     (t/end intvl)
     (t/new-period 1 :days))))

(defn days-until-next-week [date]
  (->> date
      t/day-of-week
      t/int
      (- 8)))

(defn week-number-of-year [date]
  (let [year (t/year date)]
    (loop [d (t/date (str year "-01-01"))
           w 1]
      (let [start-of-next-week (t/>> d (t/new-period (days-until-next-week d) :days))]
      (if (and (= (t/year start-of-next-week) year)
               (-> start-of-next-week
                   (t.i/relation date)
                   ((fn [r] (some #{:precedes :meets :equals} [r])))))
        (recur start-of-next-week (inc w))
        w)))))

(def calendar 
  (-> days-of-this-year
      (->> (group-by week-number-of-year))
      (->> (into (sorted-map)))
      (vals)
      (->> (into []))))

(clerk/html
 (html
  [:div.grid.grid-cols-1.divide-y
 [:div.grid.grid-cols-7.divide-x 
  (->> ["Mon" "Tue" "Wed" "Thu" "Fri" "Sat" "Sun"]
       (map (fn [dow] [:div.w-full.h-6 dow])))]
   (->> calendar
        (map (fn [week]
               [:div.grid.grid-cols-7.divide-x
                (->> week (map (fn [day]
                                 (let [d (t/day-of-month day)
                                       is-first (= d 1)]
                                  [:div.w-full.h-24.p-2
                                   (t/format (t/formatter "MMM dd") day)
                                   (if is-first

                                     (t/format (t/formatter "dd") day))]))))])))]))
