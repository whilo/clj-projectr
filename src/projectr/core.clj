(ns projectr.core
  (:use [projectr.world :only [load-buildings]]
        [projectr.render :only [as-game-coords init-render! dispose-render! draw-all! clear-plane! update-camera! to-flat-float-array]]
        [projectr.geometry :only [rand-coords-outside-building triangulate each-ear exclude each-triag]]
        [clojure.core.incubator :only [dissoc-in]])
  (:require [projectr.simulation :as sim]
            [projectr.server :as serv]
            [projectr.client :as cli])
  (:gen-class)
  (:import [com.badlogic.gdx.audio Sound Music]
           [com.badlogic.gdx ApplicationListener InputProcessor Gdx Input$Keys]
           [com.badlogic.gdx.backends.lwjgl LwjglApplication LwjglApplicationConfiguration]
           [com.badlogic.gdx.math MathUtils Rectangle Vector3]
           [com.badlogic.gdx.utils Array TimeUtils]))

; one can create random data with (re-rand #"REGEXP")

(defn spawn-emanation!
  "Create a new emanation at a random position."
  [state]
  (swap! state #(update-in % [:emanations] conj {:position (rand-coords-outside-building %)
                                                 :speed {:x 0 :y 0}
                                                 :inertia (+ 0.3 (* 0.7 (rand)))
                                                 :sprite "emanation.png"})))


(defn spawn-symbol!
  "Create a new symbol at first player's position."
  [state]
  (swap! state #(let [{:keys [players]} %
                      {player :tom} players
                      {:keys [position]} player]
                  (update-in % [:symbols] conj {:position position
                                                :inertia 2
                                                :sprite "water.png"}))))


; control first player
(defn- input-touch-move! [state]
  (if (.isTouched Gdx/input)
    (let [{:keys [volatile resolution]} @state
          width (resolution :x)
          height (resolution :y)
          touch-pos (Vector3.)
          x (.getX Gdx/input)
          y (.getY Gdx/input)]
      (.set touch-pos x y 0)
      #_(.unproject (volatile :camera) touch-pos)
      (swap! state (fn [old] (assoc-in old
                                      [:players :tom :position]
                                      {:x (/ x width)
                                       :y (- 1 (/ y height))}))))))


(defn- input-key-move! [state]
  (let [dt (.getDeltaTime Gdx/graphics)
        x-dir (* 0.2
                 (+ (if (.isKeyPressed Gdx/input Input$Keys/LEFT) (- dt) 0)
                    (if (.isKeyPressed Gdx/input Input$Keys/RIGHT) dt 0)))
        y-dir (* 0.2
                 (+ (if (.isKeyPressed Gdx/input Input$Keys/DOWN) (- dt) 0)
                    (if (.isKeyPressed Gdx/input Input$Keys/UP) dt 0)))]
    (cli/move! (:tom (:players (swap! state (fn [old]
                                              (-> old
                                                  (update-in [:players :tom :position :x] + x-dir)
                                                  (update-in [:players :tom :position :y] + y-dir)))))))))

(declare stop)
(defn- input-key! [state key]
  (cond (= Input$Keys/E key) (spawn-emanation! state)
        (= Input$Keys/S key) (spawn-symbol! state)
        (= Input$Keys/Q key) (if (.isKeyPressed Gdx/input Input$Keys/ALT_LEFT) (stop state) nil)
        :else nil))


