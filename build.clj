(ns build
  (:require [org.corfield.build :as bb]))

(def lib 'brdloush/zonky-homework)
(def version "1.0.0-SNAPSHOT")

(defn ci "Run the CI pipeline of tests (and build the uberjar)." [opts]
  (-> opts
      (assoc :lib lib :version version :main 'brdloush.zonky-homework.core)
      (bb/clean)
      (bb/uber)))