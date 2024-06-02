(ns rimworld
 (:require [clojure.java.io :as io]
           [clojure.data.xml :as xml]
           [com.rpl.specter :as sp]))

(def path "/Users/justingood/Library/Application Support/RimWorld/Saves/Northern Binda (Permadeath).rws")

(with-open [rdr (io/reader path)]
  #_
  (let [lines (line-seq rdr)]
    (doseq [line (take 5 lines)]
      (println line)))

  #_ ;; Errors
  (xml/parse rdr)

  )

;; Something about the first character being a BOM
#_
(with-open [rdr (io/reader path)]
  (let [first-line (first (line-seq rdr))]
    (println "First line:" first-line)
    (println "First line bytes:" (map int (seq first-line)))))

#_ ;; Too big to print
(with-open [rdr (io/reader path :encoding "UTF-8")]
  (let [content (slurp rdr)
        cleaned-content (if (= (first content) \ufeff)
                          (subs content 1)
                          content)]
    (let [input-stream (io/input-stream (java.io.ByteArrayInputStream. (.getBytes cleaned-content "UTF-8")))]
      (with-open [is input-stream]
        (let [parsed-xml (xml/parse is)]
          (println "Parsed XML:" parsed-xml))))))

#_;; Getting somewhere
(with-open [rdr (io/reader path :encoding "UTF-8")]
  (let [content (slurp rdr)
        cleaned-content (if (= (first content) \ufeff)
                          (subs content 1)
                          content)]
    (with-open [input-stream (java.io.ByteArrayInputStream. (.getBytes cleaned-content "UTF-8"))]
      (let [parsed-xml (xml/parse input-stream)]
        (->> parsed-xml
             (sp/select [:content sp/ALL
                         #(-> % :tag (= :game))
                         :content sp/ALL
                         #(-> % :tag (= :playLog))
                         :content sp/ALL
                         :tag
                         ]))
        ))))

(defn tags-path
  "Creates a Specter path for navigating nested XML tags."
  [tags]
  (->> tags
       (map (fn [tag] [:content sp/ALL #(= (:tag %) tag)]))
       (apply concat)))

(defn select-tags
  "Selects elements from XML based on a sequence of nested tags."
  [xml tags]
  (sp/select (concat (tags-path tags) [:content sp/ALL #_:tag]) xml))

(with-open [rdr (io/reader path :encoding "UTF-8")]
  (let [content (slurp rdr)
        cleaned-content (if (= (first content) \ufeff)
                          (subs content 1)
                          content)]
    (with-open [input-stream (java.io.ByteArrayInputStream. (.getBytes cleaned-content "UTF-8"))]
      (let [parsed-xml (xml/parse input-stream)]
        (->> #_(select-tags parsed-xml [:game :taleManager :tales])
             (select-tags parsed-xml [:game :playLog :entries])
             (shuffle)
             (take 3))
        ))))
