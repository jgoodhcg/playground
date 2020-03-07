(ns aclaimant-tech-interview
  (:require [com.rpl.specter :as sp]
            [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.test :refer [deftest is run-tests]]))

;; Github users have followers, you can find them using an API we've created that mimics Github's API:
;; https://acl-github-interview.herokuapp.com/users/<LOGIN>/followers?page=1

;; See the Github docs for more info on the API:
;; https://developer.github.com/v3/users/followers/

(defn get-followers [user]
  (loop [next   (str
                 "https://acl-github-interview.herokuapp.com/users/"
                 user
                 "/followers?page=1")
         result []]
    (if (nil? next)
      result
      (let [response  (client/get next)
            followers (->> response
                           :body
                           json/read-str
                           (map (fn [x] (get x "login"))))]
        (recur
         (->> response :links :next :href)
         (concat result followers))))))

;; Write a function to get the followers common between two users:
(defn get-common-followers [user-1 user-2]
  (let [user-1-followers (set (get-followers user-1))
        user-2-followers (set (get-followers user-2))]

    (clojure.set/intersection user-1-followers user-2-followers)))

;; Make these tests pass:
(deftest test-common-followers
  (is (= #{"chownation" "joelash"} (get-common-followers "snoe" "manicolosi")))
  (is (= #{} (get-common-followers "skuttleman" "manicolosi")))
  (is (= #{"narendraj9" "devn" "yalu" "nrfm" "jgmize" "tony824" "RutledgePaulV" "joelash" "Biserkov" "KevinHock"}
         (get-common-followers "gfredericks" "timothypratley"))) )


(run-tests)



