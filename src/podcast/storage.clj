(ns podcast.storage
  "Manages local filesystem layout and metadata for generated episodes."
  (:require
    [clojure.edn     :as edn]
    [clojure.java.io :as io]
    [clojure.string  :as str])
  (:import
    (java.time Instant ZoneId)
    (java.time.format DateTimeFormatter)))

(def ^:private timestamp-formatter
  (-> (DateTimeFormatter/ofPattern "yyyyMMdd-HHmmss")
      (.withZone (ZoneId/systemDefault))))

(defn ensure-dir!
  [dir]
  (let [file (io/file dir)]
    (when-not (.exists file)
      (.mkdirs file))
    (.getAbsolutePath file)))

(defn- sanitize
  [s]
  (-> (or s "episode")
      str/lower-case
      (str/replace #"https?://" "")
      (str/replace #"[^a-z0-9]+" "-")
      (str/replace #"^-+" "")
      (str/replace #"-+$" "")
      (or "episode")))

(defn- episode-id
  [source-url]
  (let [timestamp (.format timestamp-formatter (Instant/now))
        slug      (sanitize source-url)]
    (format "tbtr-%s-%s" timestamp slug)))

(defn- write-script!
  [path script]
  (spit path script)
  path)

(defn- write-episode-edn!
  [path data]
  (with-open [w (io/writer path)]
    (.write w (pr-str data)))
  path)

(defn- word-count
  [s]
  (when (and s (not (str/blank? s)))
    (->> (str/split s #"\s+")
         (remove str/blank?)
         count)))

(defn- episode-map
  [{:keys [episode-id source-url script-path episode-dir output-root script-text responses response-data]}]
  (let [metadata (:metadata response-data)
        timestamp (.toEpochMilli (Instant/now))]
    {:episode/id         episode-id
     :episode/source-url source-url
     :episode/output-root output-root
     :episode/dir         episode-dir
     :stage/generation    {:model/name       (:model responses)
                           :model/max-output (:max-output-tokens responses)
                           :responses/usage  (:usage metadata)
                           :timestamp        timestamp}
     :stage/script        {:status      :script-staged
                           :generated-at timestamp
                           :script-path script-path
                           :word-count  (word-count script-text)}}))

(defn read-episode
  [edn-path]
  (when edn-path
    (let [file (io/file edn-path)]
      (when (.exists file)
        (with-open [r (io/reader file)]
          (edn/read {:eof nil} (java.io.PushbackReader. r)))))))

(defn update-stage!
  [edn-path stage-key stage-data]
  (let [episode (or (read-episode edn-path) {})
        updated (assoc episode stage-key stage-data)]
    (write-episode-edn! edn-path updated)
    updated))

(defn load-episode-state
  "Load an existing episode directory and return initial pipeline state maps."
  [episode-dir]
  (let [dir      (io/file episode-dir)
        edn-path (.getAbsolutePath (io/file dir "episode.edn"))
        data     (read-episode edn-path)]
    (when data
      (let [script-path (get-in data [:stage/script :script-path])
            script-file (when script-path (io/file script-path))
            script-text (when (and script-file (.exists script-file))
                          (slurp script-file))]
        {:episode {:id           (:episode/id data)
                   :status       (get-in data [:stage/script :status])
                   :episode-dir  (.getAbsolutePath dir)
                   :episode-edn  edn-path
                   :script-path  script-path
                   :output-root  (:episode/output-root data)
                   :stage/merge  (:stage/merge data)}
         :script  (when script-text
                    {:script   script-text
                     :metadata {:source (:episode/source-url data)}})
         :audio   (:stage/audio data)
         :edn     data}))))

(defn stage-episode!
  "Persist the generated script to disk and return enriched episode metadata."
  [context response-data]
  (let [script-text (:script response-data)
        source-url  (or (get-in response-data [:metadata :source])
                        (get-in context [:options :url]))
        episode-id  (episode-id source-url)
        output-root (or (get-in context [:options :output-root]) "resources/tbtr")
        episode-dir (io/file output-root episode-id)
        script-path (.getAbsolutePath (io/file episode-dir "script.md"))
        edn-path    (.getAbsolutePath (io/file episode-dir "episode.edn"))
        output-root-path (.getAbsolutePath (io/file output-root))
        episode-dir-path (.getAbsolutePath episode-dir)]
    (ensure-dir! episode-dir)
    (when (seq script-text)
      (write-script! script-path script-text))
    (let [episode-edn (episode-map {:episode-id episode-id
                                    :source-url source-url
                                    :script-path script-path
                                    :episode-dir episode-dir-path
                                    :output-root output-root-path
                                    :script-text script-text
                                    :responses (:responses context)
                                    :response-data response-data})]
      (write-episode-edn! edn-path episode-edn)
      (assoc response-data
             :episode {:id          episode-id
                       :status      :script-staged
                       :output-root output-root-path
                       :episode-dir episode-dir-path
                       :script-path (when (seq script-text) script-path)
                       :episode-edn edn-path}))))
