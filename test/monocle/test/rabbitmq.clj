(ns monocle.test.rabbitmq
  (:use clojure.test)
  (:require [clojure.java.io :as io])
  (:use monocle.rabbitmq :reload)
  (:import (java.io StringReader)))

(def uri "amqp://localhost/test")

(def exch "test.exchange")

(def queue "test.foo")

(def _key "foo")

(defmacro with-rabbit [[ch cfg] & body]
  `(let [~ch (connect ~cfg)]
     (bind-queue ~ch ~exch ~queue ~_key)
     ~@body
     (delete-queue ~ch ~exch ~queue)
     (delete-exchange ~ch ~exch)))

(deftest ^{:integration true}
  t-publish
  (testing "make sure we have a connection"
    (with-rabbit [chan {:uri uri}]
      (is (= 2 (write-rabbitmq [chan exch _key]
                               (io/reader
                                (StringReader. "foo\nbar")) 1)))
      (is (= "foo\n" (get-rabbitmq [chan exch queue]))))))
