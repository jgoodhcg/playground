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

(s/def ::day-heading
  (s/cat
    :day-of-week #{"\"Monday" "\"Tuesday" "\"Wednesday" "\"Thursday" "\"Friday" "\"Saturday" "\"Sunday"}
    :month-day #(re-matches #"^\s{1}(January|Febuary|March|April|May|June|July|August|September|October|November|December){1}\s[0-9]{1,2}" %)
    :year #(re-matches #"^\s{1}[0-9]{4}\"" %)

    ;; should check on the order in the export sheet before trusting this
    :cals blank-or-float?
    :fat blank-or-float?
    :sat blank-or-float?
    :carbs blank-or-float?
    :fiber blank-or-float?
    :sugar blank-or-float?
    :prot blank-or-float?
    :sod blank-or-float?
    :chol blank-or-float?
    :potassium blank-or-float?))

(s/def ::meal-heading
  (s/cat
    :meal #{" Breakfast" " Lunch" " Dinner" " Snacks/Other"}
    ;; should check on the order in the export sheet before trusting this
    :cals blank-or-float?
    :fat blank-or-float?
    :sat blank-or-float?
    :carbs blank-or-float?
    :fiber blank-or-float?
    :sugar blank-or-float?
    :prot blank-or-float?
    :sod blank-or-float?
    :chol blank-or-float?
    :potassium (s/? blank-or-float?)))

(-> item-heading-example-a (clojure.string/split #","))
(-> item-heading-example-b (clojure.string/split #","))

(s/def ::item-heading )

(def last-meal-edited nil)

(with-open [rdr (clojure.java.io/reader "/home/justin/Desktop/Food Diary May 2019.eml")]
  (doall
    (->> rdr
         (line-seq)
         (map #(clojure.string/split % #","))
         (reduce
           (fn [data item]
             (let [maybe-day-data  (s/conform ::day-heading item)
                   maybe-meal-data (s/conform ::meal-heading item)]
               (if (not= :clojure.spec.alpha/invalid maybe-day-data)
                 (conj data (merge maybe-day-data
                                   {:meals {}}))
                 (if (not= :clojure.spec.alpha/invalid maybe-meal-data)
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
                            (fn [meals]
                              (assoc meals
                                     (merge maybe-meal-data {:items []}))))))

                   ;; TODO figure out all these values
                   (let [maybe-item-heading false ;; does it have commas and no quotes?
                         maybe-item-amount  false ;; does it have quotes and " x "?
                         maybe-meal-data    {}    ;; needs to be padded with zeros on the end
                         ]
                     (if maybe-item-heading
                       (->> data
                            (sp/transform
                              [sp/LAST :meals @last-meal-edited :items]
                              (fn [meal-items]
                                (conj meal-items maybe-meal-data))))
                       (if maybe-item-amount
                         (->> data
                              (sp/transform
                                [sp/LAST :meals @last-meal-edited :items sp/LAST]
                                (fn [meal-item]
                                  ;; just use the raw string
                                  (assoc meal-item {:amount item}))))
                         data))))
                 )))
           [])
         )))
