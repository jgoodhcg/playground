(ns openai.text-to-speech
  (:require [secrets :as secrets]
            [clj-http.client :as client]
            [cheshire.core :as json]
            [clojure.java.io :as io]))


(def base-url "https://api.openai.com/v1")

;; curl https://api.openai.com/v1/audio/speech \
;;   -H "Authorization: Bearer $OPENAI_API_KEY" \
;;   -H "Content-Type: application/json" \
;;   -d '{
;;     "model": "tts-1",
;;     "input": "The quick brown fox jumped over the lazy dog.",
;;     "voice": "alloy"
;;   }' \
;;   --output speech.mp3

(defn save-mp3-from-text-file [{:keys [text-file-path
                                       mp3-file-path
                                       voice]
                                :or {voice "onyx"}}]
  (let [input-text (slurp text-file-path)
        url        (str base-url "/audio/speech")
        api-key    secrets/openai-api-key
        response   (client/post url
                                {:headers {"Authorization" (str "Bearer " api-key)
                                           "Content-Type"  "application/json"}
                                 :body    (json/encode {:model "tts-1"
                                                        :voice voice
                                                        :input input-text})
                                 :as      :byte-array})]
    (with-open [out (io/output-stream mp3-file-path)]
      (.write out (:body response)))))

(comment
  (save-mp3-from-text-file
   {:text-file-path "resources/openai-audio-test.txt"
    :mp3-file-path  "resources/eopnai-audio-test.mp3"}
   )

  (save-mp3-from-text-file
   {:text-file-path "resources/a-day-in-the-life.txt"
    :mp3-file-path  "resources/a-day-in-the-life.mp3"}
   )
  ;;
  )
