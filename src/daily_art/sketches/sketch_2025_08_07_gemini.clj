(ns daily-art.sketches.sketch-2025-08-07-gemini
  (:require [quil.core :as q]
            [daily-art.core :as art]))

(defn setup []
  (q/frame-rate 30)
  (q/color-mode :hsb 360 100 100 1.0)
  (q/background 0 0 95)
  {})

(defn draw-state [state]
  (let [x-center (/ (q/width) 2)
        y-center (/ (q/height) 2)
        circle-size 100
        hue (mod (q/frame-count) 360)]
    (q/fill hue 80 80)
    (q/no-stroke)
    (q/ellipse x-center y-center circle-size circle-size)))

(comment
  ;; Run this in the REPL to start the sketch
  (art/run-sketch
   {:title "Gemini Basic Sketch"
    :size [800 600]
    :setup setup
    :draw draw-state})
  ;;
  )
