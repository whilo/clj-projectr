(ns projectr.server
  (:use [clojure.core.incubator :only [dissoc-in]]
        ring.adapter.jetty
        ring.middleware.file
        ring.middleware.file-info
        ring.middleware.format-response
        [ring.util.response :exclude (not-found)]
        compojure.core
        compojure.route)
  (:require [clojure.edn :as edn]))

(defn update-player [old player]
  (let [[name entry] player]
    (assoc-in old [:players name] entry)))

(defn update-position! [state request]
  (println (edn/read-string (slurp (:body request))))
  #_(let [[player] (edn/read-string (slurp (:body request)))]
      (swap! state update-player player)))

(defn start [state]
  (let [handler (routes (GET "/state" [] (response (dissoc @state :volatile :hud)))
                        (POST "/position" request #(update-position! state %))
                        (not-found "<h1>404 Page not found</h1>"))
        app (-> handler wrap-clojure-response)
        server (run-jetty app {:port 31254 :join? false})]
    (swap! state #(assoc-in % [:volatile ::server] server))

    (.start server)))


(defn stop [state]
  (let [{{server ::server} :volatile} @state]
    (.stop server)
    (swap! state #(dissoc-in % [:volatile ::server]))))