(defn- libgdx-listener [state]
  (reify ApplicationListener
    (create [this]
      (init-render! state))
    (pause [this])
    (resume [this])
    (render [this]
      ; this guarantees continuous movement
      (input-key-move! state)
      (input-touch-move! state)

      (clear-plane!)
      (update-camera! state)
      (draw-all! state))
    (resize [this width height]
      (println "size change: " width " " height)
      (swap! state (fn [old] (assoc old :resolution {:x width :y height}))))
    (dispose [this]
      (dispose-render! state)
      (swap! state #(dissoc % :volatile))
      (System/exit 0))))


(defn- create-input-processor
  "Connects user input functions through a gdx wrapper."
  [state]
  (reify InputProcessor
    (keyDown [this keycode] (input-key! state keycode)
      true)
    (keyTyped [this character] true)
    (keyUp [this keycode] true)
    (mouseMoved [this screenX screenY] true)
    (scrolled [this amount] true)
    (touchDown [this screenX screenY pointer button] true)
    (touchDragged [this screenX screenY pointer] true)
    (touchUp [this screenX screenY pointer button] true)))


(defn start
  "Create a game."
  [state]
  (let [cfg (LwjglApplicationConfiguration.)
        res (:resolution @state)
        listener (libgdx-listener state)
        app (LwjglApplication. listener cfg)
        input (create-input-processor state)]
    (set! (.-title cfg) "ProjectR Simulation Client")
    (set! (.-width cfg) (res :x))
    (set! (.-height cfg) (res :y))
    (set! (.-forceExit cfg) false)
    (set! (.-fullscreen cfg) false)
    (.setInputProcessor Gdx/input input)
    (swap! state (fn [old] (assoc old :volatile
                                 (assoc (or (:volatile old) {})
                                   :app app
                                   :config cfg
                                   :input input))))))


(defn stop
  "Stop a running game and clean state map."
  [state]
  (let [volatile (:volatile @state)
        app (:app volatile)]
    (.exit app)
    (Thread/sleep 1000)
    (swap! state #(dissoc % :volatile))))


(defn swap-state!
  "Takes state atom and swaps in a new-state map. It carries volatile stuff with it."
  [state up-fn]
  (swap! state (fn [old] (assoc (up-fn old) :volatile (old :volatile)))))


(def state-demo1
  {:emanations [{:position {:x 0.8 :y 0.3}
                 :speed {:x 0 :y 0}
                 :force {:x 0 :y 0}
                 :inertia 0.5
                 :sprite "emanation.png"}
                {:position {:x 0.1 :y 0.9}
                 :speed {:x 0 :y 0}
                 :force {:x 0 :y 0}
                 :inertia 1
                 :sprite "emanation.png"}]
   :players {:tom {:position {:x 0.5 :y 0.5}
                   :speed {:x 1 :y 1}
                   :sprite "player.png"
                   :inertia 0.8}}
   :symbols [{:position {:x 0.1 :y 0.8}
              :inertia 1
              :sprite "water.png"}]
   :buildings (load-buildings)
   :hud (fn [{:keys [emanations players symbols buildings resolution]}]
          (str "Press s for new symbol\n"
               "Press e for new emanation\n"
               (count emanations) " emanations \n"
               (count players) " players \n"
               (count symbols) " symbols \n"
               (count buildings) " buildings \n"
               "resolution: " resolution))

   :resolution {:x 800 :y 480}})


(defn -main [& args]
  (let [state (atom state-demo1)]
    (start state)
    (sim/start state)))


(comment

  (def test-state (atom state-demo1))
                                        ; start rendering
  (start test-state)
                                        ; control (pure) simulation
  (sim/start test-state)
  (sim/stop test-state)

  (serv/start test-state)
  (serv/stop test-state)

  (cli/start test-state)
                                        ; Change game-state safely
  (swap-state! test-state (fn [old] state-demo1))
  (cli/move! (:tom (:players @test-state)))
  (org.httpkit.client/post "http://localhost:31254/position" {:body (:emanations @test-state)})

                                        ; the static part of libgdx seems to be stateful, sometimes it breaks
                                        ; and it leaks! also this makes the whole system a singleton instance :-(
  (stop test-state)





                                        ; detect reflection (for performance critical paths)
  (set! *warn-on-reflection* true)




                                        ; refresh sprites on the fly, rereads file from disk
  (swap! test-state #(dissoc-in % [:volatile (keyword 'projectr.render/sprite-cache) "building.png"]))






                                        ; debug triangulation
  (swap! test-state #(assoc % :debug-triags {})) ;reset
                                        ; can be done without threads, but hey, we have the jvm and it was easier :-D
  (defn triag-slowly [points]
    (loop [todo points
           finished []]
      (Thread/sleep 200)
      (cond (= (count todo) 3) (do
                                 (swap! test-state #(assoc-in % [:debug-triags points] (conj finished todo)))
                                 (conj finished todo))
            :else (let [ear (first (each-ear todo))]
                    (swap! test-state #(assoc-in % [:debug-triags points] finished))
                    (if ear
                      (recur (exclude todo [(second ear)])
                             (conj finished ear))
                      (recur (reverse points) []))))))

  (dorun (pmap #(triag-slowly (:polies %)) (:buildings (state-demo1)))))
