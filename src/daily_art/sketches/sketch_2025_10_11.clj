(ns daily-art.sketches.sketch-2025-10-11
  (:require [quil.core :as q]
            [daily-art.core :as art]))

(defn setup []
  (q/frame-rate 30)
  (q/color-mode :hsb 360 100 100 1.0)
  (q/background 10)  ; Dark background
  
  ;; Draw random starfield with Perlin noise opacity and subtle colors
  (q/noise-seed 12345)
  (doseq [_ (range 800)]
    (let [x (rand 800)
          y (rand 800)
          noise-val (q/noise (* x 0.01) (* y 0.01))
          brightness (* 70 noise-val)
          size (+ 0.5 (* 4 noise-val))
          ;; Subtle star colors: white, blue-white, yellow-white
          hue (cond 
                (< (rand) 0.6) 200  ; Cool blue-white
                (< (rand) 0.8) 60   ; Warm yellow-white  
                :else 0)            ; Pure white
          saturation (* 15 (rand))  ; Very low saturation
          ]
      (when (> noise-val 0.2)
        (q/stroke hue saturation brightness 0.7)
        (q/stroke-weight size)
        (q/point x y))))
  
  (let [golden-angle (* 2 Math/PI (/ 1 1.618))]  ; Golden angle
    ;; Generate all nodes first
    (let [nodes (for [i (range 60)]
                  (let [angle (* i golden-angle)
                        radius (* 42 (Math/sqrt i))
                        base-x (+ 400 (* radius (Math/cos angle)))
                        base-y (+ 400 (* radius (Math/sin angle)))
                        x (+ base-x (* 8 (- (rand) 0.5)))
                        y (+ base-y (* 8 (- (rand) 0.5)))]
                    {:x x :y y :id i}))]
      
      ;; Draw ethereal connections first (back layer)
      (doseq [node1 nodes
              node2 nodes
              :when (< (:id node1) (:id node2))
              :let [dist (Math/sqrt (+ (Math/pow (- (:x node1) (:x node2)) 2)
                                       (Math/pow (- (:y node1) (:y node2)) 2)))]
              :when (< dist 80)]
        (let [opacity (* 0.3 (- 1 (/ dist 80)))]
          (q/stroke 200 30 60 opacity)
          (q/stroke-weight 0.5)
          (q/line (:x node1) (:y node1) (:x node2) (:y node2))))
      
      ;; Draw spiral connections (middle layer)
      (doseq [i (range (dec (count nodes)))]
        (let [node1 (nth nodes i)
              node2 (nth nodes (inc i))
              opacity (* 0.6 (+ 0.4 (* 0.6 (Math/sin (* i 0.1))))) ]
          (q/stroke 200 40 70 opacity)
          (q/stroke-weight 1)
          (q/line (:x node1) (:y node1) (:x node2) (:y node2))))
      
      ;; Draw nodes with tri-circle overlap (front layer)
      (doseq [{:keys [x y id]} nodes]
        (let [pulse (+ 0.8 (* 0.2 (Math/sin (* id 0.2))))]
          ;; Main circle
          (q/stroke 200 40 80 (* 0.9 pulse))
          (q/ellipse x y (* 4 pulse) (* 4 pulse))
          ;; Left overlap - slightly different hue
          (q/stroke 195 45 85 (* 0.7 pulse))
          (q/ellipse (- x 1) y (* 3 pulse) (* 3 pulse))
          ;; Right overlap - slightly different hue
          (q/stroke 205 35 75 (* 0.7 pulse))
          (q/ellipse (+ x 1) y (* 3 pulse) (* 3 pulse)))))
   
  ;; Save the frame
  (q/save-frame "podcast-cover-art.png")
  {}))

(comment
  (art/run-sketch
   {:title "Elegant Dark Nodes"
    :size [800 800]
    :setup setup})

  
  )
