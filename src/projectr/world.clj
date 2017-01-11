(ns projectr.world
  (:use [projectr.geometry :only [triangulate]])
  (:require [clojure.math.numeric-tower :as math]
            [clojure.java.io :as io]
            [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clojure.data.zip.xml :as zip-xml]))

(def root (-> (.getResourceAsStream (clojure.lang.RT/baseLoader) "map.osm")
              xml/parse
              zip/xml-zip))


(defn osm->points
  [root]
  (into {}
        (for [n (zip-xml/xml-> root :node)]
          [(Long/parseLong (zip-xml/attr n :id))
           {:lon (Double/parseDouble (zip-xml/attr n :lon))
            :lat (Double/parseDouble (zip-xml/attr n :lat))}])))

(defn osm->ways
  [root pred]
  (into {}
    (for [m (zip-xml/xml-> root :way)]
      (let [b (pred m)]
        (if b [(Long/parseLong (zip-xml/attr m :id))
         (vec (for [n (zip-xml/xml-> m :nd)]
                 (Long/parseLong (zip-xml/attr n :ref))))])))))

(def bounds
  (let [n (first (zip-xml/xml-> root :bounds))]
    {:minlon (Double/parseDouble (zip-xml/attr n :minlon))
     :minlat (Double/parseDouble (zip-xml/attr n :minlat))
     :maxlon (Double/parseDouble (zip-xml/attr n :maxlon))
     :maxlat (Double/parseDouble (zip-xml/attr n :maxlat))}))

(def points (osm->points root))

#_(val (first points))
; warning only inserts 102 entries for me atm.
#_(map persist/add-point (map val points))

(def ways (osm->ways root (fn [_] true)))

#_(val (last ways))
; warning only inserts 102 entries for me atm.
#_(map persist/add-way (map val ways))

(defn as-coords [polygons]
  (for [[k v] polygons] (map points v)))

#_(first (as-coords ways))


(defn as-ratios [coords]
  (for [c coords]
    (for [{lon :lon lat :lat} c]
      (let [min-x (:minlon bounds)
            max-x (:maxlon bounds)
            min-y (:minlat bounds)
            max-y (:maxlat bounds)
            scale-x (- max-x min-x)
            scale-y (- max-y min-y)]
        {:x (/ (- lon min-x) scale-x)
         :y (/ (- lat min-y) scale-y)}))))

#_(first (as-ratios (as-coords ways)))

(defn is-building? [way]
  (some (zip-xml/attr= :k "building")
        (zip-xml/xml-> way :tag)))



(defn- bounding-box [polies]
  (let [xs (map :x polies)
        ys (map :y polies)]
    {:top (apply min ys)
     :left (apply min xs)
     :right (apply max xs)
     :bottom (apply max ys)}))

(defn load-buildings
  "Wraps each building polygon sequence in a map and
   adds a sprite entry."
  ([] (load-buildings (as-ratios (as-coords (osm->ways root is-building?)))))
  ([polies] (load-buildings polies (map triangulate polies)))
  ([polies triags] (map (fn [b t] {:polies b
                                  :triags t
                                  :bounding-box (bounding-box b)
                                  :sprite "building.png"})
                        polies triags)))
