(ns projectr.simulation
  (:use [projectr.geometry]
        [clojure.math.numeric-tower :as math]))


(defn attr-force [source target]
  (let [spos (source :position)
        tpos (target :position)
        sert (source :inertia)
        tert (target :inertia)
        attr-const 0.00005
        d (dist spos tpos)
        ed (eucl-dist d)
        nd (scale (/ 1 (max ed 0.0001)) d)
        f (/ (* sert tert attr-const)
             (max 0.8 (* ed ed)))]
    {:x (* f (nd :x))
     :y (* f (nd :y))}))


(defn- in-bbox?  [{:keys [x y]} {{:keys [top left right bottom]} :bounding-box}]
  (and (>= x left) (<= x right)
       (>= y top) (<= y bottom)))

#_(in-bbox? {:x 1 :y 2} {:bounding-box {:top 0 :left 0 :right 10 :bottom 20}})


(defn colliding? [buildings pos speed]
  (let [end-pos (merge-with + pos speed)]
    (some true? (map #(cutting? % [pos end-pos])
                     (concat (mapcat (comp each-pair :polies)
                                     (filter (partial in-bbox? end-pos) buildings)))))))


#_(colliding? [{:polies [{:x 1 :y 1} {:x 0 :y 1} {:x 0 :y 0}]
                :bounding-box {:top 0 :left 0 :right 1 :bottom 1}}] {:x 0.8 :y 0.3} {:x -0.5 :y 0.3})
#_(cutting? [{:x 0 :y 0} {:x 1 :y 1}] [{:x 0.8 :y 0.3} {:x -0.5 :y 0.30001}])


(defn move-emanation [buildings attractors dt emanation]
  (let [{:keys [speed position inertia]} emanation
        decay (math/expt 0.8 dt)
        total-force (reduce (partial merge-with +)
                            (map (partial attr-force emanation) attractors))
        etf (eucl-dist total-force)
        thresh 0.00002
        nsp (if (> etf thresh)
              (merge-with +
                          (scale decay speed)
                          (scale (/ decay (* inertia 10)) total-force))
              (scale decay speed))]
    (assoc emanation
      :force total-force
      :speed nsp
      :position (if-not (colliding? buildings position nsp)
                  (merge-with + position nsp)
                  position
                  #_(do (println "colliding: " position nsp) )))))



(defn emanate [state delta-time]
  (let [{:keys [players symbols emanations buildings]} state
        emanies (map #(update-in % [:inertia] * -0.1) emanations)]
    (assoc state :emanations (pmap (partial move-emanation
                                         buildings
                                         (concat symbols
                                                 (vals players)
                                                 emanies)
                                         delta-time)
                                emanations))))


(defn decay-symbols [state delta-time]
  (let [{:keys [symbols]} state
        decay (math/expt 0.95 delta-time)]
    (assoc state :symbols
           (filter #(> (% :inertia) 0.05)
                   (map #(assoc % :inertia (* decay (% :inertia))) symbols)))))


(defn simulate!
  "Simulation loop in dependence of delta time."
  [state last-millis]
  (let [current-millis (System/currentTimeMillis)
        delta-time (/ (- current-millis last-millis)
                      1000.)]
    (swap! state (fn [old]
                   (-> old
                       (emanate delta-time)
                       (decay-symbols delta-time))))
    (Thread/sleep 50)
    (if (::run (:volatile @state))
      (recur state current-millis))))


(defn start
  "Starts the parallel simulation in background threads."
  [state]
  (swap! state #(assoc-in % [:volatile ::run] true))
  (.start (Thread. #(simulate! state (System/currentTimeMillis)))))


(defn stop
  "Stop/pause background simulation."
  [state]
  (swap! state #(assoc-in % [:volatile ::run] false)))
