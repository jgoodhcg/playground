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

;; can I define a melody via position in a scale?
(defn diatonic-melody
  [positions s]
  (->> positions (mapv #(get s (-> % (- 1))))))
(!> [:piano (->> :c (scale major-intervals) (diatonic-melody [1 3 7]))])

;; let's play around with chords
(!> [:piano #{:a :b :c}])
;; how about a major triad?
(defn chord
  [positions s]
  (->> (diatonic-melody positions s) set))
(def triad [1 3 5])
(!> [:piano (->> :c (scale major-intervals) (chord triad))])

;; circle of fifths
(def major-circle-sharps [:c :g :d :a :e :b :f# :c# :g# :d# :a# :f])
(defn I
  [])
(defn IV
  [])
(defn V
  [])
