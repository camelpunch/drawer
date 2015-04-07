(ns ^:figwheel-always drawer.core
    (:require [monet.canvas :as canvas]
              [monet.geometry :as geo]
              [reagent.core :as r]
              [goog.dom :as dom]
              [goog.events :as events]
              [cljs.core.async :refer [chan put!]])
    (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]))

(enable-console-print!)

(defonce state
  (r/atom {:tool nil
           :coords {:x 0 :y 0}}))

(defn class-for [state k v]
  (when (= v (state k)) "active"))

(defn activate [state k v]
  (assoc state
         k (when (not= v (state k)) v)))

(defn by-id [id]
  (.getElementById js/document id))

(defn page []
  [:div
   [:p "Coords:" (str (@state :coords))]
   [:p.tool
    [:button.btn
     {:id "square"
      :class (class-for @state :tool :square)
      :on-click (fn [e] (swap! state activate :tool :square))}
     "Square"]]
   [:p.tool
    [:button.btn
     {:id "circle"
      :class (class-for @state :tool :circle)
      :on-click (fn [e] (swap! state activate :tool :circle))}
     "Circle"]]])

(def initial-bg {:x 0 :y 0 :w 640 :h 480})

(defn listen [el type]
  (let [c (chan)]
    (events/listen el type (fn [e] (put! c e)))
    c))

(defn removed? [key old-state new-state]
  (and (old-state key)
       (nil? (new-state key))))

(defn added? [key old-state new-state]
  (and (not (nil? (new-state :tool)))
       (not= (old-state :tool) (new-state :tool))))

(def dims
  {:square {:x 0 :y 0 :w 100 :h 100}
   :circle {:x 0 :y 0 :rw 100 :rh 100}})

(def draw-fns
  {:square (fn [ctx val]
             (-> ctx
                 (canvas/fill-style "#000000")
                 (canvas/fill-rect val)))
   :circle (fn [ctx val]
             (-> ctx
                 (canvas/ellipse val)
                 (canvas/stroke-style "#000000")
                 (canvas/stroke)))})

(defn page-did-mount []
  (let [canvas-dom (by-id "canvas")
        monet-canvas (canvas/init canvas-dom "2d")
        mouse-moves (listen canvas-dom events/EventType.MOUSEMOVE)
        mouse-clicks (listen canvas-dom events/EventType.CLICK)]

    (canvas/add-entity
     monet-canvas
     :background
     (canvas/entity initial-bg
                    nil
                    (fn [ctx val]
                      (-> ctx
                          (canvas/fill-style "#ffffff")
                          (canvas/fill-rect val)))))

    (go-loop []
      (alt!
        mouse-moves ([e _]
                     (swap! state assoc
                            :coords {:x (.-offsetX e)
                                     :y (.-offsetY e)})
                     (recur))))

    (add-watch
     state :display-shape
     (fn [_ _ old-state new-state]
       (cond

         (removed? :tool old-state new-state)
         (canvas/remove-entity monet-canvas (old-state :tool))

         (added? :tool old-state new-state)
         (let [old-tool (old-state :tool)
               new-tool (new-state :tool)
               dimensions (new-tool dims)
               draw-fn (new-tool draw-fns)
               entity (canvas/entity dimensions
                                     #(merge % (@state :coords))
                                     draw-fn)]
           (canvas/remove-entity monet-canvas old-tool)
           (canvas/add-entity monet-canvas new-tool entity)))))

    (swap! state assoc :tool :square)))

(defn page-component []
  (r/create-class {:render page
                   :component-did-mount page-did-mount}))

(r/render-component [page-component]
                    (by-id "app"))
