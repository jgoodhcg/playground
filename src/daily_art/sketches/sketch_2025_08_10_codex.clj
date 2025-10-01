(ns daily-art.sketches.sketch-2025-08-10-codex
  "Generative flow-field ribbons. Elegant, evolving motion."
  (:require [quil.core :as q]
            [daily-art.core :as art]))

(defn- random-particle
  [w h]
  {:x (q/random w)
   :y (q/random h)
   :px nil
   :py nil
   :h (q/random 190 300)   ;; cool hues: blue -> violet
   :s (q/random 50 90)})

(defn setup []
  (q/frame-rate 60)
  (q/color-mode :hsb 360 100 100 1.0)
  (q/background 230 40 6) ; deep night blue
  (let [w (q/width)
        h (q/height)
        count 900]
    {:particles (vec (repeatedly count #(random-particle w h)))
     :time 0
     :noise-scale 0.0025
     :speed 1.6}))

(defn- flow-angle
  [{:keys [x y]} noise-scale t]
  (let [n (q/noise (* x noise-scale)
                   (* y noise-scale)
                   (* t 0.005))]
    (* q/TWO-PI (+ n 0.15))))

(defn- wrap-or-reseed
  [{:keys [x y h s] :as p} w hgt]
  (if (and (<= -2 x (+' w 2))
           (<= -2 y (+' hgt 2)))
    p
    ;; reseed softly, keep hue/sat for palette continuity
    (assoc p :x (q/random w)
              :y (q/random hgt)
              :px nil :py nil
              :h (if (< (q/random 1) 0.15) (q/random 190 300) h)
              :s (if (< (q/random 1) 0.15) (q/random 50 90) s))))

(defn update-state
  [{:keys [particles noise-scale speed] :as state}]
  (let [w (q/width)
        h (q/height)
        t (:time state)
        ;; small global breathing to create subtle tempo shifts
        speed* (+ speed (* 0.4 (Math/sin (* 0.01 t))))]
    (-> state
        (update :time inc)
        (assoc :particles
               (mapv (fn [{:keys [x y] :as p}]
                       (let [a (flow-angle p noise-scale t)
                             dx (* speed* (Math/cos a))
                             dy (* speed* (Math/sin a))
                             nx (+ x dx)
                             ny (+ y dy)]
                         (-> p
                             (assoc :px x :py y :x nx :y ny)
                             (wrap-or-reseed w h))))
                     particles)))))

(defn draw-state
  [{:keys [particles time]}]
  ;; Fade the canvas slightly for trails
  (q/no-stroke)
  (q/fill 230 40 6 0.05)
  (q/rect 0 0 (q/width) (q/height))

  ;; Draw ribbons
  (doseq [{:keys [x y px py h s]} particles]
    (when (and px py)
      (let [twinkle (+ 70 (* 25 (Math/sin (* 0.02 (+ time x y)))))
            alpha 0.35
            weight (+ 0.6 (* 0.7 (q/noise (* 0.01 x) (* 0.01 y))))]
        (q/stroke h s twinkle alpha)
        (q/stroke-weight weight)
        (q/line px py x y)))))

(comment
  ;; Run this in your REPL to start the sketch
  (art/run-sketch
   {:title "Codex Ribbons — 2025-08-10"
    :size [1280 720]
    :setup setup
    :update update-state
    :draw draw-state})
  )
