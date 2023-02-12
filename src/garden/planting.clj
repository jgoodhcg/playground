;; # 🍅 Planting Stuff
;; I'm not quite satisfied with the thins I've tried for managing my garden.
;; I want to play around with visualizing some kind of calendar that tells me when to plant my seeds.
(ns garden.planting
  {:nextjournal.clerk/error-on-missing-vars :off
   :nextjournal.clerk/toc true}
  (:require [nextjournal.clerk :as clerk]
            [tick.core :as t]
            [tick.alpha.interval :as t.i]
            [hiccup.core :refer [html]]
            [garden.db :refer [planting-data]]
            [potpuri.core :as pot])) 

{::clerk/visibility {:code :hide :result :hide}}
(comment
  (clerk/serve! {:watch-paths ["src" "resources"] :browse? true}) 
  (clerk/show! "src/garden/planting.clj") 
)
{::clerk/visibility {:code :show :result :show}}

;; ## 🛠️ Getting setup
;; Going to focus just on this year for now
;; ### ✳️ Days in the year
(def days-of-this-year
  (->> (let [intvl (t.i/bounds (t/year))]
         (t/range
          (t/beginning intvl)
          (t/end intvl)
          (t/new-period 1 :days)))
       (map t/date)))

;; ### ✳️ Calendar utils
;; These are some utilities I pulled in from another project.
;; Someday I'll put these in a utility library.
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

