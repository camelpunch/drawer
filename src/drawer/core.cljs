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

(defn new-tile []
  {:impressions []})

(defonce state
  (r/atom {:editor :level
           :shape :circle
           :coords {:x 0 :y 0}
           :tile :a
           :tiles {:a (new-tile)
                   :b (new-tile)
                   :c (new-tile)
                   :d (new-tile)}}))

(def tile-width 100)
(def tiles-wide 5)
(def tiles-high 4)

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

(defn coords-from-event [e]
  {:x (.-offsetX e) :y (.-offsetY e)})

(defn update-coords [s e]
  (assoc s :coords (coords-from-event e)))

(defn grid-align [pos]
  (- pos (mod pos tile-width)))

(defn update-grid-coords [s e]
  (assoc s :coords (-> (coords-from-event e)
                       (update-in [:x] grid-align)
                       (update-in [:y] grid-align))))

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
   "B" {:perform #(swap! state switch-to-next :shape [:rect :circle :line])
        :description "Switch to next brush"}
   "T" {:perform #(swap! state switch-to-next :tile [:a :b :c :d])
        :description "Switch to next tile"}})

(defn paint [s]
  (update-in s [:tiles (s :tile) :impressions] conj (shape s)))

(defn event-charcode [e]
  (js/String.fromCharCode (.-keyCode e)))

(defn page-did-mount []
  (let [tile-editor (by-id "tile-editor")
        level-editor (by-id "level-editor")
        keys (listen js/document events/EventType.KEYUP)
        tile-mouse-moves (listen tile-editor events/EventType.MOUSEMOVE)
        tile-mouse-clicks (listen tile-editor events/EventType.CLICK)
        level-mouse-moves (listen level-editor events/EventType.MOUSEMOVE)]

    (go-loop []
      (alt!
        tile-mouse-moves  ([e _] (swap! state update-coords e)
                           (recur))
        tile-mouse-clicks ([_ _] (swap! state paint)
                           (recur))
        level-mouse-moves ([e _] (swap! state update-grid-coords e)
                           (recur))
        keys              ([e _]
                           ((get-in key-commands [(event-charcode e) :perform] #()))
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

(defn add-key [imp]
  (update-in imp [1] merge {:key (str "imp-" (rand-int 999999))}))

(defn tile-component [t]
  (map add-key (t :impressions)))

(defn page []
  [:div.ctnr
   [:header.hd
    [:h1.logo "Drawer"]
    [:p.source
     [:a {:href "https://github.com/camelpunch/drawer"} "GitHub"]]]

   [:main.mn
    [:p (str "Coords: " (@state :coords))]
    [:ul.menu.edtrs
     [menu-item :editor :level "Level Editor"]
     [menu-item :editor :tile "Tile Editor"]]

    [:div#level-editor
     {:class (s/join " " ["workspace"
                          (class-for @state :editor :level)])}
     [:svg#level-editor
      {:width (* tiles-wide tile-width)
       :height (* tiles-high tile-width)}]]

    [:ul.menu.brshs
     {:class (class-for @state :editor :tile)}
     [menu-item :shape :rect "Square"]
     [menu-item :shape :circle "Circle"]
     [menu-item :shape :line "Line"]]

    [:div
     {:class (s/join " " ["workspace"
                          (class-for @state :editor :tile)])}
     [:svg#tile-editor
      {:width (* tiles-wide tile-width)
       :height (* tiles-high tile-width)}
      (tile-component (get-in @state [:tiles (@state :tile)]))
      [shape @state]]]

    [:ul.menu
     [menu-item :tile :a "Tile A"]
     [menu-item :tile :b "Tile B"]
     [menu-item :tile :c "Tile C"]
     [menu-item :tile :d "Tile D"]]]

   [:aside.asd
    [:h3 "Keys:"]
    [:dl.keys
     (mapcat (fn [[k {description :description}]]
               [[:dt.key-name {:key k} k]
                [:dd.key-desc {:key description} description]])
             key-commands)]]])

(defn page-component []
  (r/create-class {:render page
                   :component-did-mount page-did-mount}))

(r/render-component [page-component]
                    (by-id "app"))
