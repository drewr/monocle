(ns monocle.io
  (:require [clojure.java.io :as io])
  (:import (java.io RandomAccessFile)
           (org.apache.commons.io.input CountingInputStream)))

(defn slurp-offset [f]
  (try
    (.readLong f)
    (catch Exception _
      0)))

(defn spit-offset [f n]
  (.seek f 0)
  (.writeLong f n))

(defn counting-stream-reader
  ([file]
     (counting-stream-reader file 0))
  ([file offset]
     (let [f (io/file file)
           cis (CountingInputStream. (io/input-stream f))]
       (.skip cis offset)
       [cis (io/reader cis)])))

(defmacro with-offset
  "This is only intended for use across JVMs!  Provides no protection
  against other threads."
  [bindings & body]
  `(let [raf# (RandomAccessFile. ~(second bindings) "rw")
         lock# (-> raf# .getChannel .lock)
         ~(first bindings) (slurp-offset raf#)
         ~(last bindings) (fn [n#] (spit-offset raf# n#))]
     (try
       ~@body
       (finally
         (.release lock#)
         (.close raf#)))))

(defn write-printer [pfn reader batch]
  (count
   (apply concat
          (for [b (interpose ["-- "]
                             (partition-all batch (line-seq reader)))
                l b]
            (pfn l)))))

(defn write-stdout [reader batch _]
  (write-printer println reader batch))

(defn write-stderr [reader batch _]
  (write-printer #(binding [*out* *err*] (println %)) reader batch))
