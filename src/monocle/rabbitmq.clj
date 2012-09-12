(ns monocle.rabbitmq
  (:require [langohr.core :refer [connect create-channel]]
            [langohr.exchange :as lexch]
            [langohr.basic :as l])
  (:require [com.mefesto.wabbitmq :as mq]))

(defn bind-queue [uri exch queue key]
  (mq/with-broker {:uri uri}
    (mq/with-channel
      (mq/exchange-declare exch "direct")
      (mq/queue-declare queue)
      (mq/queue-bind queue exch key))))

(defn delete-exchange [uri exch]
  (mq/with-broker {:uri uri}
    (mq/with-channel
      (mq/exchange-delete exch))))

(defn delete-queue [uri exch queue]
  (mq/with-broker {:uri uri}
    (mq/with-channel
      (mq/queue-delete queue))))

(defn write-rabbitmq [[uri exch key] reader batch]
  (let [conn (connect {:uri uri
                       :connection-timeout 3000})
        chan (create-channel conn)]
    (count
     (for [b (partition-all batch (line-seq reader))]
       (let [payload (str (apply str (interpose "\n" b)) "\n")]
         (l/publish chan exch key (.getBytes payload)))))))

(defn get-rabbitmq [[uri exch queue]]
  (let [conn (connect {:uri uri})
        chan (create-channel conn)]
    (String. (.getBody (l/get chan queue)) "utf-8")))
