(ns monocle.core
  (:gen-class)
  (:require [clojure.tools.cli :as cli]
            [clojure.tools.logging :as log]
            [monocle.io :as io]
            [monocle.rabbitmq :as rmq])
  (:use [monocle.io :only [with-offset]]))

(def options
  [["-i" "--interface"
    (str "Where to send output (stdout,\n"
         "                                "
         " stderr, amqp://HOST:PORT/VHOST|EXCH|KEY)")
    :default "stdout"]
   ["-f" "--file" "File to monitor for content"]
   ["-o" "--offset-file" "File with offset information"]
   ["-b" "--batch" "Size of batches from FILE"
    :default 25 :parse-fn #(Integer. %)]
   ["-h" "--help" "Help!" :default false :flag true]])

(defn amqp-opts [iface]
  (.split iface "\\|"))

(defn whichfn [iface]
  (condp #(.startsWith %2 %1) iface
    "stdout" io/write-stdout
    "stderr" io/write-stderr
    "amqp:" (partial rmq/write-rabbitmq (amqp-opts iface))
    (throw
     (Exception.
      (format "invalid output interface: %s" iface)))))

(defn send-iface [iface reader batch]
  ((whichfn iface) reader batch))

(defn main [{:keys [file offset-file interface batch]}]
  (with-offset [offset offset-file set-new-offset!]
    (let [[stream reader] (io/counting-stream-reader file offset)
          c (.getCount stream)
          linecount (send-iface interface reader batch)]
      (log/debugf "starting %s at %d" file c)
      (let [c2 (.getCount stream)]
        (log/debugf "read %d lines %d bytes"
                    (or linecount 0)
                    (- c2 c))
        (set-new-offset! c2)))))

(defn delete-trailing-whitespace [s]
  (.replaceAll s "[ ]+\n" "\n"))

(defn -main [& args]
  (let [[{:keys [interface file offset-file batch help] :as opts} args helpstr]
        (apply cli/cli args options)]
    (when-not offset-file
      (log/fatalf "-o not supplied" offset-file))
    (when-not file
      (log/fatalf "-f not supplied" file))
    (when help
      (println (delete-trailing-whitespace helpstr)))
    (when (and offset-file file)
      (main opts))))
