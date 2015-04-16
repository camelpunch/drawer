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
  (switch-to-next s :tile (range (count (s :tiles)))))

(defn snap [grid pos]
  (- pos (mod pos grid)))

(defn grid-align [coords grid]
  (let [snapper (partial snap grid)]
    (-> coords
        (update-in [:x] snapper)
        (update-in [:y] snapper))))

(defn update-level-coords [{:keys [tile-width] :as s} coords]
  (assoc s :level-coords (grid-align coords tile-width)))

(defn paint [{:keys [tile shape tile-coords] :as s}]
  (update-in s [:tiles tile :impressions]
             conj (sh/shape shape tile-coords)))

(defn paint-tile [{:keys [tile level-coords] :as  s}]
  (update-in s [:level :impressions]
             conj {:tile tile
                   :coords level-coords}))
