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
        (let [[current depth] (first stack)
              rest-stack      (rest stack)
              current-string  (get current ":block/string")
              current-children (get current ":block/children")
            ;; Prepare the child UIDs for pulling
              child-uids      (when current-children
                                (map (fn [c] [:block/uid (get c ":block/uid")]) current-children))
            ;; Pull the child blocks if any
              child-blocks    (when (seq child-uids)
                                (pull-many selector (pr-str child-uids)))
            ;; Prepare new stack, adding children with increased depth
            ;; Do not reverse child-blocks to maintain order
              new-stack       (concat (map (fn [b] [b (inc depth)]) child-blocks) rest-stack)
            ;; Accumulate the text with indentation
              indent          (apply str (repeat depth "  ")) ; two spaces per depth level
              new-text        (str text (when (not (str/blank? text)) "\n") indent current-string)]
          (recur new-stack new-text)))))
  ;; => "[Listed out projects](((1145XASm6))) from the [[project]]s page that are still interesting to me now \n  Noticed a real bias towards recency\n  Lots of [[game]] ideas\n    [[survey game]]\n    [[2024 language-expansion]] notes mostly include games and game stack ideas\n    [mixer game](((V6FYVsybB)))\n    [async strategy](((7cwBkIBSn))) (cooperative or competative)\n    [Remote teambuilding](((ijSctXztq)))\n    [[eco-rpg]] (dream game)\n    [[solitary-gardener]] (prequisite to eco-rpg)\n    [[potions-master]]\n    [Story generator](((BrdU5sN1V)))\n    [[covid run]]\n  Some [[Ai]] stuff\n    I was [into fine tuning](((UB4QfUgmo))) for a second -- less so now (just doesn't seem worth it)\n    These ideas of [little robots](((s-2HDWWVu))) and this [one](((yAhHay5P-))) about a [[magic]] specific theme are interesting\n    [[story telling]] [stuff](((do5I1bhrA))) even [multiplayer](((LHsRYYiBI)))\n    [[rimworld]] focused stuff comes up a lot\n      [save -> screenplay](((dJDlwkBiH)))\n      [All 4 of these ideas I mapped out earlier today](((m81YRNglM)))\n    [[dnd audio poc]] and [other session support](((B8GnN6AMJ)))\n      If I did do this it might be fun to integrate with [[Caleb Delnay]] on [[rolekit]]\n    [support interactions with kaiti](((Ay896H4Xb))) there might be something generalizable here about [[neurodivergent]] flavor difference support\n  [[art]] things\n    [Screen based game of life](((Ium6PLGF2)))\n    I've been really interested in physical book designing [since playing with](((J4f8cMv19))) [[LaTeX]] [*](((zXsUtTttd)))\n      and even since doing [[org-mode]] stuff with [[jgood.online]] and [[org-blog]]\n      [chat messages -> book](((IYOKJhWDc)))\n      [[Roam Research]] -> book\n  Lots of [[video making]]\n    [[nostalgia video project]] and [other life stuff](((2KBog2--d))) including [house](((_GznoKyxr))) [*](((LmCchaOW3)))\n  [[Grand Gardens]]\n  [[1127 Fuller Ave SE]] projects\n  [[Roam Research]] things come up sometimes\n    [Roam to org-mode](((WnlhicT-W)))\n    [Better mobile app](((7a_BUmgm3)))\n  Misc\n    [Bluesky moots graph](((Fg4nPd--3)))\n    [[moral software job]]\n    [Visualizing methane leaks](((bI9sfgVBF)))\n    [Prehistory timeline](((rpeJE7PQG)))\n  [[life chart]] stuff\n    [Github timeline](((LEi19O9Md)))\n    [Other life stuff](((eUmRtil3G)))\n    [Electronic devices](((l7rG6LIXH)))\n    \n  Started a [[chatgpt thread]] on making a [[Roam Research]] [[cli]] tool to generate a single text file given a block id for use in [[chatgpt]]\n    [Api reference](https://roamresearch.com/#/app/developer-documentation/page/mdnjFsqoA) and [clojure sdk](https://github.com/Roam-Research/backend-sdks/tree/master/clojure) -- I think I just need `pull` and `pull-many`\n    Script the assistant spit out after a few prompts\n      ```clojure\n(ns roam-export.core\n  (:require [clj-http.client :as http]\n            [cheshire.core :as json]))\n\n(def graph-name \"MY-GRAPH\") ; Replace with your graph name\n(def api-url (str \"https://api.roamresearch.com/api/graph/\" graph-name))\n(def token \"roam-graph-token-for-MY-GRAPH-1JN132hnXUYIfso22\") ; Replace with your token\n\n(def headers\n  {\"accept\" \"application/json\"\n   \"Authorization\" (str \"Bearer \" token)\n   \"Content-Type\" \"application/json\"})\n\n(defn fetch-block [uid]\n  \"Fetch a single block with its children and references.\"\n  (let [url (str api-url \"/pull\")\n        body (json/generate-string\n               {:eid (str \"[:block/uid \\\"\" uid \"\\\"]\")\n                :selector \"[:block/uid :node/title :block/string {:block/children [:block/uid :block/string]} {:block/refs [:node/title :block/string :block/uid]}]\"})]\n    (-> (http/post url {:headers headers :body body})\n        :body\n        (json/parse-string true))))\n\n(defn fetch-multiple-blocks [uids]\n  \"Fetch multiple blocks in bulk.\"\n  (let [url (str api-url \"/pull-many\")\n        eids (map #(str \"[:block/uid \\\"\" % \"\\\"]\") uids)\n        body (json/generate-string\n               {:eids (str \"[\" (clojure.string/join \", \" eids) \"]\")\n                :selector \"[:block/uid :node/title :block/string {:block/children [:block/uid :block/string]}]\"})]\n    (-> (http/post url {:headers headers :body body})\n        :body\n        (json/parse-string true))))\n\n(defn process-block-data [block]\n  \"Format block data recursively for text output.\"\n  (let [content (get block \"block/string\" \"\")\n        children (get block \"block/children\" [])]\n    (->> children\n         (map #(str \"  - \" (get % \"block/string\" \"\")))\n         (cons content)\n         (clojure.string/join \"\\n\"))))\n\n(defn main []\n  (let [main-block-uid \"08-30-2022\" ; Replace with your block UID\n        main-block (fetch-block main-block-uid)\n        main-content (process-block-data main-block)\n\n        ; Fetch linked references\n        linked-refs (map #(get % \"block/uid\") (get main-block \"block/refs\" []))\n        linked-blocks (when (seq linked-refs)\n                        (fetch-multiple-blocks linked-refs))\n        linked-content (->> linked-blocks\n                            (map process-block-data)\n                            (clojure.string/join \"\\n\"))]\n\n    ; Aggregate output\n    (spit \"roam_export.txt\"\n          (str \"Main Block:\\n\"\n               main-content \"\\n\\n\"\n               \"Linked References:\\n\"\n               linked-content))\n\n    (println \"Exported to roam_export.txt\")))\n\n(main)```\n    [[prompt]]s\n      > I've got a Roam research block that has child blocks that are organizing and summarizing links to other pages and blocks. I'd like to have something that I can plug in this block reference to and have it spit out a single text file or text that I can plug into chatgpt.\n\nThe text should include the block and all the children, but also segments for all the linked blocks  (with their children) and pages (with their direct block content).\n\nIn general, how should I approach this? Should it be a script that executes on a database export? Should it be a Roam Render script, should I use the new backend api?\n      > I think I can do this with just two types of api calls \"pull\" and \"pull many\" [full api docs for those endpoints redacted for brevity]\n      > Can you sketch this out in clojure?\n        It first wrote it in [[python]]\n    {{[[TODO]]}} Write out this tool so I can ask an llm what I should work on"

;;
  )
