(ns playground.bb.openai
  (:require [babashka.curl :as curl]
            [babashka.cli :as cli]
            [babashka.json :as json]
            [clojure.term.colors :as c]
            [puget.printer :as puget]
            [playground.bb.secrets :as secrets]))

;; example usage
;; bb -m playground.bb.openai -t models

(defmulti ai :target)

(def base-url
  "https://api.openai.com/v1")

(def base-headers
  {"Content-Type"  "application/json"
   "Authorization" (str "Bearer " secrets/openai-api-key)})

(def base-curl-opts
  {:throw false :headers base-headers})

(defn print-response
  [{:keys [status headers] :as response}]
  (cond
    (= 200 status)
    (do (-> (str "Success " status) c/blue println)
          (-> response :body json/read-str puget/cprint))

    :else
    (do (-> (str "Failure " status) c/red println)
        (-> response :body json/read-str puget/cprint))))

(defmethod ai :models
  [_]
  (-> (str base-url "/models")
      (curl/get base-curl-opts)
      #_print-response
      :body
      json/read-str
      :data
      (->> (map :id))
      puget/cprint))

(defmethod ai :chat-test
  [_]
  (-> (str base-url "/chat/completions")
      (curl/get (merge base-curl-opts
                       {:body (json/write-str
                               {:model    "gpt-3.5-turbo"
                                :messages [{:role    "system"
                                            :content "You are a helpful assistant"}
                                           {:role    "user"
                                            :content "hello?"}]})}))
      print-response
      ))

(defn -main [& params]
  (let [parsed-params
        (cli/parse-opts params {:require [:target]
                                :alias   {:t :target
                                          :l :label}
                                :coerce  {:target :keyword}})]
    (ai parsed-params)))
