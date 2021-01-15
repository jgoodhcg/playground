(ns crux-intro
  (:require [crux.api :as crux]))

(def node (crux/start-node {}))

(def manifest
  {:crux.db/id  :manifest
   :pilot-name  "Johanna"
   :id/rocket   "SB002-sol"
   :id/employee "22910x2"
   :badges      "SETUP"
   :cargo       ["stereo" "gold fish" "slippers" "secret note"]})

(crux/submit-tx node [[:crux.tx/put manifest]])

;; TODO continue from here https://juxt.pro/blog/crux-tutorial-put
