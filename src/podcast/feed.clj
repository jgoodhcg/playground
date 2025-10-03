(ns podcast.feed
  "Builds and updates the podcast RSS feed."
  (:require
    [clojure.data.xml :as xml]
    [clojure.java.io :as io]
    [clojure.java.shell :as shell]
    [clojure.string :as str]))

(defn- build-description
  "Build episode description with summary and metadata."
  [script-metadata audio-metadata]
  (let [summary (:summary script-metadata)
        source (:source script-metadata)
        script-model (:model script-metadata)
        voice (:voice audio-metadata)
        usage (:usage script-metadata)
        lines (cond-> []
                summary (conj summary)
                true (conj "")
                source (conj (str "Source: " source))
                script-model (conj (str "Script generated with OpenAI " script-model))
                voice (conj (str "Audio generated with OpenAI TTS (voice: " voice ")"))
                (and usage (:total_tokens usage))
                (conj (str "Total tokens: " (:total_tokens usage))))]
    (if (seq lines)
      (str/join "\n" lines)
      "A new episode.")))

(defn- build-item
  "Constructs an XML item map for a new episode."
  [context state]
  (let [episode-id (get-in state [:episode :id])
        merged-file (get-in state [:episode :stage/merge :merged-file])
        script-metadata (get-in state [:script :metadata])
        audio-metadata (get-in state [:episode :stage/audio])
        episode-url (str (get-in context [:podcast :episode-base-url]) episode-id ".mp3")
        file-size (if merged-file
                    (.length (io/file merged-file))
                    0)
        title (or (:title script-metadata) episode-id)
        description (build-description script-metadata audio-metadata)]
    (xml/element :item {}
                 (xml/element :title {} title)
                 (xml/element :description {} description)
                 (xml/element :pubDate {} (.format (java.text.SimpleDateFormat. "EEE, dd MMM yyyy HH:mm:ss Z") (new java.util.Date)))
                 (xml/element :guid {} episode-url)
                 (xml/element :enclosure {:url episode-url
                                          :length (str file-size)
                                          :type "audio/mpeg"})
                 (xml/element :itunes:author {} (get-in context [:podcast :author]))
                 (xml/element :itunes:summary {} description))))

(defn- build-channel
  "Constructs the main channel structure for the RSS feed."
  [context items]
  (let [details (:podcast context)]
    (xml/element :channel {}
                 (xml/element :title {} (:title details))
                 (xml/element :link {} (:url details))
                 (xml/element :description {} (:description details))
                 (xml/element :language {} "en-us")
                 (xml/element :itunes:author {} (:author details))
                 (xml/element :itunes:explicit {} "no")
                 (xml/element :itunes:category {:text "Technology"})
                 items)))

(defn- generate-xml
  "Generates the full RSS XML document."
  [items]
  (xml/emit-str
    (xml/element :rss {:version "2.0"
                       :xmlns:itunes "http://www.itunes.com/dtds/podcast-1.0.dtd"
                       :xmlns:atom "http://www.w3.org/2005/Atom"}
                 items)))

(defn- download-remote-feed
  "Attempt to download existing feed from S3. Returns nil if not found or on error."
  [bucket local-path s3cfg]
  (let [remote-path (str "s3://" bucket "/podcast/podcast.xml")
        download-cmd (cond-> ["s3cmd"]
                       s3cfg (concat ["-c" s3cfg])
                       true (concat ["get" remote-path local-path "--force"]))]
    (println "[feed] Attempting to download existing feed from" remote-path)
    (let [result (apply shell/sh download-cmd)]
      (when (= 0 (:exit result))
        (println "[feed] Successfully downloaded existing feed")
        true))))

(defn update-feed!
  "Downloads existing feed (if available), adds new episode, saves locally."
  [context state]
  (let [rss-file-path (get-in context [:options :rss-file] "output/feed/podcast.xml")
        bucket (get-in context [:options :spaces-bucket])
        s3cfg (get-in context [:options :s3cfg])
        rss-file (io/file rss-file-path)]
    (io/make-parents rss-file-path)

    ;; Try to download from remote
    (download-remote-feed bucket rss-file-path s3cfg)

    ;; Parse existing feed (either downloaded or previously local)
    (let [existing-feed (when (.exists rss-file)
                          (try (xml/parse-str (slurp rss-file-path))
                               (catch Exception e
                                 (println "[feed] Warning: Could not parse existing feed:" (.getMessage e))
                                 nil)))
          new-item (build-item context state)
          existing-items (when existing-feed
                           (->> (-> existing-feed :content first :content)
                                (filter #(= :item (:tag %)))))
          all-items (concat [new-item] existing-items)
          channel (build-channel context all-items)
          final-xml (generate-xml channel)]
      (spit rss-file-path final-xml)
      (println "[feed] Feed written to" rss-file-path)
      {:rss {:status (if existing-feed :feed-updated :feed-created)
             :path rss-file-path
             :items (count all-items)}})))
