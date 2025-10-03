(ns podcast.publish
  "Uploads generated assets to DigitalOcean Spaces via s3cmd."
  (:require
    [clojure.java.shell :as shell]
    [clojure.java.io :as io]))

(defn- upload-file
  "Upload a single file to S3 using s3cmd."
  [local-path remote-path s3cfg]
  (let [cmd (cond-> ["s3cmd"]
              s3cfg (concat ["-c" s3cfg])
              true (concat ["put" local-path remote-path "--acl-public"]))]
    (println "[publish] Uploading" local-path "to" remote-path)
    (let [result (apply shell/sh cmd)]
      (if (= 0 (:exit result))
        (do
          (println "[publish] Successfully uploaded" remote-path)
          {:status :success :remote-path remote-path})
        (do
          (println "[publish] Failed to upload" remote-path)
          (println "[publish] Error:" (:err result))
          {:status :failed :error (:err result) :remote-path remote-path})))))

(defn upload!
  "Uploads episode MP3 and RSS feed to DigitalOcean Spaces."
  [context state]
  (let [bucket (get-in context [:options :spaces-bucket])
        s3cfg (get-in context [:options :s3cfg])
        episode-id (get-in state [:episode :id])
        merged-file (get-in state [:episode :stage/merge :merged-file])
        rss-file (get-in state [:feed :rss :path])]

    (cond
      (not merged-file)
      (do
        (println "[publish] No merged audio file found; skipping upload")
        {:publish {:status :no-audio-file}})

      (not (.exists (io/file merged-file)))
      (do
        (println "[publish] Audio file does not exist:" merged-file)
        {:publish {:status :audio-file-missing :path merged-file}})

      (not rss-file)
      (do
        (println "[publish] No RSS feed file found; skipping upload")
        {:publish {:status :no-rss-file}})

      (not (.exists (io/file rss-file)))
      (do
        (println "[publish] RSS file does not exist:" rss-file)
        {:publish {:status :rss-file-missing :path rss-file}})

      :else
      (let [episode-remote (str "s3://" bucket "/podcast/episodes/" episode-id ".mp3")
            feed-remote (str "s3://" bucket "/podcast/podcast.xml")
            episode-result (upload-file merged-file episode-remote s3cfg)
            feed-result (upload-file rss-file feed-remote s3cfg)]
        {:publish {:status (if (and (= :success (:status episode-result))
                                    (= :success (:status feed-result)))
                             :published
                             :partial-failure)
                   :episode episode-result
                   :feed feed-result}}))))
