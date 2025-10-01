(ns daily-art.sketches.sketch-2025-08-11-codex
  "Setting sun with atmospheric wobble."
  (:require [quil.core :as q]
            [daily-art.core :as art]))

(defn- make-wobble-ellipse [radius t]
  (let [base-radius (* radius (q/random 1.05 1.25))]
    {:angle (q/random (* 2 Math/PI))
     :speed (q/random -0.005 0.005)
     :x-radius (* base-radius (q/random 0.8 1.2))
     :y-radius (* base-radius (q/random 0.8 1.2))
     :offset [(* radius (q/random -0.1 0.1))
              (* radius (q/random -0.1 0.1))]
     :opacity (q/random 0.05 0.15)
     :seed (q/random 10000)}))

(defn setup []
  (q/frame-rate 60)
  (q/color-mode :hsb 360 100 100 1.0)
  (let [w (q/width)
        h (q/height)
        cx (/ w 2)
        cy (* h 0.6)                 ; sit low like a setting sun
        radius (* (min w h) 0.22)
        wobble-ellipse-count 8]
    {:time 0
     :center [cx cy]
     :radius radius
     :bg [250 35 8]                  ; deep dusk blue
     :sun-h 8                        ; vivid warm red/orange
     :wobble-ellipses (vec (repeatedly wobble-ellipse-count #(make-wobble-ellipse radius 0)))}))

(defn update-state [{:keys [wobble-ellipses] :as state}]
  (let [t (inc (:time state))]
    (-> state
        (assoc :time t)
        (assoc :wobble-ellipses
               (mapv (fn [e]
                       (-> e
                           (update :angle + (:speed e))
                           (update :x-radius + (* 0.1 (q/noise (* 0.1 t) (:seed e))))
                           (update :y-radius + (* 0.1 (q/noise (* 0.1 t) (+ (:seed e) 10))))))
                     wobble-ellipses)))))

(defn- draw-wobble-ellipses
  [ellipses [cx cy] sun-h]
  (q/no-stroke)
  (doseq [{:keys [angle speed x-radius y-radius offset opacity]} ellipses]
    (q/push-matrix)
    (q/translate (+ cx (first offset)) (+ cy (second offset)))
    (q/rotate angle)
    (q/fill sun-h 85 95 opacity)
    (q/ellipse 0 0 x-radius y-radius)
    (q/pop-matrix)))

(defn- draw-sun-disc
  [[cx cy] radius h]
  ;; Soft radial falloff by concentric rings
  (q/no-stroke)
  (doseq [i (range 70 0 -1)]
    (let [t (/ i 70.0)
          r (* radius t)
          sat (+ 70 (* 25 (- 1 t)))   ; slightly more saturated toward rim
          bri (+ 70 (* 30 t))]        ; brightest near center
      (q/fill h sat bri 1.0)
      (q/ellipse cx cy (* 2 r) (* 2 r))))
  ;; Thin bright rim for definition
  (q/no-fill)
  (q/stroke h 85 95 0.9)
  (q/stroke-weight 2.2)
  (q/ellipse cx cy (* 2 radius) (* 2 radius)))

(defn draw-state
  [{:keys [center radius bg sun-h wobble-ellipses]}]
  ;; Clear with solid dusk background each frame (no trails)
  (apply q/fill bg)
  (q/no-stroke)
  (q/rect 0 0 (q/width) (q/height))
  ;; Draw wobbling ellipses behind the sun
  (draw-wobble-ellipses wobble-ellipses center sun-h)
  ;; Draw sun disc first
  (draw-sun-disc center radius sun-h))

(comment
  ;; Run in the REPL to preview the sketch
  (art/run-sketch
   {:title "Codex — Setting Sun — 2025-08-11"
    :size [1280 720]
    :setup setup
    :update update-state
    :draw draw-state})
  )
