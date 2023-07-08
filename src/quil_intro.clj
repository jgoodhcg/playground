(ns quil-intro
  (:require [quil.core :as q]
            [quil.middleware :as m]))

(defrecord Pill [x y dx dy color])

(defn setup []
  (q/frame-rate 30)
  (q/background 255)
  (vec (repeatedly 50 #(->Pill (q/random (q/width))
                               (q/random (q/height))
                               (q/random -1 1)
                               (q/random -1 1)
                               [0 0 0]))))

(defn update-pill [pills pill]
  (let [new-pill (->Pill (mod (+ (:x pill) (:dx pill)) (q/width))
                         (mod (+ (:y pill) (:dy pill)) (q/height))
                         (:dx pill)
                         (:dy pill)
                         (:color pill))]
    (if (some #(and (not= % pill)
                    (< (q/dist (:x new-pill) (:y new-pill) (:x %) (:y %)) 15))
              pills)
      (->Pill (:x new-pill)
              (:y new-pill)
              (q/random -1 1)
              (q/random -1 1)
              (map #(-> %
                        (+ (* (rand 1) 250))
                        (mod 255)
                        float
                        (Math/round)
                        (max 0)
                        (min 255)) [0 0 0]))
      new-pill)))

(defn update-state [pills]
  (map (partial update-pill pills) pills))

(defn draw-state [pills]
  (q/background 255)
  (doseq [{:keys [x y color]} pills]
    (apply q/fill color)
    (q/ellipse x y 10 20)))

(q/defsketch example
  :title "Wandering Pills"
  :setup setup
  :update update-state
  :draw draw-state
  :size [1920 1080]
  :middleware [m/fun-mode])
