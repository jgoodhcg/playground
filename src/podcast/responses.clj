(ns podcast.responses
  "Builds narration scripts by calling the OpenAI Responses API."
  (:require
    [cheshire.core   :as json]
    [clj-http.client :as client]
    [clojure.string  :as str]
    [secrets         :as secrets]))

(def ^:private responses-url "https://api.openai.com/v1/responses")

(defn- render-template
  [template replacements]
  (reduce
    (fn [acc [k v]]
      (str/replace acc (str "{{" (name k) "}}") (str v)))
    template
    replacements))

(defn- build-request
  [context]
  (let [{:keys [options responses]} context
        {:keys [prompt-template system-prompt model max-output-tokens]} responses
        url (:url options)]
    (when-not url
      (throw (ex-info "Missing --url argument." {:options options})))
    (when (str/blank? prompt-template)
      (throw (ex-info "Prompt template cannot be blank." {:responses responses})))
    {:model model
     :instructions (or system-prompt "You are an expert narrator.")
     :user-prompt (render-template prompt-template {:url url})
     :max-output max-output-tokens
     :tools [{:type "web_search"}]
     :tool-choice "auto"}))

(defn- extract-text
  [body]
  (let [segments (for [item (:output body)
                       :when (= "message" (:type item))
                       part (:content item)
                       :when (= "output_text" (:type part))]
                   (:text part))
        text (->> segments
                  (map #(str/trim %))
                  (remove str/blank?)
                  (str/join "\n\n"))]
    (when (str/blank? text)
      (throw (ex-info "Responses API returned no text." {:body body})))
    text))

(defn generate
  "Invoke the OpenAI Responses API using the configured template and return a narration script."
  [context]
  (let [{:keys [model instructions user-prompt max-output tools tool-choice]} (build-request context)
        api-key secrets/openai-api-key]
    (when (str/blank? api-key)
      (throw (ex-info "OPENAI_API_KEY is missing." {})))
    (println "[responses] model=" model)
    (println "[responses] tools=" (map :type tools))
    (let [truncate (fn [label text]
                     (let [max-len 120]
                       (println label
                                (if (<= (count text) max-len)
                                  text
                                  (str (subs text 0 max-len) "…")))))]
      (truncate "[responses] instructions=" instructions)
      (truncate "[responses] user prompt=" user-prompt))
    (let [payload (cond-> {:model model
                           :instructions instructions
                           :input [{:role "user"
                                    :content [{:type "input_text" :text user-prompt}]}]
                           :tools tools}
                    tool-choice (assoc :tool_choice tool-choice)
                    max-output (assoc :max_output_tokens max-output))
          response (client/post responses-url
                                 {:headers {"Authorization" (str "Bearer " api-key)
                                            "Content-Type" "application/json"}
                                  :body    (json/encode payload)
                                  :as      :json
                                  :coerce  :always
                                  :throw-exceptions false})]
      (if (= 200 (:status response))
        (let [body (:body response)
              text (or (:output_text body)
                       (extract-text body))
              usage (:usage body)]
          (println "[responses] tokens usage=" usage)
          {:script   text
           :metadata {:source  (:url (:options context))
                      :model   model
                      :web-search-calls (for [item (:output body)
                                              :when (= "web_search_call" (:type item))]
                                          (select-keys item [:id :status :action]))
                      :usage   usage
                       :request payload}})
        (throw (ex-info "Responses API error" {:status (:status response)
                                                :body   (:body response)}))))))
