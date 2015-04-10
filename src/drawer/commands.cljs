(ns drawer.commands
  (:require [drawer.shapes :as sh]))

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

(defn grid-align [pos grid]
  (- pos (mod pos grid)))

(defn update-level-coords [{:keys [tile-width] :as s} coords]
  (assoc s :level-coords
         (-> coords
             (update-in [:x] #(grid-align % tile-width))
             (update-in [:y] #(grid-align % tile-width)))))

(defn paint [{:keys [tile shape tile-coords] :as s}]
  (update-in s [:tiles tile :impressions]
             conj (sh/shape shape tile-coords)))
