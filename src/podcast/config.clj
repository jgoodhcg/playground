
(ns podcast.config
  "Loads configuration for prompts, voices, paths, and Spaces credentials."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(def default-config
  {:assistant
   {:model "gpt-4.1-mini"
    :system-prompt
    "You are an expert podcast scriptwriter who produces concise yet vivid narrations. Blend informative clarity with a warm, conversational tone."
    :presets
    {:default
     {:prompt-template
      (str "You will produce a full narration script for an audio episode summarizing a single source.\n"
           "Source URL: {{url}}\n\n"
           "Structure the response with clear section headings: Intro, Post Summary, Comment Highlights, Verbatim Read."
           "For Comment Highlights, synthesize themes and provide representative quotes.\n"
           "Write in Markdown, using short paragraphs and speaker cues when helpful.")}
     :quick-scan
     {:prompt-template
      (str "Write a short narration script for a podcast segment about {{url}}.\n"
           "Keep it under 500 words, focused on the key takeaways and whether the listener should continue to the full read.")}}}})

(defn- deep-merge
  "Recursively merge maps, preferring values from `b` when not both maps."
  [a b]
  (cond
    (and (map? a) (map? b)) (merge-with deep-merge a b)
    :else b))

(defn- read-config-file
  [path]
  (when path
    (let [file (io/file path)]
      (when (.exists file)
        (with-open [r (io/reader file)]
          (edn/read {:eof {}} r))))))

(defn load-config
  "Load an EDN config file at `path` and merge it with defaults.

  Missing files are ignored so the CLI can run with baked-in defaults."
  [path]
  (let [user-config (or (read-config-file path)
                        (when path
                          (println "Config file not found, ignoring:" path)
                          {}))]
    (deep-merge default-config (or user-config {}))))
