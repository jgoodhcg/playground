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

{::clerk/visibility {:code :fold :result :hide}}
(def days-of-this-year
  (->> (let [intvl (t.i/bounds (t/year))]
         (t/range
          (t/beginning intvl)
          (t/end intvl)
          (t/new-period 1 :days)))
       (map t/date)))

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

(defn fill-week
  "chatgpt wrote this function, I just renamed it and made the prefill item a hiccup vector"
  [vec]
  (let [num-to-add (- 7 (count vec))]
    (if (> num-to-add 0)
      (concat (repeat num-to-add [:div.w-full.h-24.p-2.bg-gray-200]) vec)
      vec))) 

(defn season
  "Chatgpt wrote this"
  [month]
  (let [winter #{12 1 2}
        spring #{3 4 5}
        summer #{6 7 8}
        fall #{9 10 11}]
    (cond
      (winter month) :winter
      (spring month) :spring
      (summer month) :summer
      (fall month) :fall
      :else :invalid))) 

{::clerk/visibility {:code :show :result :show}}
(clerk/html
(let [include-seasons true]
  (html
   [:div.grid.grid-cols-1.divide-y
    [:div.grid.grid-cols-7.divide-x
     (->> ["Mon" "Tue" "Wed" "Thu" "Fri" "Sat" "Sun"]
          (map (fn [dow] [:div.w-full.h-8.p-2.bg-gray-100 dow])))]
    (->> calendar
         (map (fn [week]
                [:div.grid.grid-cols-7.divide-x
                 (->> week
                      (map (fn [day]
                             (let [d (t/day-of-month day)
                                   is-first (= d 1)
                                   is-today (t/= day (t/date))
                                   this-season (season (-> day t/month t/int))]
                               [(keyword (str "div.w-full.h-24"
                                              (cond is-today ".bg-indigo-200"
                                                    is-first ".bg-gray-100")))
                                (when include-seasons
                                  (cond
                                    (= this-season :spring) [:div.w-full.h-1.bg-green-100]
                                    (= this-season :summer) [:div.w-full.h-1.bg-lime-100]
                                    (= this-season :fall)   [:div.w-full.h-1.bg-amber-100]
                                    (= this-season :winter) [:div.w-full.h-1.bg-blue-100]))
                                [:div.m-2
                                 (t/format (t/formatter (str (when is-first "MMM ")
                                                             "dd")) day)]])))
                      fill-week
                      )])))])))
 





 