(ns roam-api
  (:require [clojure.data.json :as json]
            [clj-http.client :as http]
            [secrets :refer [roam-api-token]]
            [potpuri.core :as pot]
            [clojure.string :as str]
            [clojure.pprint :refer [pprint]]))

(def selector
  [:block/uid
   :node/title
   :block/string
   {:block/children [:block/uid #_#_:block/string {:block/refs [:node/title :block/uid]}]}
   {:block/refs [:node/title #_:block/string :block/uid]}])

(def eid
  [:block/uid "ArtdSbqUV"])

(def base-url "https://api.roamresearch.com/")

(defn api [graph-name action]
  (case action
    :pull      (str base-url "api/graph/" graph-name "/pull")
    :pull-many (str base-url "api/graph/" graph-name "/pull-many")))

(defn pull [selector eid]
  (-> (http/post (api "jgood-brain" :pull)
                 {:headers           {"Authorization" (str "Bearer " roam-api-token)}
               ;; important that selector and eid are strings of edn not json
                  :body              (json/write-str {:selector (str selector)
                                                      :eid     (str eid)})
                  :content-type      :json
                  :accept            :json
               ;; technically redirects were designed to only work on GET and HEAD based on RFCs for http
               ;; but people use them for post all the time
                  :redirect-strategy :lax})
      :body
      json/read-str
      (get "result")))

(defn pull-many [selector eids]
  (-> (http/post (api "jgood-brain" :pull-many)
                 {:headers           {"Authorization" (str "Bearer " roam-api-token)}
               ;; important that selector and eid are strings of edn not json
                  :body              (json/write-str {:selector (str selector)
                                                      :eids     (str eids)})
                  :content-type      :json
                  :accept            :json
               ;; technically redirects were designed to only work on GET and HEAD based on RFCs for http
               ;; but people use them for post all the time
                  :redirect-strategy :lax})
      :body
      json/read-str
      (get "result")))

(comment
  (pull selector eid)

  (-> (pull selector eid)
      (get ":block/children")
      (->> (map (fn [c] [:block/uid (get c ":block/uid")])))
      vec
      str
      (->> (pull-many selector)))

  (let [initial-result (pull selector eid)
        first-string   (get initial-result ":block/string")
        first-children (-> initial-result
                           (get ":block/children")
                           (->> (map (fn [c] [:block/uid (get c ":block/uid")])))
                           vec
                           str
                           (->> (pull-many selector)))]
    (loop [result first-children
           text   first-string]
      (pprint (pot/map-of result text))
      (Thread/sleep 1000)
      (let [s        (->> result
                          (map (fn [b] (str (get b ":block/string"))))
                          (str/join "\n"))
            children (->> result (map (fn [b] (get b ":block/children"))) flatten)]
        (if (seq children)
          (recur (->> children
                      (remove nil?)
                      (map (fn [c] [:block/uid (get c ":block/uid")]))
                      vec
                      str
                      (pull-many selector))
                 (str text "\n" s))
          (str text " \n " s)))
      ))
;;
  )
