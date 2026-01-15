(ns daily-art.sketches.sketch-2025-10-23
  (:require
   [daily-art.core :as art]
   [quil.core      :as q]))

(defn setup
  []
  (q/frame-rate 30)
  (q/color-mode :hsb 360 100 100 1.0)

  ;; Create beautiful sunset gradient
  (let [width  800
        height 800]
    (doseq [y (range height)]
      (let [t          (/ y height)
            ;; Sunset colors: deep purple -> red -> orange -> yellow ->
            ;; light blue
            hue        (cond
                         (< t 0.2) (+ 280 (* 60 (/ t 0.2))) ; Deep purple to
                                                            ; red
                         (< t 0.4) (+ 0 (* 30 (/ (- t 0.2) 0.2))) ; Red to
                                                                  ; orange
                         (< t 0.6) (+ 30 (* 30 (/ (- t 0.4) 0.2))) ; Orange to
                                                                   ; yellow
                         (< t 0.8) (+ 60 (* 180 (/ (- t 0.6) 0.2))) ; Yellow
                                                                    ; to cyan
                         :else     (+ 240 (* 40 (/ (- t 0.8) 0.2)))) ; Cyan to
                                                                     ; deep
                                                                     ; blue
            saturation (cond
                         (< t 0.3) 80
                         (< t 0.7) 90
                         :else     70)
            brightness (cond
                         (< t 0.2) 30
                         (< t 0.5) 85
                         (< t 0.8) 95
                         :else     40)]
        (q/stroke hue saturation brightness 1.0)
        (q/line 0 y width y))))

  ;; Draw maze of black squares
  (q/no-stroke)
  (q/fill 0 0 0 1.0)  ; Black squares
  
  (let [square-size 20
        width 800
        height 800
        cols (/ width square-size)
        rows (/ height square-size)]
    (doseq [row (range rows)
            col (range cols)]
      (when (and (not= row 0) (not= row (dec rows))
                 (not= col 0) (not= col (dec cols)))
        ;; Create maze pattern with some randomness
        (when (or (and (= (mod row 2) 0) (< (rand) 0.7))
                  (and (= (mod col 2) 1) (< (rand) 0.3)))
          (q/rect (* col square-size) (* row square-size) square-size square-size)))))

  {})

(comment
  (art/run-sketch
   {:title "Blank Canvas",
    :size  [800 800],
    :setup setup})
  ;;
  )
