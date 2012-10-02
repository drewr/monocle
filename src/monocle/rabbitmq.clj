(ns monocle.rabbitmq
  (:require [clojure.java.io :as io])
  (:import (com.rabbitmq.client AMQP$Channel
                                AMQP$BasicProperties$Builder
                                ConnectionFactory)
           (java.security KeyStore)
           (javax.net.ssl SSLContext
                          KeyManagerFactory
                          TrustManagerFactory)))

(defn sslctx [{:keys [client clientpw trust trustpw]}]
  (let [ks (KeyStore/getInstance "JKS")
        kmf (KeyManagerFactory/getInstance "SunX509")
        tks (KeyStore/getInstance "JKS")
        tmf (TrustManagerFactory/getInstance "SunX509")]
    (.load ks (io/input-stream client) (char-array clientpw))
    (.init kmf ks (char-array clientpw))
    (.load tks (io/input-stream trust) (char-array trustpw))
    (.init tmf tks)
    (doto (SSLContext/getInstance "SSLv3")
      (.init (.getKeyManagers kmf)
             (.getTrustManagers tmf) nil))))

(defn make-connection [cfg]
  (let [hb ConnectionFactory/DEFAULT_HEARTBEAT
        to ConnectionFactory/DEFAULT_CONNECTION_TIMEOUT
        f (doto (ConnectionFactory.)
            (.setUri (:uri cfg))
            (.setConnectionTimeout (:timeout cfg 10000))
            (.setRequestedHeartbeat (:heartbeat cfg hb)))]
    (when (.isSSL f)
      (.useSslProtocol f (sslctx (:ssl cfg))))
    (.newConnection f)))

(defmacro with-rabbit [[ch cfg] & body]
  `(with-open [conn# (make-connection ~cfg)
               ~ch (.createChannel conn#)]
     ~@body))

(defn bind-queue [chan exch queue key]
  (.exchangeDeclare chan exch "direct"
                    #_durable true
                    #_auto-del false
                    #_internal false {})
  (.queueDeclare chan queue
                 #_durable true
                 #_exclusive false
                 #_auto-del false {})
  (.queueBind chan queue exch key))

(defn delete-exchange [chan exch]
  (.exchangeDelete chan exch))

(defn delete-queue [chan exch queue]
  (.queueDelete chan queue))

(defn write-chan [[chan exch key] reader batch]
  (count
   (for [b (partition-all batch (line-seq reader))]
     (let [payload (str (apply str (interpose "\n" b)) "\n")]
       (.basicPublish chan exch key
                      (.build (AMQP$BasicProperties$Builder.))
                      (.getBytes payload))))))

(defn write-rabbitmq [[uri exch key] reader batch opts]
  (with-rabbit [chan (assoc {:uri uri
                             :timeout 5000}
                       :ssl (:ssl opts))]
    (bind-queue chan exch key key)
    (write-chan [chan exch key] reader batch)))

(defn get-rabbitmq [[chan exch queue]]
  (String. (.getBody (.basicGet chan queue true)) "utf-8"))
