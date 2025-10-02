(ns podcast.assistant
  "Handles interactions with the OpenAI Assistants API to produce narration scripts."
  (:require
    [clj-http.client :as client]
    [clojure.string  :as str]
    [cheshire.core   :as json]
    [secrets         :as secrets]))

(def ^:private chat-url "https://api.openai.com/v1/chat/completions")

(defn- render-template
  [template replacements]
  (reduce
    (fn [acc [k v]]
      (str/replace acc (str "{{" (name k) "}}") (str v)))
    template
    replacements))

(defn- build-prompt
  [{:keys [options assistant]}]
  (let [preset-id  (or (:prompt-preset options) "default")
        preset-key (if (keyword? preset-id) preset-id (keyword preset-id))
        preset     (or (get-in assistant [:presets preset-key])
                       (get-in assistant [:presets :default]))
        template   (:prompt-template preset)
        url        (:url options)]
    (when-not url
      (throw (ex-info "Missing URL for assistant prompt." {:options options})))
    (when-not template
      (throw (ex-info "No prompt template found for preset." {:preset preset-id})))
    {:preset preset-key
     :system (or (:system-prompt assistant)
                 "You are an expert narrator.")
     :user   (render-template template {:url url})
     :model  (or (:model assistant) "gpt-4.1-mini")}))

(defn- parse-response
  [{:keys [status body]}]
  (if (= 200 status)
    (let [choice (first (:choices body))
          content (get-in choice [:message :content])
          text (if (coll? content)
                 (->> content (map :text) (remove nil?) (str/join "\n"))
                 content)]
      (when (str/blank? text)
        (throw (ex-info "Assistant response was empty." {:body body})))
      {:text text :usage (:usage body)})
    (throw (ex-info "Assistant API error" {:status status :body body}))))

(defn generate
  "Call the OpenAI Chat Completions API using the configured preset and return a script map."
  [context]
  (let [{:keys [system user model preset]} (build-prompt context)
        api-key                          secrets/openai-api-key]
    (when (str/blank? api-key)
      (throw (ex-info "OPENAI_API_KEY is missing." {})))
    (println "[assistant] model=" model "preset=" (name preset))
    (let [truncate (fn [label text]
                     (let [max-len 120]
                       (println label
                                (if (<= (count text) max-len)
                                  text
                                  (str (subs text 0 max-len) "…")))))]
      (truncate "[assistant] system prompt=" system)
      (truncate "[assistant] user prompt=" user))
    (let [response (client/post chat-url
                                {:headers {"Authorization" (str "Bearer " api-key)
                                           "Content-Type" "application/json"}
                                 :body    (json/encode {:model    model
                                                       :messages [{:role "system" :content system}
                                                                  {:role "user"   :content user}]})
                                 :as      :json
                                 :coerce  :always
                                 :throw-exceptions false})
          {:keys [text usage]} (parse-response response)]
    (println "[assistant] tokens usage=" usage)
    {:script   text
     :metadata {:source (:url (:options context))
                :model  model
                :preset preset
                :usage  usage}})))
