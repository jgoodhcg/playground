(ns daily-art.sketches.sketch-2025-08-07-gemini-hex
  (:require [quil.core :as q]
            [daily-art.core :as art]))

(defn- draw-hexagon
  "Private helper to draw a single hexagon at a given location and size."
  [x y size]
  (q/begin-shape)
  (doseq [angle (range 0 q/TWO-PI (/ q/TWO-PI 6))]
    (let [vx (+ x (* size (Math/cos angle)))
          vy (+ y (* size (Math/sin angle)))]
      (q/vertex vx vy)))
  (q/end-shape :close))

(defn setup []
  (q/color-mode :hsb 360 100 100 1.0)
  {:hex-size 30
   :time 0})

(defn update-state [state]
  (update state :time inc))

(defn draw-state
  "Draws an animated grid of hexagons with properties determined by Perlin noise."
  [{:keys [hex-size time]}]
  (q/background 240 30 5) ;; Deep blue background
  (q/no-stroke)

  ;; Grid layout calculations
  (let [hex-width (* (Math/sqrt 3) hex-size)
        hex-height (* 2 hex-size)
        spacing-x hex-width
        spacing-y (* hex-height 0.75)
        noise-scale 0.005 ;; Zoom level for the noise
        time-scale 0.01] ;; Speed of the animation

    ;; Iterate over a grid to place hexagons
    (doseq [row (range -1 (inc (/ (q/height) spacing-y)))]
      (doseq [col (range -1 (inc (/ (q/width) spacing-x)))]
        (let [x (* col spacing-x)
              y (* row spacing-y)
              ;; Offset every other row for a proper hex grid
              offset-x (if (even? row)
                         x
                         (+ x (/ spacing-x 2)))
              ;; Calculate a noise value for this position and time
              noise-val (q/noise (* offset-x noise-scale)
                                 (* y noise-scale)
                                 (* time time-scale))
              ;; Map the noise value (0 to 1) to visual properties
              hue (q/map-range noise-val 0 1 180 300) ;; Blues to Purples
              brightness (q/map-range noise-val 0 1 70 100)
              alpha (q/map-range noise-val 0 1 0.7 1.0)
              current-size (q/map-range noise-val 0 1 0 hex-size)]
          
          (q/fill hue 80 brightness alpha) ;; Use the calculated properties
          (draw-hexagon offset-x y current-size))))))

(comment
  ;; Run this in your REPL to start the sketch
  (art/run-sketch
   {:title "Hexagonal Final: Animated Flow"
    :size [1280 720]
    :setup setup
    :update update-state
    :draw draw-state})

  ;; Press ESC to close the sketch window
  )
