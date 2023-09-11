(ns clerk-notebooks.transactions-2023-09-11
  (:require [nextjournal.clerk :as clerk]
            [clojure.data.csv :as csv]
            [clojure.string :as str]))

(comment
  (clerk/serve! {:browse? true})
  (clerk/show! "/home/justin/projects/playground/src/clerk_notebooks/transactions_2023_09_11.clj")
  )

; # Hello, Clerk 👋
(defn csv-data->maps [csv-data]
  (map zipmap
       (->> (first csv-data) ;; First row is the header
            #_(map keyword) ;; Drop if you want string keys instead
            repeat)
       (rest csv-data)))

(def records
  (-> "src/clerk_notebooks/transactions_2023_09_11.csv"
      slurp
      csv/read-csv
      csv-data->maps
      #_
      (->> (filter #(= "Subscriptions" (get % "Category"))))
      (->> (filter #(= "Groceries" (get % "Category"))))
      #_#_first
        (select-keys ["Amount" "Category" "Date"])))

(defn parse-date [date-str]
  (let [[m d y] (map #(Integer. %) (str/split date-str #"/"))]
    {:year y, :month m}))

(defn to-year-month [date-str]
  (let [[m d y] (map #(Integer. %) (clojure.string/split date-str #"/"))]
    (str y "-" (format "%02d" m))))

(defn sum-by-year-month [records]
  (->> records
       (map (fn [record]
              (let [{:strs [Amount Date]} record
                    yearmonth (to-year-month Date)
                    amount (Math/abs (Float. (clojure.string/replace Amount #"[^\d.-]" "")))]
                {:yearmonth yearmonth, :amount amount})))
       (group-by :yearmonth)
       (map (fn [[yearmonth grouped]]
              {:yearmonth yearmonth, :total (reduce + (map :amount grouped))}))
       (sort-by :yearmonth)))


(def data (sum-by-year-month records))

(clerk/vl
 {:data     {:values data}
  :mark     "bar"
  :width    500
  :encoding {
             :x {:field "yearmonth"
                 :type  "temporal"
                 :axis  {:title "Year-Month", :format "%Y-%m"}}
             :y {:field "total"
                 :type  "quantitative"
                 :axis  {:title "Total Amount"}}
             }})
