(ns roam
  (:require [datascript.core :as d]))

(->> "/home/justin/Desktop/jgood-brain.edn"
     slurp
     read-string
     (d/q '[:find ?n
            :where [?e :node/title ?n]])
     vec
     flatten)
