(ns fat-secret
  (:require [clojure.spec.alpha :as s]))

(def day-heading-example "\"Saturday, June 1, 2019\",2614,112.19,56.411,199.95,24.4,35.59,114.91,2850,130,1961")

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

(with-open [rdr (clojure.java.io/reader "/home/justin/Desktop/Food Diary May 2019.eml")]
  (doall
    (->> rdr
         (line-seq)
         (map #(clojure.string/split % #","))
         (partition-by (fn [s] (s/valid? ::day-heading s)))
         (drop 1)
         (reduce
           (fn [data i]
             (let [item           (first i)
                   maybe-day-data (s/conform ::day-heading item)]
               (if (not= :clojure.spec.alpha/invalid maybe-day-data)
                 (conj data (merge maybe-day-data
                                   {:meals []}))
                 data
                 )))
           [])
         )))
