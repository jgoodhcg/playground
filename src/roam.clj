(ns roam
  (:require [datascript.core :as d]))

(->> "/home/justin/Desktop/jgood-brain.edn"
     slurp
     read-string
     (d/q '[:find ?n
            :where [?e :node/title ?n]])
     vec
     flatten)

;; This was a query result in the form of:
;; [ ["page-title" <id-of-block-ref>] ... ]
(->> "/home/justin/Desktop/tmp-pages.edn"
     slurp
     read-string
     (map first)
     frequencies
     (map identity)
     (sort-by second)
     (reverse)
     (take 25))
