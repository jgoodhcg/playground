(ns quil-intro
  (:require [quil.core :as q]
            [quil.middleware :as m]))

(defrecord Pill [x y dx dy])

(defn setup []
  (q/frame-rate 30)
  (q/background 255)
  (vec (repeatedly 50 #(->Pill (q/random (q/width))
                               (q/random (q/height))
                               (q/random -1 1)
                               (q/random -1 1)))))

(defn update-pill [pill]
  (->Pill (mod (+ (:x pill) (:dx pill)) (q/width))
          (mod (+ (:y pill) (:dy pill)) (q/height))
          (:dx pill)
          (:dy pill)))

(defn update-state [pills]
  (map update-pill pills))

(defn draw-state [pills]
  (q/background 255)
  (doseq [{:keys [x y]} pills]
    (q/ellipse x y 50 100)))

(q/defsketch example
  :title "Wandering Pills"
  :setup setup
  :update update-state
  :draw draw-state
  :size [1920 1080]
  :middleware [m/fun-mode])
