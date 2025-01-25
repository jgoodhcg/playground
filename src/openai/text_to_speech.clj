(ns openai.text-to-speech
  (:require [secrets :as secrets]
            [clj-http.client :as client]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def base-url "https://api.openai.com/v1")

(defn- partition-text [text chunk-size]
  (->> text
       (partition-all chunk-size)
       (map #(apply str %))))

(defn save-mp3-from-text-file
  [{:keys [text-file-path mp3-file-path voice]
    :or {voice "onyx"}}]
  (let [input-text  (slurp text-file-path)
        parts       (partition-text input-text 4000)
        base-name   (if (str/ends-with? mp3-file-path ".mp3")
                      (subs mp3-file-path 0 (- (count mp3-file-path) 4))
                      mp3-file-path)
        url         (str base-url "/audio/speech")
        api-key     secrets/openai-api-key]
    (doseq [[idx part] (map-indexed vector parts)]
      (let [response (client/post url
                                  {:headers {"Authorization" (str "Bearer " api-key)
                                             "Content-Type"  "application/json"}
                                   :body    (json/encode {:model "tts-1"
                                                          :voice voice
                                                          :input part})
                                   :as      :byte-array})
            out-file (format "%s-pt-%d.mp3" base-name (inc idx))]
        (with-open [out (io/output-stream out-file)]
          (.write out (:body response)))))))

(comment
  (save-mp3-from-text-file
   {:text-file-path "resources/openai-audio-test.txt"
    :mp3-file-path  "resources/eopnai-audio-test.mp3"})

  (save-mp3-from-text-file
   {:text-file-path "resources/a-day-in-the-life.txt"
    :mp3-file-path  "resources/a-day-in-the-life.mp3"})

  (save-mp3-from-text-file
   {:text-file-path "resources/2054-reflections.txt"
    :mp3-file-path  "resources/2054-reflections.mp3"})

  (save-mp3-from-text-file
   {:text-file-path "resources/2054-ns-4o.txt"
    :mp3-file-path  "resources/2054-ns-4o.mp3"})

  (save-mp3-from-text-file
   {:text-file-path "resources/2054-ns-o1.txt"
    :mp3-file-path  "resources/2054-ns-o1.mp3"})
  ;;
  )
