(ns music-theory
  (:require [edna.core :as edna]))

(defn !> [music] (edna/play! music))

;; chromatic scale // all the notes
(!> [:piano :c :c# :d :d# :e :f :f# :g :g# :a :a# :b])
;; same thing but using flats instead of sharps
(!> [:piano :c :d= :d :e= :e :f :g= :g :a= :a :b= :b])

;; major scale
;; sequenc of intervals between the notes
;; whole, whole, half, whole, whole, whole, half
;; in the key of c
(!> [:piano :c :d :e :f :g :a :b :+c])
;; in the key of d
(!> [:piano :d :e :f# :g :a :b :c# :+d])

;; let's make a function to play a scale!
(def chromatic-scale [:c :c# :d :d# :e :f :f# :g :g# :a :a# :b])
;; we can represent the intervals of a scale by number of semi-tones
(def major-intervals [2 2 1 2 2 2 1])
;; 🎉
(defn scale
  [intervals k]
  (let [shifted (->> chromatic-scale
                     cycle
                     (drop (.indexOf chromatic-scale k))
                     (take 12)
                     vec)
        indexes (->> intervals
                     (map-indexed
                       (fn [index interval]
                         (as-> intervals $
                           (take index $)
                           (reduce + $)
                           (+ interval $)
                           (- $ 1)))))]
    (->> indexes
         (mapv #(get shifted %)))))

;; now we can play major scale in any key
(!> [:piano (scale major-intervals :f)]) ;; apparently nesting vecs doesn't bother edna
;; and if we shuffle the notes around we get a simple diatonic melody
(!> [:piano (->> :f (scale major-intervals) shuffle (take 6))])

;; let's try minor scale
(def minor-intervals [2 1 2 2 1 2 2])
(!> [:piano (scale minor-intervals :c)])
(!> [:piano (->> :c (scale minor-intervals) shuffle (take 5))])
