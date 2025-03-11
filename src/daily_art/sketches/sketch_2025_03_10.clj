(ns daily-art.sketches.sketch-2025-03-10
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [daily-art.core :as art]))

(defn setup []
  (q/frame-rate 30)
  (q/color-mode :hsb 360 100 100 1.0)
  (q/background 200 30 95)
  {})

(defn draw-hexagon [x y size color]
  (q/push-matrix)
  (q/translate x y)
  
  (apply q/fill color)
  (q/no-stroke)
  (q/begin-shape)
  (doseq [angle (range 0 (* 2 Math/PI) (/ (* 2 Math/PI) 6))]
    (q/vertex (* size (Math/cos angle)) 
              (* size (Math/sin angle))))
  (q/end-shape :close)
  
  (q/pop-matrix))

(defn draw-hexagon-sun [x y size]
  ;; Colors for rings
  (let [white [0 0 100]
        sky-blue [200 30 95]
        ring-count 6
        base-size size]
    
    ;; Draw alternating rings from back to front (largest to smallest)
    (doseq [i (range ring-count 0 -1)]
      (let [;; For sizing:
            ;; - For sky-blue (even i), use normal size spacing
            ;; - For white rings (odd i), make them thinner
            scale-factor (if (even? i)
                           ;; Blue rings - normal spacing
                           (+ 0.8 (* i 0.25))
                           ;; White rings - thinner (closer to the next blue ring)
                           (+ 0.8 (* (- i 0.5) 0.25)))
            current-size (* base-size scale-factor)
            ;; Alternate between sky blue and white
            ;; Start with sky blue for outer rings
            color (if (even? i) sky-blue white)]
        (draw-hexagon x y current-size color)))
    
    ;; Draw the central white hexagon as the sun
    (draw-hexagon x y base-size white)))

(defn draw-ground [y-horizon]
  ;; Draw ground
  (q/fill 0 0 100)
  (q/no-stroke)
  (q/rect 0 y-horizon (q/width) (- (q/height) y-horizon)))

(defn draw-state [state]
  (q/background 200 30 95)
  
  (let [y-horizon (* (q/height) 0.6)
        sun-x (* (q/width) 0.5)
        sun-y (* (q/height) 0.4)
        sun-size 50]
    
    ;; Draw elements from back to front
    (draw-ground y-horizon)
    (draw-hexagon-sun sun-x sun-y sun-size))
  
  ;; Uncomment to save a frame
  ;; (art/save-frame "sunrise-hexagon")
  )

(comment
  ;; Run this in the REPL to start the sketch
  (art/run-sketch
   {:title "Hexagon Sunrise"
    :size [1280 720]  ;; 16:9 aspect ratio
    :setup setup
    :draw draw-state})
  
  ;; Press ESC to close the sketch window
)
