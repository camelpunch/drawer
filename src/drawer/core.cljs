(ns ^:figwheel-always drawer.core
    (:require [monet.canvas :as canvas]))

(enable-console-print!)

(def canvas-dom
  (.getElementById js/document "canvas"))

(defonce monet-canvas
  (canvas/init canvas-dom "2d"))

(canvas/add-entity
 monet-canvas :background
 (canvas/entity {:x 0 :y 0 :w 600 :h 600}
                nil
                (fn [ctx val]
                  (-> ctx
                      (canvas/fill-style "#191d21")
                      (canvas/fill-rect val)))))

(def down inc)
(def up dec)

(defn set-direction [{:keys [y y-direction] :as entity}]
  (assoc entity :y-direction
         (cond
           (and (= down y-direction) (> 100 y)) down
           (and (= up y-direction) (> 0 y)) down
           :else up)))

(defn move [{:keys [y-direction] :as entity}]
  (update-in entity [:y] y-direction))

(canvas/add-entity
 monet-canvas :foo
 (canvas/entity {:x 10 :y 10 :w 100 :h 100 :y-direction inc}
                (fn [entity]
                  (-> entity
                      (update-in [:x] inc)
                      set-direction
                      move))
                (fn [ctx val]
                  (-> ctx
                      (canvas/fill-style "#ff00ff")
                      (canvas/fill-rect val)))))
