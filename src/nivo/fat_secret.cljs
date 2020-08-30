(ns nivo.fat-secret
  (:require [reagent.core :as r]
            [reagent.dom :as rd]))

(defn view []
  [:div [:h1 "It works!"]])

(defn ^:dev/after-load start
  []
  (rd/render [view] (.getElementById js/document "app"))  )

(defn ^:export main
  []
  (start))
