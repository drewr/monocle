(ns monocle.rabbitmq-test
  (:use clojure.test)
  (:require [clojure.java.io :as io])
  (:use monocle.rabbitmq :reload)
  (:import (java.io StringReader)))

(def uri "amqp://localhost/test")

(def exch "test.exchange")

(def queue "test.foo")

(def _key "foo")

(defmacro with-rabbit [body]
  `(do
     (bind-queue uri ~exch ~queue ~_key)
     ~body
     (delete-queue ~uri ~exch ~queue)
     (delete-exchange ~uri ~exch))  )

(deftest ^{:integration true}
  t-publish
  (testing "make sure we have a connection"
    (with-rabbit
      (is (= 2 (write-rabbitmq [uri exch _key]
                               (io/reader
                                (StringReader. "foo\nbar")) 1))))))
