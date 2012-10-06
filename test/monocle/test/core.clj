(ns monocle.test.core
  (:use clojure.test)
  (:use monocle.core :reload-all)
  (:require [clojure.java.io :as io])
  (:import (java.io File)))

(def tmp nil)

(use-fixtures :each
  (fn [t]
    (with-redefs [tmp (File/createTempFile "__monocle-" ".offset")]
      (t)
      (.delete tmp))))

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
  (testing "should publish offset after reading"
    (let [out (with-out-str
                (main {:file (.getPath (io/resource "log.txt"))
                       :offset-file (str tmp)
                       :interface "stdout"
                       :batch 3
                       :reset true}))]
      (is (.startsWith out "{\"time"))
      (is (= 3 (count (re-seq #"-- \n" out))))))
  (testing "should publish offset after reading"
    (let [out (with-out-str
                (main {:file (.getPath (io/resource "log.txt"))
                       :offset-file (str tmp)
                       :interface "stdout"
                       :batch 3
                       :reset false}))]
      (is (.startsWith out "{\"time"))
      (is (= 3 (count (re-seq #"-- \n" out))))))
  (testing "should publish offset after reading"
    (let [out (with-out-str
                (main {:file (.getPath (io/resource "log.txt"))
                       :offset-file (str tmp)
                       :interface "stdout"
                       :batch 3
                       :reset false}))]
      (is (not (.startsWith out "{\"time")))
      (is (= 0 (count (re-seq #"-- \n" out)))))))

(deftest bulk
  (testing "should add bulk format"
    (let [out (with-out-str
                (main {:file (.getPath (io/resource "bulk.txt"))
                       :offset-file (str tmp)
                       :interface "stdout"
                       :batch 1
                       :reset false
                       :index "foo"
                       :type "t"}))
          ans (slurp (io/resource "bulk-answer.txt"))]
      (is (= out ans))
      (is (= 2 (count (re-seq #"-- \n" out)))))))
