(ns nbb.moon-stuff
  (:require ["suncalc$default" :as suncalc]))

(def latitude 42.9)

(def longitude -85.6)

 ;; not (js/Date) that returns a string, (js/Date.) returns an #inst
(def today (js/Date.))

(def moon-position (suncalc/getMoonPosition today latitude longitude))

(def sun-times (suncalc/getTimes today latitude longitude))

(def moon-illumination (suncalc/getMoonIllumination today))

(println {:latitude          latitude
          :longitude         longitude
          :today             today
          :moon-position     moon-position
          :sun-times         sun-times
          :moon-illumination moon-illumination})

(defn determine-phase
  [phase-number]
  (cond
    (and (>= phase-number 0.0)
         (< phase-number 0.1))
    :new-moon

    (and (>= phase-number 0.1)
         (< phase-number 0.25))
    :waxing-crecent

    (and (>= phase-number 0.25)
         (< phase-number 0.3))
    :first-quarter

     (and (>= phase-number 0.3)
         (< phase-number 0.5))
    :waxing-gibbous

    (and (>= phase-number 0.5)
         (< phase-number 0.6))
    :full-moon

    (and (>= phase-number 0.6)
         (< phase-number 0.75))
    :waning-gibbous

    (and (>= phase-number 0.75)
         (< phase-number 0.8))
    :last-quarter

    (and (>= phase-number 0.8)
         (< phase-number 1))
    :waning-crescent

    true :error-determining-phase))

(println (str "Moon is currently " (-> moon-illumination (.-phase) determine-phase)))
