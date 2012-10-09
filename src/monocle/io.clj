(ns monocle.io
  (:require [clojure.java.io :as io])
  (:import (java.io File RandomAccessFile)
           (org.apache.commons.io.input CountingInputStream)))

(defn raf [f]
  (if (or (instance? File f)
          (string? f))
    (RandomAccessFile. f "rw")
    f))

(defn slurp-offset [f]
  (let [f (raf f)]
    (try
      (.readLong f)
      (catch Exception _
        0))))

(defn spit-offset [f n]
  (let [f (raf f)]
    (.seek f 0)
    (.writeLong f n)))

(defn counting-stream-reader
  ([file]
     (counting-stream-reader file 0))
  ([file offset]
     (let [f (io/file file)
           cis (CountingInputStream. (io/input-stream f))]
       (when (<= offset (.length f))
         (.skip cis offset))
       [cis (io/reader cis)])))

(defmacro with-offset
  "This is only intended for use across JVMs!  Provides no protection
  against other threads."
  [bindings & body]
  `(let [raf# (raf ~(second bindings))
         lock# (-> raf# .getChannel .lock)
         ~(first bindings) (slurp-offset raf#)
         ~(last bindings) (fn [n#] (spit-offset raf# n#))]
     (try
       ~@body
       (finally
         (.release lock#)
         (.close raf#)))))

(defn write-printer [pfn batches]
  (count
   (apply concat
          (for [b (interpose ["-- "] batches)
                l b]
            (pfn l)))))

(defn write-stdout [batches _]
  (write-printer println batches))

(defn write-stderr [batches _]
  (write-printer #(binding [*out* *err*] (println %)) batches))
