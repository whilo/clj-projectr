(ns projectr.geometry
  (:use [clojure.math.numeric-tower :as math]))


(defn dist [a b]
  {:x (- (b :x) (a :x))
   :y (- (b :y) (a :y))})


(defn scale
  [a {:keys [x y]}]
  {:x (* a x) :y (* a y)}
     #_(zipmap (keys v) (map op (vals v) (repeat a))))


(defn eucl-dist [d]
  (let [sq #(* % %)]
    (math/sqrt (+ (sq (d :x)) (sq (d :y))))))


(defn each-pair
  ([verts] (each-pair (cycle verts) (count verts) []))
  ([verts cnt pairs]
     (if (= cnt 0)
       pairs
       (recur (rest verts) (dec cnt) (conj pairs (take 2 verts))))))


(defn cut-point [[p1 p2] [p1* p2*]]
  (let [{x1 :x y1 :y} p1
        {x2 :x y2 :y} p2
        {x1* :x y1* :y} p1*
        {x2* :x y2* :y} p2*]
    (if (or (== y2* y1*)
            (== 0 (- (- x2 x1)
                     (* (- y2 y1)
                        (/ (- x2* x1*) (- y2* y1*))))))
      #_(println "cut-point div by zero: " p1 p2 p1* p2*)
      nil

      (let [z* (/ (- x2* x1*) (- y2* y1*))
            a (/ (+ (- x1* x1) (* (- y1 y1*) z*))
                 (- (- x2 x1) (* (- y2 y1) z*)))]
        {:x (+ (* a (- x2 x1)) x1)
         :y (+ (* a (- y2 y1)) y1)}))))


(defn cutting? [[p1 p2] [p1* p2*]]
  (let [{x1 :x y1 :y} p1
        {x2 :x y2 :y} p2
        {x1* :x y1* :y} p1*
        {x2* :x y2* :y} p2*
        c (cut-point [p1 p2] [p1* p2*])]
    (if c (let [{cx :x cy :y} c]
            (and (> cx (min x1 x2)) (< cx (max x1 x2))
                 (> cy (min y1 y2)) (< cy (max y1 y2))
                 (> cx (min x1* x2*)) (< cx (max x1* x2*))
                 (> cy (min y1* y2*)) (< cy (max y1* y2*))))
        false)))


#_(cut-point [{:x 0 :y 0} {:x 1 :y 0.5}] [{:x 0 :y 1} {:x 1 :y 0}])
#_(cutting? [{:x 0.4 :y 0.4} {:x 0.6 :y 0.6}] [{:x 0.6 :y 0.4} {:x 0.4 :y 0.6}])


(defn colinear [v1 v2 v3]
  (- (* (- (:x v2) (:x v1))
        (- (:y v3) (:y v1)))
     (* (- (:x v3) (:x v1))
        (- (:y v2) (:y v1)))))


(defn point-in-triangle? [p [v1 v2 v3]]
  (let [b0 (math/abs (colinear v1 v2 v3))]
    (if (> b0 1E-6)
      (let [b1 (/ (colinear p v2 v3) b0)
            b2 (/ (colinear p v3 v1) b0)
            b3 (- 1.0 b1 b2)]
        (and (> b1 1e-6) (> b2 1e-6) (> b3 1e-6)))
      false)))


#_(point-in-triangle? {:x 0 :y -1} [{:x -1 :y -1}
                                   {:x 1 :y -1}
                                   {:x 0 :y 1}])


(defn each-triag
  ([verts] (each-triag (cycle verts) (count verts) []))
  ([verts cnt triags]
     (if (= cnt 0)
       triags
       (recur (rest verts) (dec cnt) (conj triags (take 3 verts))))))


(defn convex-triag? [[{ax :x ay :y} {bx :x by :y} {cx :x cy :y}]]
  (<= (- (* (- ax bx) (- cy by))
         (* (- ay by) (- cx bx)))
      0))


(defn vec-to-point [[x y]]
  {:x x :y y})

(defn exclude [points to-exclude]
  (filter (fn [p] (not (some #(= p %) to-exclude))) points))


(defn points-in-triangle? [points triag]
  (cond (empty? points) false
        (point-in-triangle? (first points) triag) true
        :else (recur (rest points) triag)))


(defn- spanned-area-sign [p1 p2 p3]
  (int (Math/signum (float (+ (* (:x p1) (- (:y p3) (:y p2)))
                              (* (:x p2) (- (:y p1) (:y p3)))
                              (* (:x p3) (- (:y p2) (:y p1))))))))

#_(spanned-area-sign {:x -1 :y 1} {:x 0 :y 0} {:x 1 :y 1})

(defn each-ear
  [polies]
  (filter #(not (points-in-triangle? (exclude polies %) %))
          (filter #(>= (apply spanned-area-sign %) 0)
                  (each-triag polies))))


; https://github.com/libgdx/libgdx/blob/master/gdx/src/com/badlogic/gdx/math/EarClippingTriangulator.java
(defn- area [vertices]
  (reduce (fn [acc e]
            (let [p1 (nth vertices e)
                  p2 (nth vertices (mod (inc e) (count vertices)))]
              (+ acc
                 (- (* (:x p1) (:y p2))
                    (* (:x p2) (:y p1))))))
          0
          (range 0 (count vertices))))



(defn clockwise? [vertices]
  (< (area vertices) 0))


(defn triangulate
  ([points]
     (loop [todo (if (clockwise? points) points (reverse points))
            finished []]
       (if (= (count todo) 3) (conj finished todo)
           (if-let [ear (first (each-ear todo))]
             (recur (exclude todo [(second ear)]) (conj finished ear))
             (if-let [desp-ear (first (filter convex-triag? (each-triag todo)))]
               (recur (exclude todo [(second desp-ear)]) (conj finished desp-ear))
               []))))))


(defn in-building? [point building]
  (let [triags (building :triags)]
    (some #(point-in-triangle? point %) triags)))


(defn rand-coords-outside-building
  [state]
  (loop []
    (let [x (rand) y (rand)
          p (vec-to-point [x y])]
      (if (some (partial in-building? p) (state :buildings))
        (recur)
        p))))
