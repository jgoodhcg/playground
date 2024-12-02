(ns roam-api
  (:require [clojure.data.json :as json]
            [clj-http.client :as http]
            [secrets :refer [roam-api-token]]
            [potpuri.core :as pot]
            [clojure.string :as str]
            [clojure.pprint :refer [pprint]]
            [tick.core :as t]))

(def selector
  [:block/uid
   :node/title
   :block/string
   #_:bock/order
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
          (str text " \n " s)))))

;; chatgpt take the wheel v1
  (let [initial-result (pull selector eid)]
    (loop [stack [initial-result]
           text  ""]
      (if (empty? stack)
        text
        (let [current          (first stack)
              rest-stack       (rest stack)
              current-string   (get current ":block/string")
              current-children (get current ":block/children")
              ;; Prepare the child UIDs for pulling
              child-uids       (when current-children
                                 (map (fn [c] [:block/uid (get c ":block/uid")]) current-children))
              ;; Pull the child blocks if any
              child-blocks     (when (seq child-uids)
                                 (pull-many selector (pr-str child-uids)))
              ;; Reverse the child blocks to maintain order
              new-stack        (concat (reverse child-blocks) rest-stack)
              ;; Accumulate the text
              new-text         (str text (when (not (str/blank? text)) "\n") current-string)]
          (recur new-stack new-text)))))

  ;; chatgpt take the wheel v2
  (let [initial-result (pull selector eid)]
    (loop [stack [[initial-result 0]] ; stack of tuples [block, depth]
           text  ""]
      (if (empty? stack)
        text
        (let [[current depth]  (first stack)
              rest-stack       (rest stack)
              current-string   (get current ":block/string")
              current-children (get current ":block/children")
              ;; Prepare the child UIDs for pulling
              child-uids       (when current-children
                                 (map (fn [c] [:block/uid (get c ":block/uid")]) current-children))
              ;; Pull the child blocks if any
              child-blocks     (when (seq child-uids)
                                 (pull-many selector (pr-str child-uids)))
              ;; Prepare new stack, adding children with increased depth
              ;; Do not reverse child-blocks to maintain order
              new-stack        (concat (map (fn [b] [b (inc depth)]) child-blocks) rest-stack)
              ;; Accumulate the text with indentation
              indent           (apply str (repeat depth "  -")) ; two spaces per depth level
              new-text         (str text (when (not (str/blank? text)) "\n") indent current-string)]
          (recur new-stack new-text)))))

;; getting real deep
  (let [initial-result (pull selector eid)
        start (t/now)]
    (loop [stack [[initial-result 0]]
           text  ""
           status {:n 0 :t start}]
      (println (str "loop: " (:n status)
                    ", minutes so far: " (-> (t/between start (:t status)) (t/minutes))))
      (Thread/sleep 2500) ;; gotta stay below 50 reqs per min
      (if (empty? stack)
        text
        (let [[current depth]  (first stack)
              rest-stack       (rest stack)
              current-string   (or (get current ":block/string") (get current ":node/title"))
              current-children (get current ":block/children")
              child-uids       (when current-children
                                 (->> current-children (map (fn [c] [:block/uid (get c ":block/uid")]))))
              child-blocks     (when (seq child-uids)
                                 ;; request!
                                 (pull-many selector (pr-str child-uids)))
              refs-uids        (get current ":block/refs")
              page-refs        (->> refs-uids
                                    (map (fn [r] [:node/title (get r ":node/title")]))
                                    (remove (fn [e] (-> e second nil?))))
              block-refs       (->> refs-uids
                                    (map (fn [r] [:block/uid (get r ":block/uid")]))
                                    (remove (fn [e] (-> e second nil?))))
              refs             (->> (concat page-refs block-refs)
                                    vec
                                    ;; request!
                                    (pull-many selector))
              new-stack        (concat
                                ;; children blocks go first for depth first traversal
                                (->> child-blocks (map (fn [b] [b (inc depth)])))
                                ;; then the rest of the children blocks
                                rest-stack
                                ;; then put the refs at the end so that they get processed last
                                ;; start at 0 depth as if they are footnotes
                                (->> refs (map (fn [r] [r 0]))))
              indent           (apply str (repeat depth "  "))
              new-text         (str text (when (not (str/blank? text)) "\n") indent current-string)]
          (recur new-stack new-text {:n (inc (:n status)) :t (t/now)})))))

;;
  )

;; for roam/cljs
;; decided that api was a better place to run this code given api rate limits
;; and how I intend to use it
;; the api was also similar enough to port most of the code
(comment
  (ns roam.cljs.test.b
    (:require
     [roam.datascript :refer [pull]]
     [clojure.pprint :refer [pprint]]
     [clojure.string :refer [blank?]]
     [roam.ui.command-palette :refer [add-command]]))

  (defn get-text []
    (pprint
     (let [selector       [:block/uid
                           :node/title
                           :block/string
                           {:block/children [:block/uid]}
                           {:block/refs [:node/title :block/uid]}]
           initial-result (pull selector [:block/uid "ArtdSbqUV"])
           visited-uids   (atom #{})] ; Keep track of visited UIDs
       (loop [stack  [[initial-result 0]]
              text   ""
              status {:n 0}]
         (pprint {:status status :stack (count stack)})
         (if (empty? stack)
           text
           (let [[current depth]  (first stack)
                 rest-stack       (rest stack)
                 current-uid      (or (get current :block/uid) (get current :node/title))
                 _                (when current-uid
                                    (swap! visited-uids conj current-uid))
                 current-string   (or (get current :block/string) (get current :node/title))
                 current-children (get current :block/children)
                 child-uids       (when current-children
                                    (->> current-children
                                         (mapv #(get % :block/uid))
                                         (remove nil?)
                                         ;; Exclude visited UIDs
                                         (remove @visited-uids)))
                 child-blocks     (when (seq child-uids)
                                    (->> child-uids
                                         (mapv #(pull selector [:block/uid %]))))
                 refs-uids        (get current :block/refs)
                 refs-uids        (when refs-uids
                                    (->> refs-uids
                                         (map #(or (get % :block/uid) (get % :node/title)))
                                         (remove nil?)
                                         ;; Exclude visited UIDs
                                         (remove @visited-uids)))
                 refs             (when (seq refs-uids)
                                    (->> refs-uids
                                         (mapv (fn [uid]
                                                 (if (string? uid)
                                                   ;; It's a block UID
                                                   (pull selector [:block/uid uid])
                                                   ;; It's a page title
                                                   (pull selector [:node/title uid]))))))
                 _                (when refs-uids
                                    (swap! visited-uids into refs-uids))
                 new-stack        (concat
                                   ;; Children blocks go first for depth-first traversal
                                   (mapv #(vector % (inc depth)) child-blocks)
                                   rest-stack
                                   ;; Refs are processed last
                                   (mapv #(vector % 0) refs))
                 indent           (apply str (repeat depth "  "))
                 new-text         (str text (when (not (blank? text)) "\n") indent current-string)]
             (recur new-stack new-text (update status :n inc))))))))

  ;; cmd + p
  (add-command {:label "test-gen-ai-text" :callback get-text})

  ;;
  )
