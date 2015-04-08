(ns ^:figwheel-always drawer.core
    (:require [reagent.core :as r]
              [goog.dom :as dom]
              [goog.events :as events]
              [cljs.core.async :refer [chan put!]])
    (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]))

(enable-console-print!)

(defonce state
  (r/atom {:shape :circle
           :coords {:x 0 :y 0}}))

(defn class-for [state k v]
  (when (= v (state k)) "active"))

(defn activate [state k v]
  (assoc state
         k (when (not= v (state k)) v)))

(defn by-id [id]
  (.getElementById js/document id))

(def shapes
  {:circle (fn [x y w h]
             {:class "cursor circle"
              :cx x
              :cy y
              :r (/ w 2)})
   :rect (fn [x y w h]
           {:class "cursor square"
            :x (- x (/ w 2))
            :y (- y (/ h 2))
            :width w
            :height h})})

(defn shape []
  (let [s @state
        shape (s :shape)
        x (get-in s [:coords :x])
        y (get-in s [:coords :y])
        w 100
        h 100]
    [shape ((shapes shape) x y w h)]))

(defn switch-to [section-type section]
  (fn [e]
    (.preventDefault e)
    (swap! state activate section-type section)))

(defn listen [el type]
  (let [c (chan)]
    (events/listen el type (fn [e] (put! c e)))
    c))

(defn page-did-mount []
  (let [svg (by-id "svg")
        mouse-moves (listen svg events/EventType.MOUSEMOVE)
        mouse-clicks (listen svg events/EventType.CLICK)]

    (go-loop []
      (alt!
        mouse-moves ([e _]
                     (swap! state assoc
                            :coords {:x (.-offsetX e)
                                     :y (.-offsetY e)})
                     (recur))))

    (swap! state assoc :shape :rect)))

(defn menu-item
  [menu item human-name]
  [:li.menu-item
   [:a.btn
    {:id (name item)
     :href "#"
     :class (class-for @state menu item)
     :on-click (switch-to menu item)}
    human-name]])

(defn page []
  [:div
   [:p.inf "Coords:" (str (@state :coords))]
   [:ul.menu
    [menu-item :shape :rect "Square"]
    [menu-item :shape :circle "Circle"]]
   [:div.drawing
    [:svg#svg
     [shape]]]
   [:p
    [:a {:href "https://github.com/camelpunch/drawer"} "GitHub"]]])

(defn page-component []
  (r/create-class {:render page
                   :component-did-mount page-did-mount}))

(r/render-component [page-component]
                    (by-id "app"))
