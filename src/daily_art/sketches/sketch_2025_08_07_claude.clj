(ns daily-art.sketches.sketch-2025-08-07-claude
  (:require [quil.core :as q]
            [daily-art.core :as art]))

(defn golden-ratio []
  1.618033988749895)

(defn fibonacci-spiral-point
  "Calculate point on a Fibonacci spiral using golden angle"
  [n radius-scale]
  (let [golden-angle (* 2 Math/PI (/ 1 (golden-ratio)))
        angle (* n golden-angle)
        r (* radius-scale (Math/sqrt n))]
    [(* r (Math/cos angle))
     (* r (Math/sin angle))]))

(defn setup []
  (q/frame-rate 60)
  (q/color-mode :hsb 360 100 100 1.0)
  (q/background 240 5 98)
  {:particles (range 500)
   :time 0})

(defn ease-in-out-cubic [t]
  (if (< t 0.5)
    (* 4 t t t)
    (- 1 (Math/pow (* -2 (+ t -1)) 3) -0.5)))

(defn update-state [state]
  (update state :time inc))

(defn draw-state [{:keys [particles time] :as state}]
  (q/fill 240 5 98 0.02)
  (q/rect 0 0 (q/width) (q/height))
  
  (let [center-x (/ (q/width) 2)
        center-y (/ (q/height) 2)
        base-scale 8
        breath (* 0.3 (Math/sin (* time 0.02)))]
    
    (doseq [i particles]
      (let [[x y] (fibonacci-spiral-point i base-scale)
            progress (ease-in-out-cubic (/ (mod (+ time (* i 0.5)) 360) 360))
            rotation (* progress 2 Math/PI)
            scale (+ 1 (* breath (Math/sin (* i 0.1))))
            
            final-x (+ center-x (* scale (- (* x (Math/cos rotation))
                                           (* y (Math/sin rotation)))))
            final-y (+ center-y (* scale (+ (* x (Math/sin rotation))
                                           (* y (Math/cos rotation)))))
            
            hue (mod (+ 200 (* 60 (Math/sin (* i 0.05)))) 360)
            saturation (+ 30 (* 40 progress))
            brightness (+ 60 (* 30 (Math/sin (* time 0.01))))
            size (+ 2 (* 3 (Math/sin (+ (* i 0.1) (* time 0.03)))))]
        
        (q/no-stroke)
        (q/fill hue saturation brightness 0.7)
        (q/ellipse final-x final-y size size)))))

(comment
  ;; Run this in the REPL to start the sketch
  (art/run-sketch
   {:title "Claude Fibonacci Bloom"
    :size [800 800]
    :setup setup
    :update update-state
    :draw draw-state})
  ;;
  )
