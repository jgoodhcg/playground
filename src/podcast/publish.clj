(ns podcast.publish
  "Uploads generated assets to DigitalOcean Spaces via s3cmd.")

(defn upload!
  "Placeholder publish step; prints a reminder until implementation lands." 
  [context feed]
  (println "[publish] Skipping upload; implement s3cmd integration later.")
  {:publish {:status :skipped
             :bucket (get-in context [:options :spaces-bucket])
             :feed feed}})
