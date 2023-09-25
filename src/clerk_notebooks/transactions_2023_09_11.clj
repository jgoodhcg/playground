(ns clerk-notebooks.transactions-2023-09-11
  (:require [nextjournal.clerk :as clerk]
            [clojure.data.csv :as csv]
            [clojure.string :as str]
            [potpuri.core :as pot]))

(comment
  (clerk/serve! {:watch-paths ["src/clerk_notebooks"]}))

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
      (->> (filter #(contains?
                     #{"Groceries"
                       "Restaurants"
                       "Food Delivery"
                       "Fast Food"
                       "Farmer's Market"}
                     (get % "Category"))))
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
              (let [{:strs [Amount Date Category]} record
                    yearmonth (to-year-month Date)
                    amount (Math/abs (Float. (clojure.string/replace Amount #"[^\d.-]" "")))]
                {:yearmonth yearmonth, :amount amount, :category Category})))
       (group-by (fn [{:keys [yearmonth category]}] [yearmonth category]))
       (map (fn [[[yearmonth category] grouped]]
              {:yearmonth yearmonth, :total (reduce + (map :amount grouped)), :category category}))
       (sort-by (juxt :yearmonth :category))))

(def data (sum-by-year-month records))

(clerk/vl
 {:data     {:values data}
  :mark     {:type "bar" :width {:band 0.5}}
  :width    500
  :encoding {:x     {:field    "yearmonth"
                     :type     "temporal"
                     :axis     {:title "Year-Month", :format "%y-%m"}}
             :y     {:field "total"
                     :type  "quantitative"
                     :axis  {:title "Total Amount"}}
             :color {:field "category"}}})

(def all-transactions
  (-> "src/clerk_notebooks/transactions_2023_09_11.csv"
      slurp
      csv/read-csv
      csv-data->maps))

;; My stupid consolidation of descriptions with defs map and cond
(comment
  (def amazon-prime-regex #"Amazon Prime\*\w+")
  (def audible-regex #"Audible\*\w+")
  (def apple-regex #"Apple\.com/bill.*")
  (def paypal-regex #"Paypal.*")

  (def grouped-subs
    (->> all-transactions
         (filter #(= "Subscriptions" (get % "Category")))
         (map #(get % "Description"))
         (map (fn [d]
                (cond
                  (re-matches amazon-prime-regex d) :amazon-prime
                  (re-matches audible-regex d)      :audible
                  (re-matches apple-regex d)        :apple
                  (re-matches paypal-regex d)       :paypal
                  :else                             d)))
         set)))

;; Better chatgpt version using a map and some
(def regex-keyword-pairs
  {#"Amazon Prime\*\w+"      :amazon-prime
   #"Prime Video.*"          :amazon-prime-video
   #"Help\.hbomax.*"         :hbo
   #"Help\.max.*"            :hbo
   #"Audible\*\w+"           :audible
   #"Roam.*"                 :roam
   #"Paypal *roamresearch.*" :roam
   #"Apple\.com/bill.*"      :apple
   #"Pp*apple.*"             :apple
   #"Github.*"               :github
   #"Paypal *github Inc.*"   :github
   #"Kindle.*"               :kindle})

(defn match-keyword [{:keys [description] :as t}]
  (let [consolidated-desc (or (->> regex-keyword-pairs
                                   (some (fn [[r k]] (when (re-matches r description) k))))
                              description)]
    (merge t {:description consolidated-desc})))

(def grouped-subs
  (->> all-transactions
       (filter #(= "Subscriptions" (get % "Category")))
       (map (fn [record]
              (let [{:strs [Amount Description]} record
                    amount (Math/abs (Float. (clojure.string/replace Amount #"[^\d.-]" "")))]
                {:amount amount :description Description})))
       (map match-keyword)
       (group-by :description)
       (pot/map-vals #(->> % (map :amount) (reduce +)))))

(def consolidated-data
  (->> grouped-subs
       (map (fn [[description amount]]
              {:description description :amount amount}))
       (into [])))

(clerk/vl
 {:data     {:values consolidated-data}
  :mark     "bar"
  :width    500
  :encoding {:x     {:field "description"
                     :type  "nominal"
                     :axis  {:title "Description"}}
             :y     {:field "amount"
                     :type  "quantitative"
                     :axis  {:title "Total Amount"}}}})

(clerk/vl
 {:data     {:values consolidated-data}
  :mark     "arc"
  :encoding {:theta {:field "amount"
                     :type  "quantitative"}
             :color {:field "description"
                     :type  "nominal"}}
  :view     {:stroke nil}
  :width    300
  :height   300})

(clerk/vl
 {:data     {:values consolidated-data}
  :mark     "bar"
  :encoding {:y {:field "description"
                 :type  "nominal"
                 :axis  {:title "Subscription Categories"}}
             :x {:field "amount"
                 :type  "quantitative"
                 :axis  {:title "Total Amount"}}}})


(def essential
  #{:github
   :roam
   "Pp*google 1se Play Store"
   "Paypal *spotifyusai"
   "Paypal *hoverdotcom"
   :kindle
   "Pp*google Fitbit Llc"
   "Digitalocean.com"
   "apple icloud"
   :apple
   "1password Toronto Can"
   "Openai San Francisco CA"
   "Chatgpt Subscription San Francisco CA"
   :audible
   :amazon-prime
   "Tillerhq* Trial Over Winthrop WA"
   "Midjourney Inc. South San Fra CA"
   "Bc.hey Email x x5333 IL"
   "Paypal *github Inc"})

(def essential-data
  (filter (fn [{:keys [description]}]
            (contains? essential description))
          consolidated-data))

(def essential-total (reduce + (map :amount essential-data)))
(def overall-total (reduce + (map :amount consolidated-data)))
(def comparison-data [{:group "Essential" :amount essential-total}
                      {:group "Overall" :amount overall-total}])

(clerk/vl
 {:data     {:values comparison-data}
  :mark     "bar"
  :encoding {:x {:field "group"
                 :type  "nominal"
                 :axis  {:title "Group"}}
             :y {:field "amount"
                 :type  "quantitative"
                 :axis  {:title "Total Amount"}}}})

(def non-essential-total (- overall-total essential-total))
(def proportion-data [{:group "Essential" :amount essential-total}
                      {:group "Non-Essential" :amount non-essential-total}])

(clerk/vl
 {:data     {:values proportion-data}
  :mark     "bar"
  :encoding {:x {:field "group"
                 :type  "nominal"
                 :axis  {:title "Group"}}
             :y {:field "amount"
                 :type  "quantitative"
                 :axis  {:title "Total Amount"}}}})
