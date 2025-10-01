(ns openai.text-to-speech
  (:require
    [cheshire.core   :as json]
    [clj-http.client :as client]
    [clojure.java.io :as io]
    [clojure.string  :as str]
    [secrets         :as secrets]))

(def base-url "https://api.openai.com/v1")

(defn- partition-text
  [text chunk-size]
  (->> text
       (partition-all chunk-size)
       (map #(apply str %))))

(defn save-mp3-from-text-file
  [{:keys [text-file-path mp3-file-path voice instructions],
    :or
      {voice "ballad",
       instructions
         "Read this in a calm, thoughtful, introspective tone, like a spoken-word podcast. Keep a steady pace with light emotion and dry humor. Pause slightly after key phrases or punchlines. Emphasize sarcasm subtly without overacting. Keep the delivery grounded and human."}}]
  (let [input-text (slurp text-file-path)
        parts      (partition-text input-text 4000)
        base-name  (if (str/ends-with? mp3-file-path ".mp3")
                     (subs mp3-file-path 0 (- (count mp3-file-path) 4))
                     mp3-file-path)
        url        (str base-url "/audio/speech")
        api-key    secrets/openai-api-key]
    (doseq [[idx part] (map-indexed vector parts)]
      (let [response (client/post url
                                  {:headers {"Authorization" (str "Bearer "
                                                                  api-key),
                                             "Content-Type" "application/json"},
                                   :body    (json/encode
                                              {:model        "gpt-4o-mini-tts",
                                               :voice        voice,
                                               :input        part,
                                               :instructions instructions}),
                                   :as      :byte-array})
            out-file (format "%s-pt-%d.mp3" base-name (inc idx))]
        (with-open [out (io/output-stream out-file)]
          (.write out (:body response)))))))

(comment
  (save-mp3-from-text-file
    {:text-file-path "resources/openai-audio-test.txt",
     :mp3-file-path  "resources/eopnai-audio-test.mp3"})

  (save-mp3-from-text-file
    {:text-file-path "resources/a-day-in-the-life.txt",
     :mp3-file-path  "resources/a-day-in-the-life.mp3"})

  (save-mp3-from-text-file
    {:text-file-path "resources/2054-reflections.txt",
     :mp3-file-path  "resources/2054-reflections.mp3"})

  (save-mp3-from-text-file
    {:text-file-path "resources/2054-ns-4o.txt",
     :mp3-file-path  "resources/2054-ns-4o.mp3"})

  (save-mp3-from-text-file
    {:text-file-path "resources/2054-ns-o1.txt",
     :mp3-file-path  "resources/2054-ns-o1.mp3"})

  (save-mp3-from-text-file
    {:text-file-path "resources/chat-history-analysis.txt",
     :mp3-file-path  "resources/chat-history-analysis.mp3"})

  (save-mp3-from-text-file
    {:text-file-path "resources/vonnegut-style-monologue.txt",
     :mp3-file-path  "resources/vonnegut-style-monologue.mp3"})

  (save-mp3-from-text-file 
    {:text-file-path "resources/claude-pick-a-theme.txt"
     :mp3-file-path "resources/claude-picka-theme.mp3"})
  ;;
)
