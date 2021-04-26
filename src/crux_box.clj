(ns crux-box
  (:require [clojure.java.io :as io]
            [crux.api :as crux]))

(defn start-crux! []
  (letfn [(kv-store [dir]
            {:kv-store {:crux/module 'crux.rocksdb/->kv-store
                        :db-dir      (io/file dir)
                        :sync?       true}})]
    (crux/start-node
      {:crux/tx-log             (kv-store "/home/justin/Desktop/tx-log")
       :crux/document-store     (kv-store "/home/justin/Desktop/doc-store")
       :crux/index-store        (kv-store "/home/justin/Desktop/index-store")
       :crux.http-server/server {:port 3000}})))

(def crux-node (start-crux!))

(defn stop-crux! []
  (.close crux-node))
