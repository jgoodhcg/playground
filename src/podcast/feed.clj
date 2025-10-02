(ns podcast.feed
  "Builds and updates the podcast RSS feed.")

(defn update-feed!
  "Placeholder feed update; returns a stub map describing the RSS path." 
  [_context audio]
  {:rss {:status :pending :last-audio (:audio audio)}})
