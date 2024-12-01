(ns roam-api
  (:require [com.roamresearch.sdk.backend :as api]
            [secrets :refer [roam-api-token]]
            [potpuri.core :as pot]
            [clojure.string :as str]
            [clojure.walk :refer [postwalk]]))

(def selector
  [:block/uid
   :node/title
   :block/string
   {:block/children [:block/uid :block/string {:block/refs [:node/title :block/uid]}]}
   {:block/refs [:node/title :block/string :block/uid]}])

(def eid [:block/uid "ArtdSbqUV"])

(def result
  (:result (api/pull {:token roam-api-token :graph "jgood-brain"}
                     (str selector)
                     (str eid))))

(defn fix-keys
  [m]
  ;; Results coming out of the sdk function have fully namespaced keys.
  ;; The way I see them at the repl is with shorthand notation like `::block/refs`.
  ;; When I try to access them with something like `(-> result :result ::block/refs)`
  ;; it fails because `blocks` is not defined
  ;; If I create that namespace it doesn't match.
  ;; If I try `(-> result :result :block/refs)` it also fails because that doesn't match.
  ;; The below fixes that so the keys match the selectors -- `:block/refs`
  (postwalk (fn [k?]
              (if (keyword? k?)
                (keyword (->> k? namespace rest (str/join ""))
                         (->> k? name))
                k?)) m))

(comment
  (-> result
      fix-keys
      :block/refs
      )

  ;;
  )
