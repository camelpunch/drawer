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

(defn bounce-x [{:keys [x-dir] :as entity} container]
  (let [new-x-dir (if (or (beyond? >= right-edge container entity)
                          (and (= left x-dir)
                               (beyond? >= left-edge container entity)))
                    left
                    right)]
    (-> entity
        (assoc :x-dir new-x-dir)
        (update-in [:x] new-x-dir))))

(defn bounce-y [{:keys [y-dir] :as entity} container]
  (let [new-y-dir (if (or (beyond? >= bottom-edge container entity)
                          (and (= up y-dir)
                               (beyond? >= top-edge container entity)))
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
                     (when-not (js/isNaN new-speed)
                       (swap! state assoc :speed new-speed))))}]]])

(r/render-component [page]
                    (js/document.getElementById "app"))

(def canvas-dom
  (.getElementById js/document "canvas"))

(defonce monet-canvas
  (canvas/init canvas-dom "2d"))

(def initial-bg {:x 0 :y 0 :w 640 :h 480})
(defn current [k]
  (canvas/get-entity monet-canvas k))

(canvas/add-entity monet-canvas
                   :background
                   (canvas/entity initial-bg
                                  nil
                                  (fn [ctx val]
                                    (-> ctx
                                        (canvas/fill-style "#ffffff")
                                        (canvas/fill-rect val)))))

(defn add-ball [k x y update]
  (canvas/add-entity
   monet-canvas k
   (canvas/entity {:x x :y y :w 220 :h 220}
                  update
                  (fn [ctx val]
                    (-> ctx
                        (canvas/draw-image (js/document.getElementById "image") (val :x) (val :y)))))))

(add-ball :ball1 1000 0
          (fn [entity]
            (let [bg (current :background)
                  box (current :box)]
              (-> entity
                  (bounce-x bg)
                  (bounce-y bg)
                  (bounce-x box)
                  (bounce-y box)))))

(canvas/add-entity
 monet-canvas :box
 (let [w 300 h 400]
   (canvas/entity {:x (- (initial-bg :w) w 10)
                   :y (- (initial-bg :h) h 10)
                   :w w
                   :h h
                   :y-dir up
                   :x-dir left}
                  (fn [entity]
                    (let [bg (current :background)]
                      (-> entity
                          (bounce-x bg)
                          (bounce-y bg))))
                  (fn [ctx val]
                    (-> ctx
                        (canvas/fill-style "rgba(0, 0, 0, 0.5)")
                        (canvas/fill-rect val))))))

(add-ball :ball2 10 10
          (fn [entity]
            (let [bg (current :background)
                  box (current :box)]
              (-> entity
                  (bounce-x bg)
                  (bounce-y bg)))))
