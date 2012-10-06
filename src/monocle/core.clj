(ns monocle.core
  (:gen-class)
  (:require [clojure.tools.cli :as cli]
            [clojure.tools.logging :as log]
            [cheshire.core :as json]
            [monocle.io :as io]
            [monocle.rabbitmq :as rmq])
  (:use [monocle.io :only [with-offset]]))

(def options
  [["-i" "--interface"
    (str "Where to send output (stdout,\n"
         "                                "
         " stderr, amqps?://HOST:PORT/VHOST|EXCH|KEY)")
    :default "stdout"]
   ["-f" "--file" "File to monitor for content"]
   ["-o" "--offset-file" "File with offset information"]
   ["-b" "--batch" "Size of batches from FILE"
    :default 25 :parse-fn #(Integer. %)]
   ["-r" "--reset" "Don't write a new offset" :default false :flag true]
   ["--client" "Client keystore"]
   ["--clientpw" "Client keystore password"]
   ["--trust" "Trust keystore"]
   ["--trustpw" "Trust keystore password"]
   ["--index" (str "Format the batches in elasticsearch's bulk\n"
                   "                                "
                   " format.  Also need --type.")]
   ["--type" "The type to provide to elasticsearch"]
   ["-h" "--help" "Help!" :default false :flag true]])

(defn amqp-opts [iface]
  (.split iface "\\|"))

(defn whichfn [iface]
  (condp #(.startsWith %2 %1) iface
    "stdout" io/write-stdout
    "stderr" io/write-stderr
    "amqp" (partial rmq/write-rabbitmq (amqp-opts iface))
    (throw
     (Exception.
      (format "invalid output interface: %s" iface)))))

(defn send-iface [iface batches opts]
  ((whichfn iface) batches opts))

(defn part [rdr {:keys [batch index type]}]
  (if (and index type)
    (let [bulk-header (json/encode {:index {:_index index :_type type}})]
      (partition-all (* batch 2) (interleave (repeat bulk-header)
                                             (line-seq rdr))))
    (partition-all batch (line-seq rdr))))

(defn main [{:keys [file offset-file interface batch reset] :as opts}]
  (with-offset [offset offset-file set-new-offset!]
    (let [[stream reader] (io/counting-stream-reader file offset)
          c (.getCount stream)
          linecount (send-iface interface (part reader opts) opts)]
      (log/debugf "starting %s at %d" file c)
      (let [c2 (.getCount stream)]
        (log/debugf "read %d lines %d bytes"
                    (or linecount 0)
                    (- c2 c))
        (when-not reset
          (set-new-offset! c2))))))

(defn delete-trailing-whitespace [s]
  (.replaceAll s "[ ]+\n" "\n"))

(defn mixmatch [& args]
  (= 2 (count (set (map boolean args)))))

(defn exit [cont msg]
  (println msg)
  (reset! cont false))

(defn fail [cont err]
  (log/fatalf err)
  (reset! cont false))

(defn -main [& args]
  (let [[{:keys [interface file offset-file
                 batch help index type] :as opts} args helpstr]
        (apply cli/cli args options)
        opts (assoc opts :ssl (select-keys opts [:client :clientpw
                                                 :trust :trustpw]))
        continue? (atom true)]
    (when-not offset-file
      (fail continue? "-o not supplied"))
    (when-not file
      (fail continue? "-f not supplied"))
    (when (mixmatch index type)
      (fail continue? "supply both --index and --type if you want ES bulk"))
    (when help
      (exit continue? (delete-trailing-whitespace helpstr)))
    (when @continue?
      (main opts))))
