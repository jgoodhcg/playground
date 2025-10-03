(ns podcast.pipeline
  "Coordinates the end-to-end flow from response generation to feed updates."
  (:require
    [podcast.responses :as responses]
    [podcast.audio     :as audio]
    [podcast.config    :as config]
    [podcast.feed      :as feed]
    [podcast.publish   :as publish]
    [podcast.storage   :as storage]))

(def stage-order
  [:generate-script :stage-script :synthesize-audio :merge-audio :update-feed :publish])

(defn- stage-index [stage]
  (.indexOf stage-order stage))

(defn- should-run?
  [stage start-stage]
  (let [idx (stage-index stage)
        start-idx (stage-index start-stage)]
    (and (not= idx -1)
         (not= start-idx -1)
         (>= idx start-idx))))

(defn execute!
  "Execute the narration pipeline using the parsed CLI `options`."
  [options]
  (println "[pipeline] Building execution context")
  (let [context     (config/build-context options)
        start-stage (or (:start-stage options) :generate-script)
        response-model (get-in context [:responses :model])
        state0 (cond-> {:script nil :episode nil :audio nil}
                 (:episode-dir options) (merge (storage/load-episode-state (:episode-dir options))))]
    (println "[pipeline] Start stage:" (name start-stage))
    (when (:episode state0)
      (println "[pipeline] Loaded existing episode metadata."))
    (let [final-state
          (reduce
            (fn [state stage]
              (if-not (should-run? stage start-stage)
                state
                (case stage
                  :generate-script
                  (if (:script state)
                    (do (println "[pipeline] Existing script detected; skipping generation")
                        state)
                    (do (println "[pipeline] Generating narration with model" response-model)
                        (assoc state :script (responses/generate context))))

                  :stage-script
                  (if (:episode state)
                    (do (println "[pipeline] Episode already staged; skipping") state)
                    (let [script-data (:script state)]
                      (if-not script-data
                        (throw (ex-info "Cannot stage episode without script data. Provide --episode-dir or run generate-script." {:stage stage}))
                        (do (println "[pipeline] Staging episode artifacts")
                            (let [result (storage/stage-episode! context script-data)]
                              (-> state
                                  (assoc :script (dissoc result :episode))
                                  (assoc :episode (:episode result))))))))

                  :synthesize-audio
                  (let [episode-data (:episode state)]
                    (if (and (:audio state)
                             (= :audio-generated (:status (:audio state))))
                      (do (println "[pipeline] Audio already generated; skipping") state)
                      (if-not episode-data
                        (do (println "[pipeline] Episode not staged; skipping audio") state)
                        (let [audio-input (if-let [script (:script state)]
                                            (assoc script :episode episode-data)
                                            {:episode episode-data})
                              audio-result (audio/synthesize! context audio-input)]
                          (println "[pipeline] Synthesizing audio")
                          (-> state
                              (assoc :audio (:audio audio-result))
                              (assoc :episode (:episode audio-result)))))))

                  :merge-audio
                  (let [episode-data (:episode state)]
                    (if-not episode-data
                      (do (println "[pipeline] Episode not staged; skipping audio merge") state)
                      (do (println "[pipeline] Merging audio chunks")
                          (let [merge-result (audio/merge-audio! context state)]
                            (-> state
                                (assoc :audio (:audio merge-result))
                                (assoc :episode (:episode merge-result)))))))

                  :update-feed
                  (do (println "[pipeline] Updating RSS feed")
                      (assoc state :feed (feed/update-feed! context state)))

                  :publish
                  (if (get-in context [:options :publish])
                    (do (println "[pipeline] Uploading assets via publish module")
                        (assoc state :publish (publish/upload! context state)))
                    state)

                  state)))
            state0
            stage-order)]
      {:config (:responses context)
       :script (:script final-state)
       :episode {:episode (:episode final-state)}
       :audio (:audio final-state)
       :feed (:feed final-state)
       :publish (:publish final-state)})))
