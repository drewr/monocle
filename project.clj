(defproject com.draines/monocle "0.1.0-SNAPSHOT"
  :description "Monocle"
  :url "http://github.com/drewr/monocle"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/tools.cli "0.2.2"]
                 [org.clojure/tools.logging "0.2.3"]
                 [log4j/log4j "1.2.17"]
                 [commons-io "2.4"]
                 [com.mefesto/wabbitmq "0.2.2"]
                 [cheshire "4.0.1"]]
  :main monocle.core
  :test-selectors {:default  #(not (:integration %))
                   :integration :integration
                   :all (constantly true)})
