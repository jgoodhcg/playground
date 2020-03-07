(ns aclaimant-intro
  (:require [com.rpl.specter :as sp]))

;; 4clojure.com problem 74
; Given a string of comma separated integers, write a function which returns a
; new comma separated string that only contains the numbers which are perfect
; squares.

(defn perfect-square? [n]
  (let [sqrt (Math/sqrt n)]
    (== (int sqrt) sqrt)))

(defn perfect-squares [s]
  (-> s
      (clojure.string/split #",")
      (->> (map (fn [s] (Integer/parseInt s))))
      (->> (filter perfect-square?))
      (->> (clojure.string/join ","))))

(println (= "4,9" (perfect-squares "4,5,6,7,8,9")))
(println (= "16,25,36" (perfect-squares "15,16,25,36,37")))
(println (= "16" (perfect-squares "16,27,33,47")))
