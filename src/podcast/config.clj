(ns podcast.config
  "Provides default configuration and builds response settings from CLI options."
  (:require
    [clojure.string :as str]))

(def ^:private podcast-details
  {:title "Too Busy To Read"
   :description "A personal podcast feed of narrated articles."
   :author "Justin Good"
   :url "https://jmg.ac"
   :feed-public-url "https://tbtr.nyc3.digitaloceanspaces.com/podcast/podcast.xml"
   :episode-base-url "https://tbtr.nyc3.digitaloceanspaces.com/podcast/episodes/"})

(def ^:private default-responses
  {:model "gpt-4.1",
   :max-output-tokens 12000,
   :system-prompt
    (str
      "You are a neutral, high-information-density narrator for a podcast aimed at a mid-30s software developer. "
      "Your tone is balanced and analytical. You focus on context, mechanisms, tradeoffs, dissent, and implications, "
      "favoring systems thinking and practical takeaways. Embody this persona directly; do not describe it or the audience."),
   :prompt-template
    (str
      "Read the full content from this URL: {{url}}\n\n"
      "After reading the content, produce a spoken-word narration script based on what you read.\n\n"
      "OUTPUT FORMAT:\n"
      "First line: JSON metadata in this exact format:\n"
      "{\"title\": \"Brief descriptive title based on the actual content\", \"summary\": \"One sentence summary of what you read\"}\n\n"
      "Remaining lines: The narration script\n\n"
      "SCRIPT REQUIREMENTS:\n"
      "- Begin immediately with the introduction to the content you read\n"
      "- Do not include preamble, meta-commentary, or phrases like 'Here is the script...'\n"
      "- Do not use first-person perspective ('I', 'my')\n"
      "- Use continuous paragraphs, without headings or lists\n\n"
      "SCRIPT STRUCTURE:\n"
      "1. Introduce the content by stating its source, title, and nature\n"
      "2. Analyze the conversation's sentiment and themes from what you read\n"
      "3. Provide deeper analysis, synthesizing key insights, tradeoffs, and dissenting views from the actual content\n"
      "4. Conclude with a verbatim reading of the original post, cleaned for audio narration\n\n"
      "Important: Base everything on the actual content you fetch from the URL, not on assumptions or general knowledge.")})

(defn build-context
  "Return the runtime context given CLI `options`, applying any overrides we support."
  [options]
  (let [normalize-newlines (fn [s]
                             (some-> s
                                     (str/replace "\\n" "\n")))
        responses (cond-> default-responses
                    (:model options)           (assoc :model (:model options))
                    (:max-output-tokens options) (assoc :max-output-tokens
                                                   (:max-output-tokens options))
                    (:system-prompt options)   (assoc :system-prompt
                                                 (normalize-newlines
                                                   (:system-prompt options)))
                    (:prompt-template options) (assoc :prompt-template
                                                 (normalize-newlines
                                                   (:prompt-template
                                                     options))))]
    {:podcast podcast-details
     :responses responses,
     :options   options}))
