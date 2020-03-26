(ns workout-counter
  (:require [spec-tools.data-spec :as ds]
            [clojure.spec.alpha :as s]
            [com.rpl.specter :as sp]))

(def sample-data
  [{:start     [2020 3 1 15 16]
    :stop      [2020 3 1 15 31]
    :gym       false
    :exercises {:plank                  [{:s 1 :time {:min 1 :sec 0}}]
                :wall-sit               [{:s 1 :time {:min 1 :sec 0}}]
                :thoracic-extension-bar [{:s 1 :r 5 :each-side true}]
                :prone-bar-raise        [{:s 2 :r 10}]
                :bridge-banded          [{:s 1 :r 20}]
                :adductor-leg-lift      [{:s 1 :r 20 :each-side true}]
                :reverse-hyperextension [{:s 1 :r 15}]
                :hamstring-stretch      [{:s 1 :r 10 :each-side true}]}}

   {:start     [2020 3 4 18 11]
    :stop      [2020 3 4 19 04]
    :gym       false
    :exercises {:plank                  [{:s 1 :time {:min 1 :sec 0}}]
                :wall-sit               [{:s 1 :time {:min 1 :sec 0}}]
                :thoracic-extension-bar [{:s 1 :r 5 :each-side true}]
                :prone-bar-raise        [{:s 2 :r 10}]
                :bridge-banded          [{:s 1 :r 20}]
                :reverse-hyperextension [{:s 1 :r 15}]
                :hamstring-stretch      [{:s 1 :r 10 :each-side true}]
                :pullup                 [{:s 5 :r 5}]
                :pushup                 [{:s 1 :r 20}]
                :curl-wall-dumbell      [{:s 1 :r 10 :w 20}]}}

   {:start     [2020 3 6 19 31]
    :stop      [2020 3 6 19 59]
    :gym       false
    :exercises {:plank                  [{:s 1 :time {:min 1 :sec 0}}]
                :wall-sit               [{:s 1 :time {:min 1 :sec 0}}]
                :thoracic-extension-bar [{:s 1 :r 5 :each-side true}]
                :prone-bar-raise        [{:s 2 :r 10}]
                :bridge-banded          [{:s 1 :r 25}]
                :reverse-hyperextension [{:s 1 :r 15}]
                :hamstring-stretch      [{:s 1 :r 10 :each-side true}]
                :pullup                 [{:s 2 :r 5}
                                         {:s 1 :r 10}]
                :pushup                 [{:s 1 :r 20}]
                :curl-wall-dumbell      [{:s 1 :r 10 :w 20}]}}])

;; TODO turn this into it's own spec so it is easier to read spec validation fail output
(defn sets-reps? [exercises]
  (->> exercises
       (sp/select [sp/MAP-VALS])
       (every? (fn [srws]
                 (->> srws
                      (every? (fn [srw]
                                (if (map? srw)
                                  (and
                                   (contains? srw :s)
                                   (or (contains? srw :time)
                                       (contains? srw :steps)
                                       (contains? srw :r)))
                                  (keyword? srw)))))))))

(defn date-int-coll? [c]
  (and (= 5 (count c))
       (every? int? c)
       (-> (first c) (> 2000))
       (-> (nth c 1) (> 0))
       (-> (nth c 2) (> 0))
       (-> (nth c 1) (< 13))
       (-> (nth c 2) (< 32))))

(def workouts-spec-data [{:start        date-int-coll?
                          :stop         date-int-coll?
                          (ds/opt :gym) boolean?
                          :exercises    #(and (map? %)
                                              (sets-reps? %))}])

(def workouts-spec (ds/spec {:spec workouts-spec-data
                             :name ::workouts}))

(def exercises-to-count #{:pullup :pushup :squat})

(defn filter-fn [entry]
  (->> entry
       :start
       ((fn [c]
          (and (-> c first (> 2019))
               (-> c (nth 1) (> 2)))))))

(defn process [workout-edn-str]
  (let [data                    (clojure.edn/read-string workout-edn-str)
        spec-fail-explanation (s/explain-str workouts-spec data)]
    (if (not (= "Success!\n" spec-fail-explanation))
      (println spec-fail-explanation)
      (println
       (->> data
            ;; since march 2020
            (filter filter-fn)
            ;; all the exercises
            (sp/select [sp/ALL :exercises sp/ALL
                        (fn [exercise]
                          (some? (some exercises-to-count
                                       exercise)))])
            ;; group by exercise
            (group-by first)

            ;; do some cleanup magic
            ;; calculate total reps for each exercises-to-count
            (sp/transform [sp/MAP-VALS sp/ALL] rest)
            (sp/transform [sp/MAP-VALS]
                          (fn [srws]
                            (->> srws
                                 flatten
                                 (map (fn [{:keys [s r]}]
                                        (* s r)))
                                 (reduce +))))
            str)))))

;; Invoke with
;; clj -m workout-counter "$(cat ~/Nextcloud/gsd/workouts.edn)"
(defn -main [file-path]
  (->> file-path
       (slurp)
       (process)))
