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

;; Calendar anyone?
(def days-of-this-year
  (let [intvl (t.i/bounds (t/year))]
    (t/range
     (t/beginning intvl)
     (t/end intvl)
     (t/new-period 1 :days))))

(def calendar
  [:div.h-fit.w-100.flex.flex-row.flex-wrap
   (doall
    (->> days-of-this-year
         (map (fn [day]
                [:div.h-24.w-20.bg-indigo-600.text-white [:span (str day)]]))))])

(clerk/html (html calendar))
