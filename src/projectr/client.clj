(ns projectr.client
  (:require [clojure.edn :as edn]
            [org.httpkit.client :as http]))

(defn swap-state!
  "Takes state atom and swaps in a new-state map. It carries volatile stuff with it."
  [state up-fn]
  (swap! state (fn [old] (assoc (up-fn old)
                          :volatile (old :volatile)
                          :hud (old :hud)))))

(defn move! [player]
  (http/post "http://localhost:31254/position" {:body player}))

(defn update! [state]
  (let [{:keys [status headers body error] :as resp} @(http/get "http://localhost:31254/state")
        new (edn/read-string (slurp body))]
    (swap-state! state (fn [old] new)))
  (Thread/sleep 300)
  (if (::run (:volatile @state))
    (recur state)))


(defn start [state]
  (swap! state #(assoc-in % [:volatile ::run] true))
  (.start (Thread. #(update! state))))

(defn stop [state]
  (swap! state #(assoc-in % [:volatile ::run] false)))
