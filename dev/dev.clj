(ns dev
  (:use [projectr.core :only [state-demo1 swap-state! start stop]])
  (:require [projectr.simulation :as sim]
            [projectr.geometry :as geo]))

(def test-state (atom state-demo1))
                                        ; start rendering
(start test-state)
                                        ; control (pure) simulation
(sim/start test-state)
(sim/stop test-state)

                                        ; Change game-state safely
(swap-state! test-state (fn [old] state-demo1))
                                        ; the static part of libgdx seems to be stateful, sometimes it breaks
                                        ; and it leaks! also this makes the whole system a singleton instance :-(
(stop test-state)



(defn mfn [x]
  (+ x 4))
