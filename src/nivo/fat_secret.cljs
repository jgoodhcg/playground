(ns nivo.fat-secret
  (:require [reagent.core :as r]
            [reagent.dom :as rd]
            ["@nivo/line" :as nivo-line]))

(defn view []
  [:div [:h1 "It works!"]])

(def data
  [{:id    "this"
    :color "hsl(128, 70%, 50%)"
    :data  [{:x "a" :y 1}
            {:x "b" :y 2}
            {:x "c" :y 3}
            {:x "d" :y 5}
            {:x "e" :y 8}]}

   {:id    "that"
    :color "hsl(256, 70%, 50%)"
    :data  [{:x "a" :y 2}
            {:x "b" :y 4}
            {:x "c" :y 6}
            {:x "d" :y 10}
            {:x "e" :y 16}]}])

(defn graph []
  [:div {:style {:width  "500px"
                 :height "500px"}}
   [:h3 "Here is a graph"]
   [:> nivo-line/ResponsiveLine
    {:data data}]])

(defn ^:dev/after-load start
  []
  (rd/render [graph] (.getElementById js/document "app"))  )

(defn ^:export main
  []
  (start))
