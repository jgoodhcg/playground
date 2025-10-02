(ns podcast.audio
  "Wraps Text-to-Speech helpers to synthesize narrated audio files.")

(defn synthesize!
  "Placeholder synthesis stage; returns the input episode with dummy audio info."
  [context episode]
  (assoc episode :audio {:status :pending :voice (get-in context [:options :voice])}))
