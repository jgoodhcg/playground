(ns podcast.storage
  "Manages local filesystem layout and metadata for generated episodes.")

(defn stage-episode!
  "Stub for writing episode assets to disk.

  Returns the response data unchanged for now."
  [context response-data]
  (assoc response-data :episode {:status :pending :output-root (get-in context [:options :output-root])}))
