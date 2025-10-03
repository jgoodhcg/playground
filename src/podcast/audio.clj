(ns podcast.audio
  "Wraps Text-to-Speech helpers to synthesize narrated audio files."
  (:require
    [clojure.java.io           :as io]
    [clojure.string            :as str]
    [clojure.java.shell        :as shell]
    [openai.text-to-speech     :as tts]
    [podcast.storage           :as storage])
  (:import
    (java.time Instant)))

(def ^:private default-audio-base "episode")

(defn- chunk-index
  [file]
  (some->> (re-find #"-pt-(\d+)\.mp3$" (.getName file))
           second
           Integer/parseInt))

(defn- chunk-metadata
  [files]
  (->> files
       (sort-by chunk-index)
       (map (fn [file]
              {:path  (.getAbsolutePath file)
               :name  (.getName file)
               :index (chunk-index file)
               :bytes (.length file)}))
       (remove #(nil? (:index %)))
       vec))

(defn- audio-dir
  [episode-dir]
  (io/file episode-dir "audio"))

(defn- synthesize-chunks!
  [{:keys [script-path audio-dir voice instructions base-name]}]
  (let [base-mp3 (.getAbsolutePath (io/file audio-dir (str base-name ".mp3")))]
    (tts/save-mp3-from-text-file
      (cond-> {:text-file-path script-path
               :mp3-file-path  base-mp3
               :voice          voice}
        instructions (assoc :instructions instructions)))))

(defn synthesize!
  "Generate TTS audio for the episode script and update the episode metadata."
  [context episode-data]
  (let [{:keys [episode]} episode-data
        {:keys [script-path episode-dir episode-edn id]} episode
        script-file (some-> script-path io/file)
        voice       (or (get-in context [:options :voice]) "ballad")
        instructions (get-in context [:options :voice-instructions])
        audio-dir   (audio-dir episode-dir)
        base-name   (or id default-audio-base)
        timestamp   (.toEpochMilli (Instant/now))]
    (cond
      (nil? episode-edn)
      (do
        (println "[audio] No episode.edn path found; skipping audio synthesis.")
        (assoc episode-data :audio {:status :missing-episode-edn}))

      (or (nil? script-file) (not (.exists script-file)))
      (do
        (println "[audio] Script file missing; skipping audio synthesis:" script-path)
        (assoc episode-data :audio {:status :script-missing}))

      :else
      (let [episode-record (storage/read-episode episode-edn)
            existing       (:stage/audio episode-record)
            edn-path       episode-edn]
        (if (= :audio-generated (:status existing))
          (do
            (println "[audio] Audio already generated; skipping synthesis.")
            (assoc episode-data :audio existing))
          (do
            (storage/ensure-dir! audio-dir)
            ;; Clear previously generated chunk files to avoid mixing old data.
            (doseq [file (.listFiles audio-dir)
                    :when (str/starts-with? (.getName file) (str base-name "-pt-"))]
              (.delete file))
            (synthesize-chunks! {:script-path script-path
                                 :audio-dir   audio-dir
                                 :voice       voice
                                 :instructions instructions
                                 :base-name   base-name})
            (let [chunk-files (->> (.listFiles audio-dir)
                                   (filter #(str/starts-with? (.getName %) (str base-name "-pt-"))))
                  chunks      (chunk-metadata chunk-files)
                  audio-stage {:status        :audio-generated
                               :generated-at  timestamp
                               :voice         voice
                               :chunk-prefix  (str base-name "-pt-")
                               :audio-dir     (.getAbsolutePath audio-dir)
                               :chunk-base    (.getAbsolutePath (io/file audio-dir (str base-name ".mp3")))
                               :chunk-count   (count chunks)
                               :chunks        chunks
                               :script-path   script-path
                               :instructions  (when instructions instructions)}
                  updated-edn (storage/update-stage! edn-path
                                                     :stage/audio audio-stage)]
              (assoc episode-data
                     :audio audio-stage
                     :episode (assoc episode
                                      :stage/audio (:stage/audio updated-edn))))))))))

(defn merge-audio!
  "Merge audio chunks into a single MP3 file using ffmpeg."
  [context episode-data]
  (let [audio-details (:audio episode-data)
        episode-edn   (get-in episode-data [:episode :episode-edn])
        episode-dir   (get-in episode-data [:episode :episode-dir])]
    (cond
      (nil? episode-edn)
      (assoc-in episode-data [:audio :status] :missing-episode-edn)

      (or (nil? audio-details) (empty? (:chunks audio-details)))
      (assoc-in episode-data [:audio :status] :missing-audio-details)

      :else
      (let [{:keys [chunks audio-dir]} audio-details
            merged-filename (str (or (get-in episode-data [:episode :id]) "episode") ".mp3")
            merged-path     (.getAbsolutePath (io/file episode-dir merged-filename))
            ffmpeg-input    (str "concat:" (str/join "|" (map :path chunks)))
            command         ["ffmpeg" "-y" "-i" ffmpeg-input "-c" "copy" merged-path]
            _               (println "[audio-merge] Executing command:" (str/join " " command))
            result          (apply shell/sh command)]
        (if (= 0 (:exit result))
          (do
            (println "[audio-merge] Merge successful.")
            (let [timestamp   (.toEpochMilli (Instant/now))
                  merge-stage {:status       :audio-merged
                               :merged-at    timestamp
                               :merged-file  merged-path}]
              (storage/update-stage! episode-edn :stage/merge merge-stage)
              (-> episode-data
                  (assoc :audio (assoc audio-details :status :audio-merged))
                  (assoc-in [:episode :stage/merge] merge-stage))))
          (do
            (println "[audio-merge] Merge failed. Error:" (:err result))
            (let [error-stage {:status      :merge-failed
                               :error       (:err result)
                               :command     (str/join " " command)}]
              (storage/update-stage! episode-edn :stage/merge error-stage)
              (-> episode-data
                  (assoc :audio (assoc audio-details :status :merge-failed))
                  (assoc-in [:episode :stage/merge] error-stage)))))))))
