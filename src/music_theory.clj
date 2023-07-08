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

;; chatgpt writes edna
(def my-chords
  [:piano
   #{:c :e :g}
   #{:f :a :c}
   #{:g :b :d}
   #{:c :e :g}])

(!> my-chords)

(def my-chords-2
  [:piano
   #{:c :e :g}
   #{:f :a :c}
   #{:d :f :a}
   #{:g :b :d}
   #{:c :e :g}
   #{:b :d :f}
   #{:g :b :d}
   #{:c :e :g}])

(!> my-chords-2)

(def my-chords-2-alt
  [:piano
   #{:c :e :g}
   #{:f :a :c}
   #{:g :b :d :f}
   #{:c :e :g :b}
   #{:f :a :c :e}
   #{:g :b :d :f :a}])

(!> my-chords-2-alt)

(def my-epic-chords
  [:piano
   #{:c :e :g}
   #{:f :a :c}
   #{:g :b :d :f}
   #{:c :e :g :b}
   #{:d :f :a :c}
   #{:g :b :d :f :a}
   #{:f :a :c :e :g}
   #{:c :e :g :b :d}
   #{:f :a :c :e :g :b}])

(!> my-epic-chords)

;; /chatgpt

;;
;; Using chatgpt to understand music theory
;;

;; Uplifting chord progression
;; C major (I): C, E, G
;; G major (V): G, B, D
;; A minor (vi): A, C, E

(def uplifting-chords [#{:c :e :g} #{:g :b :d} #{:a :c :e}])

(!> [:piano uplifting-chords])
(!> [:piano (->> uplifting-chords repeat (take 4))])
(!> [:piano 1/3 uplifting-chords :r uplifting-chords])

;; Melody to play over the chords
;; C major (I): C, D, E, F, G
;; G major (V): G, A, B, C, D
;; A minor (vi): A, B, C, D, E

(!> [:piano #{:c :e :g} :c :d :f :g #{:g :b :d} :g :a :b :c :d #{:a :c :e} :a :b :c :d :e])

(def c-major-melody-notes [:c :d :e :f :g])
(def g-major-melody-notes [:g :a :b :c :d])
(def a-minor-melody-notes [:a :b :c :d :e])

(!> [:piano (-> uplifting-chords (interleave [(shuffle c-major-melody-notes)
                                  (shuffle g-major-melody-notes)
                                  (shuffle a-minor-melody-notes)]))])

(!> [:piano
     #{:c :e :g} :c :g
     #{:g :b :d} :g :a
     #{:a :c :e} :a :d])

(!> [:piano
     1/2 #{:c :e :g} 1/8 :c :g :f
     1/2 #{:g :b :d} 1/8 :g :a :d
     1/2 #{:a :c :e} 1/8 :c :a :b
     1/2 #{:c :e :g} 1/8 :c :g :f
     1/3 #{:g :b :d}
     1/3 #{:c :e :g}
     1/2 #{:a :c :e}
     1/2 #{:c :e :g}
     ])

;; Simple Alda melody
;; tempo: 140

;; chords: |c e g|g b d|a c e|

;; melody:
;;   - c e g a b c
;;   - c e g b c d
;;   - g b c d e g
;;   - g b c e g a
;;   - c e g a b c

(!> #{[:piano 1/4 (->> uplifting-chords repeat (take 4) (interpose :r))]
      [:violin 1/8
       [:c :e :g :a :b :c]
       [:c :e :g :b :c :d]
       [:g :b :c :d :e :g]
       [:g :b :c :e :g :a]
       [:c :e :g :a :b :c]
       ]})

;; Simple sad Alda melody
;; tempo: 60

;; chords: |c e g|f a c|d f a|

;; melody:
;;   - c d e f g a b
;;   - c b a g f e d
;;   - d e f g a b c
;;   - b a g f e d c

(!> #{[:piano {:tempo 60}
       (->> [#{:c :e :g} #{:f :a :c} #{:d :f :a}]
            repeat (take 2))
       (->> [#{:f :a :c} #{:d :f :a}])]
      [:violin {:tempo 60}
       [:c :d :e :f :d :c :b :f]]})

(!> [:violin {:tempo 60}
       [:c :d :e :f :d :b :f :e :d :c]])

;; /chatgpt

(!> [:percussion :a :b :c])


;; Given a chord progression like [:V :I :vi :IV], and a key like :c returns a vector of chords
(defn chord-progression [key progression]
    (map #(chord key %) progression))
