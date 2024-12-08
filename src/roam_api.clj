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
  (ns roam.ai.gen.retrieval-script.v1
    (:require
     [roam.datascript :refer [pull]]
     [clojure.pprint :refer [pprint]]
     [clojure.string :refer [blank?]]
     [roam.ui.command-palette :refer [add-command remove-command]]))

  (defn get-text []
    (pprint
     (let [page-selector        [:block/uid
                                 :node/title
                                 :block/string
                                 ;; no refs for pages (makes result too large)
                                 {:block/children [:block/uid]}]
           child-block-selector [:block/uid
                                 :block/string
                                 {:block/children [:block/uid]}
                                 {:block/refs [:node/title :block/uid]}]
           ref-block-selector   [:block/uid
                                 :block/string
                                 {:block/children [:block/uid]}]
           initial-result       (pull child-block-selector [:block/uid "ArtdSbqUV"])
           visited-uids         (atom #{})] ; Keep track of visited UIDs
       (loop [stack  [[initial-result 0]]
              text   ""
              status {:n 0}]
         (pprint {:status status :stack (count stack)})
         (if (empty? stack)
           text
           (let [[current depth] (first stack)
                 rest-stack      (rest stack)
                 refs-ids        (get current :block/refs)
                 block-ref-uids  (when refs-ids
                                   (->> refs-ids
                                        (map #(get % :block/uid))
                                        (remove nil?)
                                        (remove @visited-uids)))
                 node-ref-titles (when refs-ids
                                   (->> refs-ids
                                        (map #(get % :node/title))
                                        (remove nil?)
                                        (remove @visited-uids)))
                 ignore          (->> node-ref-titles
                                      (some #{"sensitive"
                                              "my personal journal"
                                              "health"
                                              "dream"
                                              "Archemedx"})
                                      some?)]
             (if ignore
               ;; Don't utilize any blocks referencing ignore titles
               (recur rest-stack text (update status :n inc))

               ;; Otherwise continue processing
               (let [current-uid      (or (get current :block/uid) (get current :node/title))
                     _                (when current-uid (swap! visited-uids conj current-uid))
                     current-string   (or (get current :block/string) (get current :node/title))
                     current-children (get current :block/children)
                     child-uids       (when current-children
                                        (->> current-children
                                             (mapv #(get % :block/uid))
                                             (remove nil?)
                                             (remove @visited-uids)))
                     child-blocks     (when (seq child-uids)
                                        (->> child-uids
                                             (mapv #(pull child-block-selector [:block/uid %]))))
                     refs             (concat
                                       (->> block-ref-uids (mapv (fn [uid] (pull ref-block-selector [:block/uid uid]))))
                                       (->> node-ref-titles (mapv (fn [title] (pull page-selector [:node/title title])))))
                     _                (when node-ref-titles
                                        (swap! visited-uids into node-ref-titles))
                     _                (when block-ref-uids
                                        (swap! visited-uids into block-ref-uids))
                     new-stack        (concat
                                       ;; Children blocks go first for depth-first traversal
                                       (mapv #(vector % (inc depth)) child-blocks)
                                       rest-stack
                                       ;; Refs are processed last
                                       (mapv #(vector % 0) refs))
                     indent           (apply str (repeat depth "  "))
                     new-text         (str text (when (not (blank? text)) "\n") indent current-string)]
                 (recur new-stack new-text (update status :n inc))))))))))

  ;; cmd + p
  (remove-command {:label "roam-rag"})
  (add-command {:label "roam-rag" :callback get-text})

  ;;
  )

;; examples of '[*]
(comment
  {:create/user {:db/id 1},
   :block/string
   "[Listed out projects](((1145XASm6))) from the [[project]]s page that are still interesting to me now ",
   :create/time 1732926681014,
   :block/refs [{:db/id 3064} {:db/id 73536}],
   :edit/user {:db/id 1},
   :block/children
   [{:db/id 73556}
    {:db/id 73558}
    {:db/id 73561}
    {:db/id 73565}
    {:db/id 73567}
    {:db/id 73582}
    {:db/id 73583}
    {:db/id 73584}
    {:db/id 73589}
    {:db/id 73595}
    {:db/id 73601}],
   :block/uid "ArtdSbqUV",
   :block/open true,
   :edit/time 1733025209531,
   :db/id 73555,
   :block/parents [{:db/id 73519}],
   :block/order 4,
   :block/page {:db/id 73519}}

  {:create/user {:db/id 1},
   :edit/seen-by [{:db/id 1}],
   :attrs/lookup
   [{:db/id 14110} {:db/id 17435} {:db/id 17831} {:db/id 69506}],
   :create/time 1608135704738,
   :node/title "me",
   :edit/user {:db/id 1},
   :block/children [{:db/id 24312} {:db/id 69497} {:db/id 69506}],
   :block/uid "mhuTDvCSs",
   :edit/time 1608135704740,
   :entity/attrs
   #{[{:source [:block/uid "mhuTDvCSs"], :value [:block/uid "mhuTDvCSs"]}
      {:source [:block/uid "P8O5_F80K"], :value [:block/uid "Sg-1qpRJf"]}
      {:source [:block/uid "P8O5_F80K"],
       :value [:block/uid "1Y7s2FXCI"]}]},
   :db/id 14110})

;; block context menu
(comment

(ns roam.ai.gen.retrieval-script.v2
    (:require
     [roam.datascript :refer [pull]]
     [clojure.pprint :refer [pprint]]
     [clojure.string :refer [blank?]]
     [roam.ui.block-context-menu :refer [add-command remove-command]]))

  (defn get-text [block-uid]
    (println (str "Getting text for: " block-uid))
    (pprint
     (let [text-limit           10000 ;; characters
           page-selector        [:block/uid
                                 :node/title
                                 :block/string
                                 ;; no refs for pages (makes result too large)
                                 {:block/children [:block/uid]}]
           child-block-selector [:block/uid
                                 :block/string
                                 {:block/children [:block/uid]}
                                 {:block/refs [:node/title :block/uid]}]
           ref-block-selector   [:block/uid
                                 :block/string
                                 {:block/children [:block/uid]}]
           initial-result       (pull child-block-selector [:block/uid block-uid])
           visited-uids         (atom #{})] ; Keep track of visited UIDs
       (loop [stack  [[initial-result 0]]
              text   ""
              status {:n 0}]
         (pprint {:status status :stack (count stack)})
         (if (empty? stack)
           text
           (let [[current depth] (first stack)
                 rest-stack      (rest stack)
                 refs-ids        (get current :block/refs)
                 block-ref-uids  (when refs-ids
                                   (->> refs-ids
                                        (map #(get % :block/uid))
                                        (remove nil?)
                                        (remove @visited-uids)))
                 node-ref-titles (when refs-ids
                                   (->> refs-ids
                                        (map #(get % :node/title))
                                        (remove nil?)
                                        (remove @visited-uids)))
                 ignore          (->> node-ref-titles
                                      (some #{"sensitive"
                                              "my personal journal"
                                              "health"
                                              "dream"
                                              "Archemedx"})
                                      some?)]
             (if ignore
               ;; Don't utilize any blocks referencing ignore titles
               (recur rest-stack text (update status :n inc))

               ;; Otherwise continue processing
               (let [current-uid      (or (get current :block/uid) (get current :node/title))
                     _                (when current-uid (swap! visited-uids conj current-uid))
                     current-string   (or (get current :block/string) (get current :node/title))
                     current-children (get current :block/children)
                     child-uids       (when current-children
                                        (->> current-children
                                             (mapv #(get % :block/uid))
                                             (remove nil?)
                                             (remove @visited-uids)))
                     child-blocks     (when (seq child-uids)
                                        (->> child-uids
                                             (mapv #(pull child-block-selector [:block/uid %]))))
                     refs             (concat
                                       (->> block-ref-uids (mapv (fn [uid] (pull ref-block-selector [:block/uid uid]))))
                                       (->> node-ref-titles (mapv (fn [title] (pull page-selector [:node/title title])))))
                     _                (when node-ref-titles
                                        (swap! visited-uids into node-ref-titles))
                     _                (when block-ref-uids
                                        (swap! visited-uids into block-ref-uids))
                     new-stack        (concat
                                       ;; Children blocks go first for depth-first traversal
                                       (mapv #(vector % (inc depth)) child-blocks)
                                       rest-stack
                                       ;; Refs are processed last
                                       (mapv #(vector % 0) refs))
                     indent           (apply str (repeat depth "  "))
                     new-text         (str text (when (not (blank? text)) "\n") indent current-string)]
                 (if (-> new-text count (> text-limit))
                   text
                   (recur new-stack new-text (update status :n inc)))))))))))

  ;; right click > plugins >
  (remove-command {:label "Gen LLM context"})
  (add-command {:label    "Gen LLM context"
                :callback (fn [{:as e}]
                            (pprint {:block-context e})
                            (get-text (. e -block-uid)))})

  ;;
  )
