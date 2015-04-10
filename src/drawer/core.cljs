(ns ^:figwheel-always drawer.core
    (:require [reagent.core :as r]
              [goog.dom :as dom]
              [goog.events :as events]
              [clojure.string :as s]
              [cljs.core.async :refer [chan put!]]
              [drawer.commands :as c]
              [drawer.page-components :as pc]
              [drawer.keybindings :as kb])
    (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]))

(comment
  (cemerick.piggieback/cljs-repl :repl-env (cemerick.austin/exec-env))
  )

(enable-console-print!)

(defn new-tile [id]
  {:id id
   :name (str "Tile " (inc id))
   :impressions []})

(defonce state
  (r/atom {:editor :level
           :shape :circle
           :level-coords {:x 0 :y 0}
           :tile-coords {:x 0 :y 0}
           :tile 0
           :tiles (->> (range 4)
                       (map new-tile)
                       vec)
           :tile-width 100
           :tiles-wide 5
           :tiles-high 4}))

(defn listen [el type]
  (let [c (chan)]
    (events/listen el type (fn [e] (put! c e)))
    c))

(defn coords-from-event [e]
  {:x (.-offsetX e) :y (.-offsetY e)})

(defn event-charcode [e]
  (js/String.fromCharCode (.-keyCode e)))

(defonce stopper (chan))

(defn page-did-mount []
  (let [tile-editor (dom/getElement "tile-editor")
        level-editor (dom/getElement "level-editor")
        keys (listen js/document events/EventType.KEYUP)
        tile-mouse-moves (listen tile-editor events/EventType.MOUSEMOVE)
        tile-mouse-clicks (listen tile-editor events/EventType.CLICK)
        level-mouse-moves (listen level-editor events/EventType.MOUSEMOVE)]

    (go-loop []
      (alt!
        tile-mouse-moves  ([e _] (swap! state assoc :tile-coords (coords-from-event e))
                           (recur))
        tile-mouse-clicks ([_ _] (swap! state c/paint)
                           (recur))
        level-mouse-moves ([e _] (swap! state c/update-level-coords (coords-from-event e))
                           (recur))
        keys              ([e _]
                           (swap! state
                                  (get-in kb/key-commands
                                          [(event-charcode e)
                                           :transition]
                                          identity))
                           (recur))
        stopper           :stopped))

    (swap! state assoc :shape :rect)))

(defn page-component []
  (r/create-class {:render #(pc/page state)
                   :component-did-mount page-did-mount}))

(defn stop []
  (put! stopper :please-stop))

(defn mount-root! []
  (r/render-component [page-component]
                      (dom/getElement "app")))
