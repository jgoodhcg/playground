(ns podcast.cli
  "CLI entry point for the Too Busy To Read narration pipeline."
  (:require
    [clojure.pprint    :as pprint]
    [clojure.java.io   :as io]
    [clojure.string    :as str]
    [clojure.tools.cli :as cli]
    [podcast.pipeline  :as pipeline]))

(def cli-options
  [["-u" "--url URL" "Source URL or local file path to narrate."]
   [nil  "--episode-dir PATH" "Existing episode directory to resume from (script/audio already staged)."]
   [nil  "--voice VOICE" "Voice id for OpenAI TTS synthesis." :default "ballad"]
   [nil  "--output-root PATH" "Directory for generated episode artifacts." :default "resources/tbtr"]
   [nil  "--rss-file PATH" "RSS feed XML file to append new items to." :default "output/feed/podcast.xml"]
   [nil  "--spaces-bucket NAME" "DigitalOcean Spaces bucket name." :default "tbtr"]
   [nil  "--spaces-prefix PREFIX" "Key prefix/folder inside the Spaces bucket." :default "podcast/episodes"]
   [nil  "--s3cfg PATH" "Path to s3cmd config file." :default (str (System/getProperty "user.home") "/do-tor1.s3cfg")]
   [nil  "--model MODEL" "OpenAI model used for narration generation (defaults to gpt-4.1)." :default "gpt-4.1"]
   [nil  "--max-output-tokens TOKENS" "Maximum tokens to request from the model." :default 12000 :parse-fn #(Integer/parseInt %)]
   [nil  "--system-prompt PROMPT" "Override the system prompt for narration generation."]
   [nil  "--prompt-template TEMPLATE" "Override the narration prompt template (use \\n for newlines)."]
   [nil  "--start-stage STAGE" "Stage to begin pipeline at (generate-script, stage-script, synthesize-audio, merge-audio, update-feed, publish)." :default "generate-script"]
   [nil  "--publish" "Upload audio + feed via s3cmd after generation." :default false :default-desc "false" :assoc-fn (fn [m k _] (assoc m k true))]
   ["-h" "--help" "Print this help and exit."]])

(defn usage
  [summary]
  (str/join
    \newline
    ["Too Busy To Read — CLI narrator"
     ""
     "Usage: clj -M -m podcast.cli [options]"
     ""
     "Options:"
     summary
     ""
     "Example:"
     "  clj -M -m podcast.cli --url https://news.ycombinator.com/item?id=123 --model gpt-4.1 --voice verse"]))

(defn exit!
  [code message]
  (when (seq message)
    (println message))
  (System/exit code))

(defn -main
  [& args]
  (let [{:keys [options summary errors]} (cli/parse-opts args cli-options)
        start-stage (some-> (:start-stage options) keyword)
        episode-dir (:episode-dir options)
        url         (:url options)]
    (cond
      (:help options)
      (exit! 0 (usage summary))

      (seq errors)
      (exit! 1 (str "Error:\n" (str/join \newline errors) "\n\n" (usage summary)))

      (not (contains? (set pipeline/stage-order) start-stage))
      (exit! 1 (str "Error: invalid --start-stage " (name start-stage) ".\nValid stages: "
                    (str/join ", " (map name pipeline/stage-order))
                    "\n\n" (usage summary)))

      (and (nil? url)
           (not episode-dir)
           (not (#{:update-feed :publish} start-stage)))
      (exit! 1 (str "Error: --url or --episode-dir is required for stage " (name start-stage) ".\n\n" (usage summary)))

      (and episode-dir (not (.exists (io/file episode-dir))))
      (exit! 1 (str "Error: episode directory does not exist: " episode-dir "\n\n" (usage summary)))

      :else
      (let [result (pipeline/execute! (assoc options :start-stage start-stage))]
        (println "\nPipeline complete.")
        (println "  - Episode:" (get-in result [:episode :episode :status] "pending"))
        (println "  - Audio:"   (get-in result [:audio :status] "pending"))
        (println "  - Feed:"    (get-in result [:feed :rss :status] "pending"))
        (when-let [publish-status (get-in result [:publish :publish :status])]
          (println "  - Publish:" publish-status))))))
