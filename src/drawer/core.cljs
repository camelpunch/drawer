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

(defn beyond? [comparison-fn edge-fn container obj]
  (comparison-fn (edge-fn obj) (edge-fn container)))

(defn bounce-x-within [container {:keys [x-dir] :as entity}]
  (let [new-x-dir (if (or (beyond? > right-edge container entity)
                          (and (= left x-dir)
                               (beyond? > left-edge container entity)))
                    left
                    right)]
    (-> entity
        (assoc :x-dir new-x-dir)
        (update-in [:x] new-x-dir))))

(defn bounce-y-within [container {:keys [y-dir] :as entity}]
  (let [new-y-dir (if (or (beyond? > bottom-edge container entity)
                          (and (= up y-dir)
                               (beyond? > top-edge container entity)))
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

(def initial-bg {:x 0 :y 0 :w 600 :h 480})
(defn current-bg []
  (canvas/get-entity monet-canvas :background))

(canvas/add-entity monet-canvas
                   :background
                   (canvas/entity initial-bg
                                  nil
                                  (fn [ctx val]
                                    (-> ctx
                                        (canvas/fill-style "#191d21")
                                        (canvas/fill-rect val)))))

(canvas/add-entity
 monet-canvas :square1
 (canvas/entity {:x 10 :y 10 :w 100 :h 100}
                (fn [entity]
                  (let [bg (current-bg)
                        big-box (canvas/get-entity monet-canvas
                                                   :square2)]
                    (->> entity
                         (bounce-x-within bg)
                         (bounce-y-within bg)
                         (bounce-x-within big-box)
                         (bounce-y-within big-box))))
                (fn [ctx val]
                  (-> ctx
                      (canvas/fill-style "rgba(255, 255, 255, 0.5)")
                      (canvas/fill-rect val)))))

(canvas/add-entity
 monet-canvas :square2
 (let [w 200 h 300]
   (canvas/entity {:x (- (initial-bg :w) w 10)
                   :y (- (initial-bg :h) h 10)
                   :w w
                   :h h
                   :y-dir up
                   :x-dir left}
                  (fn [entity]
                    (let [bg (current-bg)]
                      (->> entity
                           (bounce-x-within bg)
                           (bounce-y-within bg))))
                  (fn [ctx val]
                    (-> ctx
                        (canvas/fill-style "rgba(255, 0, 0, 0.2)")
                        (canvas/fill-rect val))))))
