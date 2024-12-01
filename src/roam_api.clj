(ns roam-api
  (:require [com.roamresearch.sdk.backend :as api]
            [secrets :refer [roam-api-token]]
            [potpuri.core :as pot]))

(def selector
  [:block/uid
   :node/title
   :block/string
   {:block/children [:block/uid :block/string {:block/refs [:node/title :block/uid]}]}
   {:block/refs [:node/title :block/string :block/uid]}])

(def eid [:block/uid "ArtdSbqUV"])

(def result
  (api/pull {:token roam-api-token :graph "jgood-brain"}
            (str selector)
            (str eid)))

(-> result :result
    ;; ugly keyword hammer but trying to access with `:block/refs`, `::block/refs`, and even defining a ns for block doesn't work
    (->> (pot/map-keys name))
    (get "refs")
    (->> (map (fn [ref]
                (->> ref (pot/map-keys name))))))
