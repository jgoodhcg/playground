(ns daily-art.sketches.sketch-2025-08-10-gemini
  (:require
   [quil.core :as q]
   [daily-art.core :as art]))

(def palette
  {:background [20 25 30]
   :stroke [230 230 240 220]})

(def settings
  {:title "Evolving Flow Field"
   :size [1080 1080]
   :renderer :p2d})

(defn setup []
  (q/color-mode :hsb 360 100 100 1.0)
  (q/stroke-weight 1)
  (q/no-fill)
  ;; Return the initial state
  {:t 0})

(defn update-state [state]
  ;; Increment the time value in the state
  (update state :t inc))

(defn draw [state]
  (let [{:keys [background stroke]} palette
        [width height] (:size settings)
        t (:t state)
        time-scale 0.005
        step 20
        noise-scale 0.005
        line-length 15]
    (q/background (nth background 0) (nth background 1) (nth background 2))
    (q/stroke (nth stroke 0) (nth stroke 1) (nth stroke 2) (nth stroke 3))

    (doseq [x (range 0 width step)
            y (range 0 height step)]
      (let [;; Use the time value to animate the noise field
            noise-val (q/noise (* x noise-scale) (* y noise-scale) (* t time-scale))
            angle (* noise-val q/TWO-PI 2)
            x2 (+ x (* line-length (q/cos angle)))
            y2 (+ y (* line-length (q/sin angle)))]
        (q/line x y x2 y2)))))

(comment
  ;; Run this in the REPL to start the sketch
  (art/run-sketch
   {:title (:title settings)
    :size (:size settings)
    :setup setup
    :update update-state
    :draw draw})

  ;; To save a frame
  (art/save-frame "sketch-2025-08-10-gemini")
  )