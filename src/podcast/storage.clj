(ns podcast.storage
  "Manages local filesystem layout and metadata for generated episodes.")

(defn stage-episode!
  "Stub for writing episode assets to disk.

  Returns the assistant data unchanged for now." 
  [context assistant-output]
  (assoc assistant-output :episode {:status :pending :output-root (get-in context [:options :output-root])}))
