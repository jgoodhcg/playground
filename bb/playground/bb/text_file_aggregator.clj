(ns playground.bb.text-file-aggregator
  (:require [babashka.fs :as fs]
            [babashka.cli :as cli]
            [clojure.java.io :as io]))

(defn text-file? [file]
  (let [name (str file)]
    (not (re-find #"\.(exe|png|jpg|jpeg|gif|bmp|pdf)$" name))))

(def delim "------------ ")

(def preamble (str "This is a single text file representing a code base."
                   "All files are prefixed with a line like the following: \n"
                   delim "/relative/path/to/file\n"))

(defn concat-text-files [dir output-file]
  (with-open [out-writer (io/writer output-file)]
    (.write out-writer preamble)
    (doseq [file (file-seq (fs/file dir))]
      (when (and (not (fs/directory? file)) (text-file? file))
        (let [p (fs/relativize dir file)]
          (with-open [in-reader (io/reader file)]
            (.write out-writer (str delim p "\n"))
            (io/copy in-reader out-writer)
            (.write out-writer "\n")))))))

(defn -main [& args]
  (let [opts (cli/parse-opts args {:opts {:directory :string, :output :string}})
        directory (:directory opts)
        output-file (:output opts)]
    (if (and directory output-file)
      (concat-text-files directory output-file)
      (println "Usage: bb aggregate.clj --directory <dir> --output <output-file>"))))

;; Uncomment the following line to make it executable as a script
;; (-main *command-line-args*)
