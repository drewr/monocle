(ns monocle.rabbitmq
  (:require [langohr.core :as lhc]
            [langohr.exchange :as lhe]
            [langohr.queue :as lhq]
            [langohr.consumers :as lhcons]
            [langohr.basic :as lhb])
  (:import (com.rabbitmq.client AMQP$Channel)))

(defmacro with-rabbit [[ch cfg] & body]
  `(with-open [conn# (lhc/connect ~cfg)
               ~ch (lhc/create-channel conn#)]
     ~@body))

(defn bind-queue [chan exch queue key]
  (lhe/declare chan exch "direct")
  (lhq/declare chan queue :exclusive false)
  (lhq/bind chan queue exch :routing-key key))

(defn delete-exchange [chan exch]
  (lhe/delete chan exch))

(defn delete-queue [chan exch queue]
  (lhq/delete chan queue))

(defn write-chan [[chan exch key] reader batch]
  (count
   (for [b (partition-all batch (line-seq reader))]
     (let [payload (str (apply str (interpose "\n" b)) "\n")]
       (lhb/publish chan exch key (.getBytes payload))))))

(defn write-rabbitmq [[uri exch key] reader batch]
  (with-rabbit [chan {:uri uri
                      :connection-timeout 5000}]
    (write-chan [chan exch key] reader batch)))

(defn get-rabbitmq [[chan exch queue]]
  (String. (.getBody (lhb/get chan queue)) "utf-8"))

