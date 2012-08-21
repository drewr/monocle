(ns monocle.rabbitmq-test
  (:use clojure.test)
  (:require [clojure.java.io :as io])
  (:use monocle.rabbitmq :reload)
  (:import (java.io StringReader)))

(deftest ^{:integration true}
  t-publish
  (testing "make sure we have a connection"
    (is (= 2 (write-rabbitmq ["amqp://localhost/test"
                              "test.exchange"
                              "foo"]
                             (io/reader
                              (StringReader. "foo\nbar")) 1)))))
