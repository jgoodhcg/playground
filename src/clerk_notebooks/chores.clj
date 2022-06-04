(ns clerk-notebooks.chores
  (:require [nextjournal.clerk :as clerk]))

(clerk/serve! {:browse? true})

(clerk/show! "/home/justin/projects/playground/src/clerk_notebooks/chores.clj")

; # Hello, Clerk 👋

(+ 39 3)
