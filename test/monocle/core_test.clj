(ns monocle.core-test
  (:use clojure.test)
  (:use monocle.core :reload-all))

(deftest args
  (testing "testing opts"
    (with-redefs [main (fn [& args] :main)]
      (is (= :main (-main "-o" "foo" "-f" "bar")))
      (is (not (-main))))))

(deftest amqp
  (testing "amqp uri string"
    (is (= ["amqp://localhost/test" "test.exchange" "foo"]
           (seq (amqp-opts "amqp://localhost/test|test.exchange|foo"))))))
