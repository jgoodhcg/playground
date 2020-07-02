(ns fat-secret
  (:require [clojure.spec.alpha :as s]
            [com.rpl.specter :as sp]))

(def day-heading-example "\"Saturday, June 1, 2019\",2614,112.19,56.411,199.95,24.4,35.59,114.91,2850,130,1961")

(def meal-heading-example " Breakfast,4,0.5,0,0,0,0,0,15,0,")

(def item-heading-example-a "  Stanley Brothers Charlotte's Web (Cbd Oil),4,0.5,,0,,,0,,,")

(def item-heading-example-b "  Morningstar Farms Maple Flavored Sausage Patties,240,9,0,15,3,6,24,750,0,315")

(defn blank-or-float? [v]
  (or (= v "")
      (some? (try (Float/parseFloat v)
                  (catch NumberFormatException e nil)))))

;; TODO Why can't I use `(apply s/cat [:list of :keys and :pred icates])` ???
(s/def ::day-heading
  (s/cat
    :day-of-week #{"\"Monday" "\"Tuesday" "\"Wednesday" "\"Thursday" "\"Friday" "\"Saturday" "\"Sunday"}
    :month-day #(re-matches #"^\s{1}(January|Febuary|March|April|May|June|July|August|September|October|November|December){1}\s[0-9]{1,2}" %)
    :year #(re-matches #"^\s{1}[0-9]{4}\"" %)

    ;; should check on the order in the export sheet before trusting this
    :cals (s/? blank-or-float?)
    :fat (s/? blank-or-float?)
    :sat (s/? blank-or-float?)
    :carbs (s/? blank-or-float?)
    :fiber (s/? blank-or-float?)
    :sugar (s/? blank-or-float?)
    :prot (s/? blank-or-float?)
    :sod (s/? blank-or-float?)
    :chol (s/? blank-or-float?)
    :potassium (s/? blank-or-float?)))

(s/def ::meal-heading
  (s/cat
    :meal #{" Breakfast" " Lunch" " Dinner" " Snacks/Other"}
    ;; should check on the order in the export sheet before trusting this
    :cals (s/? blank-or-float?)
    :fat (s/? blank-or-float?)
    :sat (s/? blank-or-float?)
    :carbs (s/? blank-or-float?)
    :fiber (s/? blank-or-float?)
    :sugar (s/? blank-or-float?)
    :prot (s/? blank-or-float?)
    :sod (s/? blank-or-float?)
    :chol (s/? blank-or-float?)
    :potassium (s/? blank-or-float?)))

(-> item-heading-example-a (clojure.string/split #","))

(-> item-heading-example-b (clojure.string/split #","))

(s/def ::item-heading
  (s/cat
    :description string?
    ;; should check on the order in the export sheet before trusting this
    :cals (s/? blank-or-float?)
    :fat (s/? blank-or-float?)
    :sat (s/? blank-or-float?)
    :carbs (s/? blank-or-float?)
    :fiber (s/? blank-or-float?)
    :sugar (s/? blank-or-float?)
    :prot (s/? blank-or-float?)
    :sod (s/? blank-or-float?)
    :chol (s/? blank-or-float?)
    :potassium (s/? blank-or-float?)))

(def last-meal-edited (atom nil))

(def zero-out-blank {:cals      "0"
                     :fat       "0"
                     :sat       "0"
                     :carbs     "0"
                     :fiber     "0"
                     :sugar     "0"
                     :prot      "0"
                     :sod       "0"
                     :chol      "0"
                     :potassium "0"})

(defn zero-out [x]
  (if (= :clojure.spec.alpha/invalid)
    x
    (merge zero-out-blank x)))

(def args-num (atom []))

(with-open [rdr (clojure.java.io/reader "/home/justin/Desktop/Food Diary May 2019.eml")]
  (doall
    (->> rdr
         (line-seq)
         (map #(clojure.string/split % #","))
         (reduce
           (fn [data item]
             (let [maybe-day-data  (zero-out (s/conform ::day-heading item))
                   maybe-meal-data (zero-out (s/conform ::meal-heading item))
                   maybe-item-data (zero-out (s/conform ::item-heading item))]

               ;; when it is a day
               (if (not= :clojure.spec.alpha/invalid maybe-day-data)
                 ;; build out the day and leave meals empty
                 (conj data (merge maybe-day-data
                                   {:meals {}}))
                 ;; otherwise check if it's a meal
                 (if (not= :clojure.spec.alpha/invalid maybe-meal-data)
                   ;; when it is then build out the meal data and leave items empty
                   (let [meal-keyword (-> maybe-meal-data
                                          (:meal)
                                          (clojure.string/trim)
                                          (clojure.string/lower-case)
                                          (clojure.string/replace #"/" "-")
                                          (keyword))]
                     ;; some brittle state
                     (reset! last-meal-edited meal-keyword)

                     ;; use specter to transform the last element in data to add the meals to that day
                     (->> data
                          (sp/transform
                            [sp/LAST :meals meal-keyword]
                            (fn [meal]
                              (merge maybe-meal-data {:items []})))))
                   ;; now it's either an item or an item amount
                   (let [is-item-heading (-> item
                                             (clojure.string/includes? ",")
                                             (and (not (clojure.string/includes? item "\"")))
                                             (and (not= :clojure.spec.alpha/invalid maybe-item-data)))
                         is-item-amount  (-> item
                                             (clojure.string/includes? ",")
                                             (and (not (clojure.string/includes? item "\""))))]
                     (if is-item-heading
                       (->> data
                            (sp/transform
                              [sp/LAST :meals @last-meal-edited :items]
                              (fn [meal-items]
                                (conj meal-items maybe-item-data))))
                       (if is-item-amount
                         (->> data
                              (sp/transform
                                [sp/LAST :meals @last-meal-edited :items sp/LAST]
                                (fn [meal-item]
                                  ;; just use the raw string
                                  (assoc meal-item :amount item))))
                         data)))))))
           [])
         )))
