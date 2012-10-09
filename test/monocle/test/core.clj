(ns monocle.test.core
  (:use clojure.test)
  (:use monocle.core :reload-all)
  (:require [clojure.java.io :as jio]
            [monocle.io :as io])
  (:import (java.io File)))

(def ofile nil)

(use-fixtures :each
  (fn [t]
    (with-redefs [ofile (File/createTempFile "__monocle-" ".offset")]
      (t)
      (.delete ofile))))

(deftest args
  (testing "testing opts"
    (with-redefs [main (fn [& args] :main)]
      (is (= :main (-main "-o" "foo" "-f" "bar")))
      (is (not (-main))))))

(deftest amqp
  (testing "amqp uri string"
    (is (= ["amqp://localhost/test" "test.exchange" "foo"]
           (seq (amqp-opts "amqp://localhost/test|test.exchange|foo"))))))

(deftest offset
  (testing "should NOT publish offset after reading"
    (let [out (with-out-str
                (main {:file (.getPath (jio/resource "log.txt"))
                       :offset-file ofile
                       :interface "stdout"
                       :batch 3
                       :reset true}))]
      (is (= 0 (io/slurp-offset ofile)))
      (is (.startsWith out "{\"time"))
      (is (= 3 (count (re-seq #"-- \n" out))))))
  (testing "should publish offset after reading"
    (let [out (with-out-str
                (main {:file (.getPath (jio/resource "log.txt"))
                       :offset-file ofile
                       :interface "stdout"
                       :batch 3
                       :reset false}))]
      (is (= (.length (jio/file (jio/resource "log.txt")))
             (io/slurp-offset ofile)))
      (is (.startsWith out "{\"time"))
      (is (= 3 (count (re-seq #"-- \n" out))))))
  (testing "should not do anything since last read was to the end of file"
    (let [out (with-out-str
                (main {:file (.getPath (jio/resource "log.txt"))
                       :offset-file ofile
                       :interface "stdout"
                       :batch 3
                       :reset false}))]
      (is (empty? out))))
  (testing "should reset offset when it extends past file length"
    (io/spit-offset ofile (inc (.length (jio/file (jio/resource "log.txt")))))
    (let [out (with-out-str
                (main {:file (.getPath (jio/resource "log.txt"))
                       :offset-file ofile
                       :interface "stdout"
                       :batch 3
                       :reset false}))]
      (is (.startsWith out "{\"time"))
      (is (= 3 (count (re-seq #"-- \n" out)))))))

(deftest bulk
  (testing "should add bulk format"
    (let [out (with-out-str
                (main {:file (.getPath (jio/resource "bulk.txt"))
                       :offset-file ofile
                       :interface "stdout"
                       :batch 1
                       :reset false
                       :index "foo"
                       :type "t"}))
          ans (slurp (jio/resource "bulk-answer.txt"))]
      (is (= out ans))
      (is (= 2 (count (re-seq #"-- \n" out)))))))
