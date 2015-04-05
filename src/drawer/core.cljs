(ns ^:figwheel-always drawer.core
    (:require [monet.canvas :as canvas]
              [monet.geometry :as geo]
              [reagent.core :as r]
              [goog.dom :as dom]
              [goog.events :as events]
              [cljs.core.async :refer [chan put!]])
    (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(enable-console-print!)

(def state
  (r/atom {:tool nil
           :coords []}))

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

(defn class-for [state tool]
  (when (= tool (state :tool))
    "active"))

(defn activate-tool [state tool]
  (assoc state :tool
         (when (not= tool (state :tool))
           tool)))

(defn by-id [id]
  (.getElementById js/document id))

(defn page []
  [:div
   [:p "Coords:" (str (@state :coords))]
   [:p.tool
    [:button.btn
     {:id "square"
      :class (class-for @state :square)
      :on-click (fn [e] (swap! state
                              activate-tool :square))}
     "Square"]]
   [:p.tool
    [:button.btn
     {:id "circle"
      :class (class-for @state :circle)
      :on-click (fn [e] (swap! state
                              activate-tool :circle))}
     "Circle"]]])

(r/render-component [page] (by-id "app"))

(def canvas-dom (by-id "canvas"))

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

(defn listen [el type]
  (let [c (chan)]
    (events/listen el type (fn [e] (put! c e)))
    c))

(let [c (listen canvas-dom events/EventType.MOUSEMOVE)]
  (go-loop []
    (let [e (<! c)]
      (swap! state assoc :coords [(.-offsetX e) (.-offsetY e)]))
    (recur)))
