(ns ^:figwheel-always drawer.core
    (:require [reagent.core :as r]
              [goog.dom :as dom]
              [goog.events :as events]
              [clojure.string :as s]
              [cljs.core.async :refer [chan put!]])
    (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]]))

(comment
  (cemerick.piggieback/cljs-repl :repl-env (cemerick.austin/exec-env))
  )

(enable-console-print!)

(defonce state
  (r/atom {:editor :level
           :shape :circle
           :coords {:x 0 :y 0}}))

(defn class-for [state k v]
  (s/join " " [(s/join "-" (map name [v k]))
           (if (= v (state k)) "active" "inactive")]))

(defn activate [state k v]
  (assoc state
         k (when (not= v (state k)) v)))

(defn by-id [id]
  (.getElementById js/document id))

(defn listen [el type]
  (let [c (chan)]
    (events/listen el type (fn [e] (put! c e)))
    c))

(defn update-coords [s e]
  (assoc s :coords {:x (.-offsetX e) :y (.-offsetY e)}))

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
            :height h})
   :line (fn [x y w h]
           (let [x1 (- x (/ w 2))
                 y1 (- y (/ h 2))]
             {:class "cursor line"
              :x1 x1
              :y1 y1
              :x2 (+ x1 w)
              :y2 (+ y1 h)}))})

(defn shape [s]
  (let [shape-name (s :shape)
        x (get-in s [:coords :x])
        y (get-in s [:coords :y])
        w 100
        h 100]
    [shape-name ((shapes shape-name) x y w h)]))

(defn switch-to [section-type section]
  (fn [e]
    (.preventDefault e)
    (swap! state activate section-type section)))

(defn next-item [s menu items]
  (let [current (s menu)]
    (->> items cycle (drop-while #(not= current %)) fnext)))

(defn switch-to-next [s menu items]
  (activate s menu (next-item s menu items)))

(def key-commands
  {"E" {:perform #(swap! state switch-to-next :editor [:level :tile])
        :description "Switch to next editor"}
   "T" {:perform #(swap! state switch-to-next :shape [:rect :circle :line])
        :description "Switch to next tool"}})

(defn page-did-mount []
  (let [tile-editor (by-id "tile-editor")
        level-editor (by-id "level-editor")
        keys (listen js/document events/EventType.KEYUP)
        tile-mouse-moves (listen tile-editor events/EventType.MOUSEMOVE)
        level-mouse-moves (listen level-editor events/EventType.MOUSEMOVE)]

    (go-loop []
      (alt!
        tile-mouse-moves  ([e _]
                           (swap! state update-coords e)
                           (recur))
        level-mouse-moves ([e _]
                           (swap! state update-coords e)
                           (recur))
        keys              ([e _]
                           (let [code (.-keyCode e)
                                 charcode (js/String.fromCharCode code)]
                             ((get-in key-commands [charcode :perform] #()))
                             (recur)))))

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
   [:h1.logo "Drawer"]
   [:p.source
    [:a {:href "https://github.com/camelpunch/drawer"} "GitHub"]]
   [:p (str "Coords: " (@state :coords))]
   [:ul.menu
    [menu-item :editor :level "Level Editor"]
    [menu-item :editor :tile "Tile Editor"]]

   [:ul.menu
    {:class (class-for @state :editor :level)}
    [menu-item :selected-tile :tile1 "Tile1"]]
   [:div#level-editor
    {:class (s/join " " ["workspace"
                         (class-for @state :editor :level)])}
    [:svg#level-editor]]

   [:ul.menu
    {:class (class-for @state :editor :tile)}
    [menu-item :shape :rect "Square"]
    [menu-item :shape :circle "Circle"]
    [menu-item :shape :line "Line"]]
   [:div
    {:class (s/join " " ["workspace"
                         (class-for @state :editor :tile)])}
    [:svg#tile-editor
     [shape @state]]]

   [:h3 "Keys:"]
   [:dl.keys
    (mapcat (fn [[k {description :description}]]
              [[:dt.key-name {:key k} k]
               [:dd.key-desc {:key description} description]])
            key-commands)]])

(defn page-component []
  (r/create-class {:render page
                   :component-did-mount page-did-mount}))

(r/render-component [page-component]
                    (by-id "app"))
