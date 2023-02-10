(ns garden.planting
  (:require [nextjournal.clerk :as clerk]
            [tick.core :as t]
            [tick.alpha.interval :as t.i]
            ))

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

(clerk/html
 [:div.h-full.w-full.border-2.border-indigo-600
  #_(for [day days-of-this-year]
    [:div.h-4.w-2.bg-indigo-600 [:span "hello"]])])
