(ns projectr.render
  (:use [clojure.math.numeric-tower :as math]
        [clojure.string :only [split-lines]])
  (:import [com.badlogic.gdx Gdx]
           [com.badlogic.gdx.graphics GL10 OrthographicCamera Texture]
           [com.badlogic.gdx.graphics.g2d SpriteBatch PolygonSpriteBatch PolygonRegion PolygonSprite TextureRegion BitmapFont]
           [com.badlogic.gdx.graphics.glutils ShapeRenderer$ShapeType]))


; render functions
(defn init-render!
  "Sets up the rendering pipeline."
  [state]
  (let [camera (OrthographicCamera.)
        {:keys [resolution]} @state
        width (resolution :x)
        height (resolution :y)
        _ (.setToOrtho camera false width height)
        sb (SpriteBatch.)
        pb (PolygonSpriteBatch.)
        font (BitmapFont.)]
  (swap! state (fn [old]
                 (assoc old :volatile (assoc (or (:volatile old) {})
                                        ::sprite-batch sb
                                        ::poly-batch pb
                                        ::sprite-cache {}
                                        ::font font
                                        ::camera camera))))))


(defn dispose-render! [state]
  (let [{:keys [volatile]} @state
        {poly-batch ::poly-batch
         sprite-batch ::sprite-batch
         sprite-cache ::sprite-cache} volatile]
    (.dispose poly-batch)
    (.dispose sprite-batch)
    (doall (map #(.dispose %) (vals sprite-cache)))))


(defn clear-plane! []
  (.glClearColor Gdx/gl 0.2 0.2 0.2 1)
  (.glClear Gdx/gl GL10/GL_COLOR_BUFFER_BIT))


(defn update-camera! [state]
  (let [{^OrthographicCamera camera ::camera
         ^SpriteBatch sprite-batch ::sprite-batch
         ^PolygonSpriteBatch poly-batch ::poly-batch} (@state :volatile)]
    (.update camera)
    (.setProjectionMatrix sprite-batch (.-combined camera))
    (.setProjectionMatrix poly-batch (.-combined camera))))


(defn as-game-coords [res scaled-pos]
  (let [{:keys [x y]} scaled-pos]
    {:x (* (res :x) x)
     :y (* (res :y) y)}))


(defn to-flat-float-array [poly]
  (into-array Float/TYPE (flatten (map (fn [{:keys [x y]}] [x y]) poly))))


(defn- texture-cache ^Texture [state sprite]
  (let [{sprite-cache ::sprite-cache} (@state :volatile)
        cached (sprite-cache sprite)]
    (if cached
      cached
      (let [text (Texture. (.internal Gdx/files sprite))]
        (swap! state #(assoc-in % [:volatile ::sprite-cache sprite] text))
        text))))

(defn- buildings-cache [state]
  (let [{:keys [volatile resolution buildings]} @state
        fbs (::buildings volatile)
        cache-res (::buildings-cache-res volatile)]
    (if (and fbs (= cache-res resolution)) fbs
        (let [nfbs (map to-flat-float-array (map #(map (partial as-game-coords resolution) %)
                                                 (map :polies buildings)))]
          (swap! state #(-> %
                            (assoc-in [:volatile ::buildings] nfbs)
                            (assoc-in [:volatile ::buildings-cache-res] resolution)))
          nfbs))))


(defn- btriags-cache [state]
  (let [{:keys [volatile resolution buildings]} @state
        fts (::btriags volatile)
        cache-res (::buildings-cache-res volatile)]
    (if (and fts (= cache-res resolution)) fts
        (let [nfts (map #(map (comp to-flat-float-array (partial map (partial as-game-coords resolution))) %)
                        (map :triags buildings))]
    (swap! state #(-> %
                      (assoc-in [:volatile ::btriags] nfts)
                      (assoc-in [:volatile ::buildings-cache-res] resolution)))
    nfts))))



(defn- draw-triags!
  "Call with triags in game coordinates. Use texture to draw all lists of triags."
  [state ^PolygonSpriteBatch poly-batch sprite triagss]
  (.begin poly-batch)
  (let [text-region (TextureRegion. (texture-cache state sprite))]
    (doall (map (fn [triags]
                  (doall (map #(.draw (PolygonSprite.
                                       (PolygonRegion.
                                        text-region
                                        ^floats %))
                                      poly-batch)
                              triags)))
                triagss)))
  (.end poly-batch))

(defn- draw-polies!
  "Call with polys in game coordinates. Use texture to draw all polys which have to be flat float arrays."
  [state ^PolygonSpriteBatch poly-batch sprite polies]
  (.begin poly-batch)
  (doall (map #(.draw (PolygonSprite.
                       (PolygonRegion.
                        (TextureRegion. (texture-cache state sprite))
                        ^floats %))
                      poly-batch)
              polies))
  (.end poly-batch))


(defn- draw-sprite! [state ^SpriteBatch batch sprite position transparency]
  (.begin batch)
  (let [{:keys [x y]} (as-game-coords (:resolution @state) position)
        sp (texture-cache state sprite)
        w (.getWidth sp)
        h (.getHeight sp)]
    (.setColor batch (float 1) (float 1) (float 1) (float transparency))
    (.draw batch sp (float (- x (/ w 2))) (float (- y (/ h 2)))))
  (.end batch))


(defn- draw-points! [state ^SpriteBatch batch sprite points]
  (.begin batch)
  (doall (map (fn [position]
                (let [{:keys [x y]} (as-game-coords (:resolution @state) position)
                      ^Texture sp (texture-cache state sprite)
                      w (.getWidth sp)
                      h (.getHeight sp)]
                  (.setColor batch (float 1) (float 1) (float 1) (float 1))
                  (.draw batch sp (float (- x (/ w 2))) (float (- y (/ h 2))))))
              points))
  (.end batch))

(defn draw-hud! [state ^SpriteBatch sprite-batch]
  (let [{hud :hud {height :y} :resolution
         {^BitmapFont font ::font} :volatile} @state]
    (.begin sprite-batch)
    (.setColor font (float 1) (float 1) (float 1) (float 1))
    (doall (map #(.draw font sprite-batch %1 10 %2)
                (split-lines (hud @state))
                (map #(- height 10 (* % 30)) (range 0 30))))
    (.end sprite-batch)))


; draw the whole thing
(defn draw-all!
  "Glue it all together."
  [state]
  (let [{:keys [buildings players emanations symbols volatile debug-triags]} @state
        {poly-batch ::poly-batch sprite-batch ::sprite-batch} volatile]

    (doall (map (fn [{:keys [position sprite inertia]}]
                  (draw-sprite! state sprite-batch sprite position inertia))
                (concat (vals players) symbols emanations)))

    (draw-polies! state poly-batch "building.png" (buildings-cache state))
    #_(draw-triags! state poly-batch "building.png" (btriags-cache state))

    ; debug drawings
    (draw-triags! state poly-batch "turquoise.png" (apply concat (vals debug-triags)))

    (draw-hud! state sprite-batch)))
