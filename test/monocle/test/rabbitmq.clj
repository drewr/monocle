(ns monocle.test.rabbitmq
  (:use clojure.test)
  (:require [clojure.java.io :as io])
  (:use monocle.rabbitmq :reload)
  (:import (java.io StringReader)
           (com.rabbitmq.client AMQP$Queue$BindOk)))

(def uri "amqp://localhost")

(def ssluri "amqps://localhost")

(def exch "test.direct")

(def queue "test.foo")

(def _key "test.foo")

(defmacro with-test-rabbit [[ch cfg] & body]
  `(with-rabbit [~ch ~cfg]
     (bind-queue ~ch ~exch ~queue ~_key)
     ~@body
     (delete-queue ~ch ~exch ~queue)
     (delete-exchange ~ch ~exch)))

(deftest ^{:integration true}
  t-publish-clear
  (testing "main interface"
    (write-rabbitmq [uri exch queue]
                    (partition-all
                     1 (line-seq
                        (io/reader
                         (StringReader. "foo\nbar"))))
                    {}))
  (testing "lower level interface"
    (with-test-rabbit [chan {:uri uri}]
      (is (= 2 (write-chan [chan exch _key]
                           (partition-all
                            1 (line-seq
                               (io/reader
                                (StringReader. "foo\nbar")))))))
      (is (= "foo\n" (get-rabbitmq [chan exch queue]))))))

(deftest ^{:integration true}
  t-publish-ssl
  (with-test-rabbit [chan {:uri ssluri
                           :ssl {:client "test/ssl/client.jks"
                                 :clientpw "michaelbolton"
                                 :trust "test/ssl/trust.jks"
                                 :trustpw "michaelbolton"}}]
    (is (= 2 (write-chan [chan exch _key]
                         (partition-all
                          1
                          (line-seq
                           (io/reader
                            (StringReader. "foo\nbar")))))))
    (is (= "foo\n" (get-rabbitmq [chan exch queue])))))

(deftest ^{:integration true}
  t-flush-queue
  (with-test-rabbit [chan {:uri uri}]
    (is (instance? AMQP$Queue$BindOk (flush-queue chan exch queue)))
    (is (nil? (get-rabbitmq [chan exch queue])))))
