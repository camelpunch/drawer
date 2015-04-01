(ns ^:figwheel-always drawer.core
    (:require [monet.canvas :as canvas]
              [monet.geometry :as geo]
              [reagent.core :as r]))

(enable-console-print!)

(def state (r/atom {:contained? true}))

(def canvas-dom
  (.getElementById js/document "canvas"))

(defonce monet-canvas
  (canvas/init canvas-dom "2d"))

(canvas/add-entity monet-canvas
                   :background
                   (canvas/entity {:x 0 :y 0 :w 200 :h 600}
                                  nil
                                  (fn [ctx val]
                                    (-> ctx
                                        (canvas/fill-style "#191d21")
                                        (canvas/fill-rect val)))))

(def up dec)
(def down inc)
(def left dec)
(def right inc)

(defn right-edge [obj]
  ((geo/bottom-right obj) :x))
(defn left-edge [entity]
  ((geo/top-left entity) :x))

(defn over-right-limit? [container obj]
  (> (right-edge obj) (right-edge container)))
(defn over-left-limit? [container obj]
  (< (left-edge obj) (left-edge container)))

(defn bounce-x-within [bg {:keys [x-dir] :as entity}]
  (let [new-x-dir (if (or (over-right-limit? bg entity)
                          (and (= left x-dir)
                               (not (over-left-limit? bg entity))))
                    left
                    right)]
    (-> entity
        (assoc :x-dir new-x-dir)
        (update-in [:x] new-x-dir))))

(canvas/add-entity
 monet-canvas :foo
 (canvas/entity {:x 10 :y 10 :w 100 :h 100 :y-direction down}
                (fn [entity]
                  (let [bg (canvas/get-entity monet-canvas :background)]
                    (->> entity
                         (bounce-x-within bg))))
                (fn [ctx val]
                  (-> ctx
                      (canvas/fill-style "#ff00ff")
                      (canvas/fill-rect val)))))

(defn page []
  [:div
])

(defn ^:export run []
  (r/render [page]
            (js/document.getElementById "app")))