;; ChatGPT helped me write these two functions.
;; This first one usses some hiccup that will make sense in a bit. 
;; I probably should have not included hiccup here to keep it more generic.
(defn fill-week
  "chatgpt wrote this function, I just renamed it and made the prefill item a hiccup vector"
  [vec]
  (let [num-to-add (- 7 (count vec))]
    (if (> num-to-add 0)
      (concat (repeat num-to-add [:div.w-full.h-fit.p-2]) vec)
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

;; ## 🗓️ Make a calendar
;; ### ✳️ List of weeks
(def calendar 
  (-> days-of-this-year
      (->> (group-by week-number-of-year))
      (->> (into (sorted-map)))
      (vals)
      (->> (into []))))

;; ### 🌱 Plants and timing
;; Try as I might I could not get ChatGPT to convert some spreadsheets into edn data.
;; I had to write this by hand and fill in all the offsets from a spreadsheet I found online.
;; https://www.ufseeds.com/crop-calculators.html
(identity planting-data)

;; List of all the plants
(def plants (-> planting-data keys))

;; A function that says what to plant given a day
(defn plantings-for-day [{:keys [day last-frost first-frost]}]
  (->> plants
       (map (fn [plant]
              (let [info (get planting-data plant)

                    {spring-start-offset      :start
                     spring-sow-offset        :sow
                     spring-transplant-offset :transplant}
                    (:spring info)

                    {fall-start-offset      :start
                     fall-sow-offset        :sow
                     fall-transplant-offset :transplant}
                    (:fall info)

                    offset-days (fn [offset frost]
                                  (when offset
                                    (-> frost
                                        (t/>> (t/new-period offset :days)))))

                    spring-start      (offset-days spring-start-offset last-frost)
                    spring-sow        (offset-days spring-sow-offset last-frost)
                    spring-transplant (offset-days spring-transplant-offset last-frost)
                    fall-start        (offset-days fall-start-offset first-frost)
                    fall-sow          (offset-days fall-sow-offset first-frost)
                    fall-transplant   (offset-days fall-transplant-offset first-frost)

                    action-today (fn [spring-action fall-action day]
                                       (or (= spring-action day)
                                           (= fall-action day)))

                    start      (action-today spring-start fall-start day)
                    sow        (action-today spring-sow fall-sow day)
                    transplant (action-today spring-transplant fall-transplant day)]

                  (pot/map-of plant start sow transplant)))))) 

;; ### ✳️ Gut check
(def last-frost (t/date "2023-05-15")) 
(def first-frost (t/date "2023-09-29")) 
(def day (-> last-frost (t/>> (t/new-period -56 :days)))) 
(plantings-for-day (pot/map-of day last-frost first-frost)) 

;; ### 🎨 Rendering
(merge 
 {:nextjournal/width :full}
 (clerk/html
  (let [include-seasons true]
    (html
     [:div.grid.grid-cols-1.divide-y.w-full.px-1.bg-gray-100
      [:div.grid.grid-cols-7.divide-x
       (->> ["Mon" "Tue" "Wed" "Thu" "Fri" "Sat" "Sun"]
            (map (fn [dow] [:div.w-full.h-8.p-2 dow])))]
      (->> calendar
           (map (fn [week]
                  [:div.grid.grid-cols-7.divide-x
                   (->> week
                        (map (fn [day]
                               (let [d (t/day-of-month day)
                                     is-first (= d 1)
                                     is-today (t/= day (t/date))
                                     this-season (season (-> day t/month t/int))
                                     plantings (plantings-for-day 
                                                (pot/map-of day first-frost last-frost))]
                                 [(keyword (str "div.w-full.h-fit.pb-2"
                                                (cond is-today ".bg-indigo-200"
                                                      is-first (case this-season
                                                                 :spring ".bg-green-100"
                                                                 :summer ".bg-lime-100"
                                                                 :fall   ".bg-amber-100"
                                                                 :winter ".bg-blue-100")
                                                      :else    ".bg-white")))
                                  (when include-seasons
                                    (case this-season
                                      :spring [:div.w-full.h-1.bg-green-100]
                                      :summer [:div.w-full.h-1.bg-lime-100]
                                      :fall   [:div.w-full.h-1.bg-amber-100]
                                      :winter [:div.w-full.h-1.bg-blue-100]))
                                  [:div.m-2
                                   (t/format (t/formatter (str (when is-first "MMM ")
                                                               "dd")) day)]
                                  [:div.bg-green-200
                                   (->> plantings
                                        (map (fn [{:keys [start sow transplant plant]}]
                                               (when (some true? [start sow transplant])
                                                 [:div
                                                  (str (when start "🌱 ")
                                                       (when sow "🌽 ")
                                                       (when transplant "🌲 ")
                                                       (name plant) " ")]))))]])))
                        fill-week)])))]))))

;; ## 🔃 How about an agenda?

;; ### ✳️ Generating data
(def agenda-data 
  (->> days-of-this-year
       (map (fn [day]
              (let [is-today (= day (t/date))
                    is-first-frost (= day first-frost)
                    is-last-frost (= day last-frost)
                    this-season (season (-> day t/month t/int))
                    plantings (->> (pot/map-of day first-frost last-frost)
                                   plantings-for-day
                                   (map (fn [{:keys [start sow transplant plant]}]
                                          (when (some true? [start sow transplant])
                                            (str (when start "🌱 ")
                                                 (when sow "🌽 ")
                                                 (when transplant "🌲 ")
                                                 (name plant)))))
                                   (remove nil?))]
                (when (or is-today
                          is-first-frost
                          is-last-frost
                          (not-empty plantings))
                  {:date           day
                   :is-today       is-today
                   :is-first-frost is-first-frost
                   :is-last-frost  is-last-frost
                   :season         this-season
                   :plantings      plantings}))))
       (remove nil?)
       (sort-by :date)))

;; ### 🎨 Rendering
(clerk/html
 (html
  [:div
   (->> agenda-data
        (map (fn [{:keys [date is-today plantings season is-last-frost is-first-frost]}]
               [(keyword (str "div.w-40.border-l-2.border-r-2.border-b-2.my-2.rounded"
                              (if (or is-today
                                      is-last-frost
                                      is-first-frost) ".border-indigo-200"
                                  (case season
                                    :spring ".border-teal-100"
                                    :summer ".border-green-100"
                                    :fall ".border-amber-100"
                                    :winter ".border-blue-100"))))
                [(keyword (str "div.p-1"
                               (if (or is-today
                                      is-last-frost
                                      is-first-frost) ".bg-indigo-200"
                                   (case season
                                     :spring ".bg-teal-100"
                                     :summer ".bg-green-100"
                                     :fall ".bg-amber-100"
                                     :winter ".bg-blue-100"))))
                 (str (when is-today "📅 ")
                      (when is-first-frost "⛄ ")
                      (when is-last-frost "⛄ ")
                      (->> date (t/format (t/formatter "MMM dd"))))]

                (when (not-empty plantings)
                  [:div.p-1
                   (->> plantings
                        (map (fn [s] [:div s])))])])))]))


 