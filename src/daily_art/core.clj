(ns daily-art.core
  (:require [quil.core :as q]
            [quil.middleware :as m]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn save-frame
  "Save the current frame with a formatted filename"
  [sketch-name]
  (let [dir (io/file "output" sketch-name)
        timestamp (str/replace (str (java.time.LocalDateTime/now)) #"[:.T]" "-")
        filename (format "%s-%s.png" sketch-name timestamp)]
    (.mkdirs dir)
    (q/save-frame (str "output/" sketch-name "/" filename))))

(defn setup-sketch
  "Common setup function with standard parameters"
  [width height & {:keys [frame-rate background-color] 
                   :or {frame-rate 30 
                        background-color 255}}]
  (q/frame-rate frame-rate)
  (q/background background-color)
  {})

(defn run-sketch
  "Utility to run a sketch with standard parameters"
  [options]
  (q/sketch
    :title (:title options "Daily Sketch")
    :size (:size options [800 600])
    :setup (:setup options setup-sketch)
    :update (:update options identity)
    :draw (:draw options (fn [_] nil))
    :features [:keep-on-top]
    :middleware [m/fun-mode]))