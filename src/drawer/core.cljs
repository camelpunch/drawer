(ns ^:figwheel-always drawer.core
    (:require [monet.canvas :as canvas]
              [monet.geometry :as geo]
              [reagent.core :as r]))

(enable-console-print!)

(def state (r/atom {:speed 4}))

(def up #(- % (@state :speed)))
(def down #(+ % (@state :speed)))
(def left up)
(def right down)

(defn right-edge [obj]
  ((geo/bottom-right obj) :x))
(defn left-edge [entity]
  ((geo/top-left entity) :x))
(defn bottom-edge [obj]
  ((geo/bottom-right obj) :y))
(defn top-edge [obj]
  ((geo/top-left obj) :y))

(defn over-right-limit? [container obj]
  (> (right-edge obj) (right-edge container)))
(defn over-left-limit? [container obj]
  (< (left-edge obj) (left-edge container)))
(defn over-bottom-limit? [container obj]
  (> (bottom-edge obj) (bottom-edge container)))
(defn over-top-limit? [container obj]
  (< (top-edge obj) (top-edge container)))

(defn bounce-x-within [bg {:keys [x-dir] :as entity}]
  (let [new-x-dir (if (or (over-right-limit? bg entity)
                          (and (= left x-dir)
                               (not (over-left-limit? bg entity))))
                    left
                    right)]
    (-> entity
        (assoc :x-dir new-x-dir)
        (update-in [:x] new-x-dir))))

(defn bounce-y-within [bg {:keys [y-dir] :as entity}]
  (let [new-y-dir (if (or (over-bottom-limit? bg entity)
                          (and (= up y-dir)
                               (not (over-top-limit? bg entity))))
                    up
                    down)]
    (-> entity
        (assoc :y-dir new-y-dir)
        (update-in [:y] new-y-dir))))

(defn page []
  [:div
   [:p
    [:label {:for "speed"} "Speed:"]
    [:input.inpt-num
     {:type "number"
      :min 0
      :value (@state :speed)
      :on-change (fn [e]
                   (let [new-speed (-> e
                                       .-target
                                       .-valueAsNumber)]
                     (swap! state assoc :speed new-speed)))}]]])

(r/render-component [page]
                    (js/document.getElementById "app"))

(def canvas-dom
  (.getElementById js/document "canvas"))

(defonce monet-canvas
  (canvas/init canvas-dom "2d"))

(canvas/add-entity monet-canvas
                   :background
                   (canvas/entity {:x 0 :y 0 :w 600 :h 480}
                                  nil
                                  (fn [ctx val]
                                    (-> ctx
                                        (canvas/fill-style "#191d21")
                                        (canvas/fill-rect val)))))


(canvas/add-entity
 monet-canvas :foo
 (canvas/entity {:x 10 :y 10 :w 100 :h 100 :y-direction down}
                (fn [entity]
                  (let [bg (canvas/get-entity monet-canvas :background)]
                    (->> entity
                         (bounce-x-within bg)
                         (bounce-y-within bg))))
                (fn [ctx val]
                  (-> ctx
                      (canvas/fill-style "#ff00ff")
                      (canvas/fill-rect val)))))
