(ns podcast.pipeline
  "Coordinates the end-to-end flow from response generation to feed updates."
  (:require
    [podcast.responses :as responses]
    [podcast.audio     :as audio]
    [podcast.config    :as config]
    [podcast.feed      :as feed]
    [podcast.publish   :as publish]
    [podcast.storage   :as storage]))

(defn execute!
  "Execute the narration pipeline using the parsed CLI `options`."
  [options]
  (println "[pipeline] Building execution context")
  (let [context        (config/build-context options)
        response-model (get-in context [:responses :model])]
    (println "[pipeline] Generating narration with model" response-model)
    (let [script         (responses/generate context)
          _              (println "[pipeline] Response script received")
          staged-episode (do (println "[pipeline] Staging episode artifacts")
                             (storage/stage-episode! context script))
          audio          (do (println "[pipeline] Synthesizing audio")
                             (audio/synthesize! context staged-episode))
          feed           (do (println "[pipeline] Updating RSS feed")
                             (feed/update-feed! context audio))
          publish-result (when (get-in context [:options :publish])
                           (println "[pipeline] Uploading assets via publish module")
                           (publish/upload! context feed))]
      {:config (:responses context)
       :script script
       :episode staged-episode
       :audio audio
       :feed feed
       :publish publish-result})))
