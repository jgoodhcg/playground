(ns podcast.config
  "Provides default configuration and builds response settings from CLI options."
  (:require [clojure.string :as str]))

(def ^:private default-responses
  {:model "gpt-4.1"
   :max-output-tokens 6000
   :system-prompt
   (str "You are an AI summarizer creating spoken transcripts for me to listen to in AudioRead. "
        "Maintain a neutral, thoughtful narrator voice and keep the listener oriented while flowing naturally for audio. "
        "If you lack direct access to the source material, use the web_search tool to retrieve it—start by opening the provided URL and its related discussion before broadening out. "
        "Cite important sources inline when you reference them.")
   :prompt-template
   (str "You are preparing a long-form narration for the listener based on {{url}}.\n"
        "Write in continuous paragraphs with no headings, bullets, or list markers.\n"
        "Open by identifying the source (Hacker News, Reddit, blog, etc.), the title if known, and whether the discussion has many, few, or typical comments.\n"
        "Front-load the most important insights, stakes, and why the listener should care before diving deeper.\n"
        "Summarize the linked content thoroughly, weaving in extended verbatim excerpts whenever available.\n"
        "Synthesize the top-level comments by clustering themes and highlighting different perspectives using neutral attributions such as 'one comment notes'. Expand on context, implications, and tradeoffs.\n"
        "Convert links or code into clear plain-language descriptions and remove HTML artifacts.\n"
        "If the source text cannot be accessed, rely on the discussion and relevant background knowledge.\n"
        "After the summaries and commentary, transition into a more verbatim or nuanced walk-through of the original post so the listener can hear it nearly in full.\n"
        "Be expansive—aim for up to 10,000 characters—prioritizing new information over repetition.\n"
        "Relate points to prior interests of the listener when appropriate without inventing personal anecdotes.\n"
        "Conclude with a single-sentence TL;DR that cleanly captures the takeaway."
        " Ensure any referenced facts mention their source inline when possible." )})

(defn build-context
  "Return the runtime context given CLI `options`, applying any overrides we support."
  [options]
  (let [normalize-newlines (fn [s] (some-> s (str/replace "\\n" "\n")))
        responses (cond-> default-responses
                      (:model options) (assoc :model (:model options))
                      (:max-output-tokens options) (assoc :max-output-tokens (:max-output-tokens options))
                      (:system-prompt options) (assoc :system-prompt (normalize-newlines (:system-prompt options)))
                      (:prompt-template options) (assoc :prompt-template (normalize-newlines (:prompt-template options))))]
    {:responses responses
     :options   options}))
