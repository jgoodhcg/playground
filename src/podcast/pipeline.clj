(ns podcast.pipeline
  "Coordinates the end-to-end flow from assistant generation to feed updates."
  (:require
    [podcast.assistant :as assistant]
    [podcast.audio     :as audio]
    [podcast.config    :as config]
    [podcast.feed      :as feed]
    [podcast.publish   :as publish]
    [podcast.storage   :as storage]))

(defn execute!
  "Execute the narration pipeline using the parsed CLI `options`.

  Currently stubs out each stage so we can fill them in incrementally."
  [options]
  (println "[pipeline] Loading configuration")
  (let [config         (config/load-config (:config options))
        context        (merge config {:options options})]
    (println "[pipeline] Calling assistant preset" (get-in context [:options :prompt-preset]))
    (let [script         (assistant/generate context)
          _              (println "[pipeline] Assistant script received")
          staged-episode (do (println "[pipeline] Staging episode artifacts")
                             (storage/stage-episode! context script))
          audio          (do (println "[pipeline] Synthesizing audio")
                             (audio/synthesize! context staged-episode))
          feed           (do (println "[pipeline] Updating RSS feed")
                             (feed/update-feed! context audio))
          publish-result (when (get-in context [:options :publish])
                           (println "[pipeline] Uploading assets via publish module")
                           (publish/upload! context feed))]
    {:config config
     :script script
     :episode staged-episode
     :audio audio
     :feed feed
     :publish publish-result})))
