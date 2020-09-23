(ns hello-world.core
  (:gen-class)
  #_(:require [clojure.pprint :refer [pprint]]) ;; => 29015008 binary size on macOS
  (:require [babashka.pprint :refer [pprint #_cl-format]]))

(defn -main [& _args]
  (pprint (zipmap (map (comp keyword str char) (range 97 117)) (range 20)))
  #_(println (cl-format nil "Pad with leading zeros ~5,'0d" 3)))
