(ns monocle.rabbitmq
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
  (mq/with-broker {:uri uri}
    (mq/with-channel
      (mq/with-exchange {:name exch}
        (count
         (for [b (partition-all batch (line-seq reader))]
           (let [payload (str (apply str (interpose "\n" b)) "\n")]
             (mq/publish key (.getBytes payload)))))))))
