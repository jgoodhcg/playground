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

(defn sets-reps? [exercises]
  (->> exercises
       (sp/select [sp/MAP-VALS])
       (every? (fn [srws]
                 (->> srws
                      (every? (fn [srw] (and (contains? srw :s)
                                             (or (contains? srw :time)
                                                 (contains? srw :r))))))))))

(def workouts-spec-data [{:start [int?]
                          :stop [int?]
                          :gym boolean?
                          :exercises #(and (map? %)
                                           (sets-reps? %))}])

(def workouts-spec (ds/spec {:spec workouts-spec-data
                             :name ::workouts}))

;; (s/valid? workouts-spec sample-data)
;; (s/explain-data workouts-spec sample-data)

;; (->> data
;;      (sp/select [sp/ALL :exercises ]))

;; (defn -main [workout-edn-str]
;;   (let [data                  (clojure.edn/read-string workout-edn-str)
;;         spec-fail-explanation (s/explain-str workouts-spec data)]
;;     (if (some? spec-fail-explanation)
;;       (println spec-fail-explanation)
;;       (->> data
;;            (sp/select [sp/ALL :exercises ])))
;;     ))

