(ns drawer.commands
  (:require [drawer.shapes :refer [shape]]))

(defn activate [state k v]
  (if (not= v (state k))
    (assoc state k v)
    state))

(defn next-item [s menu items]
  (let [current (s menu)]
    (->> items cycle (drop-while #(not= current %)) fnext)))

(defn switch-to-next [s menu items]
  (activate s menu (next-item s menu items)))

(defn switch-to-next-tile [s]
  (switch-to-next s :tile (vec (range (count (s :tiles))))))

(defn update-coords [s coords]
  (assoc s :coords coords))

(defn grid-align [pos grid]
  (- pos (mod pos grid)))

(defn update-grid-coords [s coords]
  (let [tile-width (s :tile-width)]
    (assoc s :coords (-> coords
                         (update-in [:x] #(grid-align % tile-width))
                         (update-in [:y] #(grid-align % tile-width))))))

(defn paint [s]
  (update-in s [:tiles (s :tile) :impressions] conj (shape s)))
