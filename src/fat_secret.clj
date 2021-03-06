(ns fat-secret
  (:require [clojure.spec.alpha :as s]
            [com.rpl.specter :as sp]
            [oz.core :as oz]
            [debux.core :refer :all]
            [cuerdas.core :as str]))

(oz/start-server!)

(def files (->> "/home/justin/Nextcloud/projects/quantification-data/raw-data/fat-secret/"
                (clojure.java.io/file)
                (file-seq)
                (filter #(.isFile %))
                (map #(-> %
                          (.toPath)
                          (.toString)))
                (filter #(str/includes? % "eml"))))

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
    :potassium (s/? blank-or-float?)
    ))

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

(def zero-fill-blank {:cals      "0"
                      :fat       "0"
                      :sat       "0"
                      :carbs     "0"
                      :fiber     "0"
                      :sugar     "0"
                      :prot      "0"
                      :sod       "0"
                      :chol      "0"
                      :potassium "0"})

(defn valid-conform? [x]
  (not= :clojure.spec.alpha/invalid x))

(defn zero-fill [x]
  (if (valid-conform? x)
    (merge zero-fill-blank x)
    x))

(defn load-lines [file]
  (with-open [rdr (clojure.java.io/reader file)]
    (into [] (line-seq rdr))))

(defn transform-lines [lines]
  (->> lines
       (reduce
         (fn [data line]
           (let [item            (clojure.string/split line #",")
                 maybe-day-data  (->> item (s/conform ::day-heading) (zero-fill))
                 maybe-meal-data (->> item (s/conform ::meal-heading) (zero-fill))
                 maybe-item-data (->> item (s/conform ::item-heading) (zero-fill))]

             ;; when it is a day
             (if (valid-conform? maybe-day-data)
               ;; build out the day, clean date fields, and leave meals empty
               (let [{:keys [year month-day]} maybe-day-data
                     clean-year               (-> year
                                                  (str/trim "\"")
                                                  (str/trim))
                     clean-month-day          (-> month-day
                                                  (str/trim))]
                 (conj data (merge maybe-day-data
                                   {:meals     {}
                                    :year      clean-year
                                    :month-day clean-month-day
                                    :date      (str/istr "~{clean-month-day}, ~{clean-year}")})))

               ;; otherwise check if it's a meal
               (if (valid-conform? maybe-meal-data)
                 ;; when it is then build out the meal data and leave items empty
                 (let [meal-keyword (-> maybe-meal-data
                                        (:meal)
                                        (clojure.string/trim)
                                        (clojure.string/lower-case)
                                        (clojure.string/replace #"/" "-")
                                        (keyword))]

                   ;; some brittle state
                   ;; saves the the meal keyword, :breakfast :lunch ... , for the next line processing
                   ;; the next line will either be a meal item that should go into the saved meal or a new meal
                   (reset! last-meal-edited meal-keyword)

                   ;; use specter to transform the last day in data to add the meal
                   (->> data
                        (sp/transform
                          [sp/LAST :meals meal-keyword]
                          (fn [_]
                            (merge maybe-meal-data {:items []})))))

                 ;; if the line has fallen through this far then
                 ;; it's either an item or an item amount
                 ;; we can do some loose counting that should be write most of the time
                 (let [is-item-heading   (-> item
                                             (count)
                                             (> 2))
                       is-item-amount    (-> item
                                             (count)
                                             (<= 2))
                       prev-meal-keyword @last-meal-edited]

                   (if (and is-item-heading
                            (valid-conform? maybe-item-data))
                     ;; when it is a meal-item then add it to the last day's last meal's :items list
                     ;; there are some lines that might be misinterpreted by this loose conform
                     ;; those are all in the beginning of the document before a valid day
                     ;; data should be empty until a valid day is reached
                     ;; sp/transform won't add anything until all the levels above :items are valid
                     (->> data
                          (sp/transform
                            [sp/LAST :meals prev-meal-keyword :items]
                            (fn [meal-items]
                              (conj meal-items maybe-item-data))))
                     ;; when it is an item amount then add the :amount key to the last item from the last meal edited on the last day in the list
                     ;; there are some lines that might be misinterpreted by this loose conform
                     ;; those are all in the beginning of the document before a valid day
                     ;; data should be empty until a valid day is reached
                     ;; sp/transform won't add anything until all the levels above :items LAST are valid
                     (if is-item-amount
                       (->> data
                            (sp/transform
                              [sp/LAST :meals prev-meal-keyword :items sp/LAST]
                              (fn [meal-item]
                                ;; just use the raw item
                                (merge meal-item {:amount item}))))
                       ;; if it isn't a day, meal, meal-item, or meal-item-amount then it is we shouldn't do anything with it
                       (do
                         (println "Following line was not a day, meal, item, or amount")
                         (println item)
                         (println)
                         data))))))))
         [])))

;; Performance for (def data ... )
;; "Elapsed time: 230266.066506 msecs" - original implementation     - 71c2a11172551eb4fdc787f10863ae2428e4deb1
;; "Elapsed time: 230696.127188 msecs" - throw some tranducing at it - 85d8175c8407cda089066b439014d01e3c2f50b6
;; "Elapsed time: 233625.543827 msecs" - move reduce to the end      - 44814f23fcb1b3fd707bb2b530c1cdc2f0e15e4e
;; "Elapsed time: 255480.378156 msecs" - rearrange some stuff        - da0e92b44d858f92ad293c1554f8c728a64aa4c2
;; "Elapsed time: 215348.098774 msecs"
;; "Elapsed time: 212417.819437 msecs"
;; "Elapsed time: 214530.844913 msecs" - transform all at the end    - 43df53a3c56276864e9535d6d138d1881924e373
(def data
  (time (->> files
             (into [] (map load-lines))
             (flatten)
             (transform-lines))))

(def calories-line-plot
  {:data  {:values data}
   :width 1500
   :layer [{:mark     {:type "boxplot" :extent "min-max"}
            :encoding {:x {:field    "date"
                           :type     "temporal"
                           :timeUnit "yearmonth"
                           :axis     {:labelExpr "[timeFormat(datum.value, '%b'), timeFormat(datum.value, '%m') == '01' ? timeFormat(datum.value, '%Y') : '']"}}
                       :y {:field "cals"
                           :scale {:zero false}
                           :type  "quantitative"}}}]})

(def macro-line-plot
  {:data   {:values data}
   :repeat {:layer ["prot" "fat" "carbs" "sugar"]}
   :spec   {:width    1500
            :mark     "line"
            :encoding {:x     {:field    "date"
                               :type     "temporal"
                               :timeUnit "yearmonth"
                               :axis     {:labelExpr "[timeFormat(datum.value, '%b'), timeFormat(datum.value, '%m') == '01' ? timeFormat(datum.value, '%Y') : '']"}}
                       :y     {:field {:repeat "layer" } :type "quantitative" :aggregate "mean"}
                       :color {:datum {:repeat "layer" :type "nominal"}}}}})

(def viz
  [:div
   [:h1 "Fat Secret Data"]
   [:div
    [:h3 "Calories"]
    [:vega-lite calories-line-plot]]
   [:div
    [:h3 "Macros"]
    [:vega-lite macro-line-plot]]])

;; Render the plot
(oz/view! viz)
