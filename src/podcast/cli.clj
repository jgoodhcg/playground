(ns podcast.cli
  "CLI entry point for the Too Busy To Read narration pipeline."
  (:require
    [clojure.pprint    :as pprint]
    [clojure.string    :as str]
    [clojure.tools.cli :as cli]
    [podcast.pipeline  :as pipeline]))

(def cli-options
  [["-u" "--url URL" "Source URL or local file path to narrate."]
   [nil  "--prompt-preset PRESET" "Prompt preset id to use for the assistant call." :default "default"]
   [nil  "--voice VOICE" "Voice id for OpenAI TTS synthesis." :default "ballad"]
   [nil  "--output-root PATH" "Directory for generated episode artifacts." :default "output/episodes"]
   [nil  "--rss-file PATH" "RSS feed XML file to append new items to." :default "output/feed/podcast.xml"]
   [nil  "--spaces-bucket NAME" "DigitalOcean Spaces bucket name." :default "spaces-tbt-readlater"]
   [nil  "--spaces-prefix PREFIX" "Key prefix/folder inside the Spaces bucket." :default "podcast/episodes"]
   [nil  "--publish" "Upload audio + feed via s3cmd after generation." :default false :default-desc "false" :assoc-fn (fn [m k _] (assoc m k true))]
   [nil  "--config PATH" "Optional EDN config file overriding defaults."]
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
     "  clj -M -m podcast.cli --url https://news.ycombinator.com/item?id=123 --prompt-preset quick-scan --voice verse"]))

(defn exit!
  [code message]
  (when (seq message)
    (println message))
  (System/exit code))

(defn -main
  [& args]
  (let [{:keys [options summary errors]} (cli/parse-opts args cli-options)]
    (cond
      (:help options)
      (exit! 0 (usage summary))

      (seq errors)
      (exit! 1 (str "Error:\n" (str/join \newline errors) "\n\n" (usage summary)))

      (nil? (:url options))
      (exit! 1 (str "Error: --url is required.\n\n" (usage summary)))

      :else
      (let [result (pipeline/execute! options)]
        (println "Pipeline result (stub):")
        (pprint/pprint result)))))
