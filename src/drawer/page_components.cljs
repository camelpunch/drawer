(ns drawer.page-components
  (:require [clojure.string :as s]
            [drawer.commands :as c]
            [drawer.shapes :as sh]
            [drawer.keybindings :as kb]))

(defn- add-impression-key [imp]
  (update-in imp [1] merge {:key (str "imp-" (rand-int 999999))}))

(defn- tile-component [{impressions :impressions}]
  [:g (when-not (empty? impressions)
        (map add-impression-key impressions))])

(defn- stringify [x]
  "Like name, but works with numbers."
  (s/replace (str x) #"^:" ""))

(defn- class-for [current k v]
  (s/join " " [(s/join "-" (map stringify [v k]))
           (if (= v current) "active" "inactive")]))

(defn page [state]
  (let [switch-to (fn [section-type section]
                    (fn [e]
                      (.preventDefault e)
                      (swap! state c/activate section-type section)))

        menu-item (fn [current-item menu item human-name]
                    [:li.menu-item
                     {:key (str menu item human-name)}
                     [:a.btn
                      {:id (stringify item)
                       :href "#"
                       :class (class-for current-item menu item)
                       :on-click (switch-to menu item)}
                      human-name]])

        {:keys [tile-coords level-coords editor shape tiles tile
                tiles-wide tiles-high tile-width]} @state]
    [:div.ctnr
     [:header.hd
      [:h1.logo "Drawer"]
      [:p.source
       [:a {:href "https://github.com/camelpunch/drawer"} "GitHub"]]]

     [:main.mn
      [:p (str "Level coords: " level-coords)]
      [:p (str "Tile coords: " tile-coords)]
      [:ul.menu.edtrs
       [menu-item editor :editor :level "Level Editor"]
       [menu-item editor :editor :tile "Tile Editor"]]

      [:div#level-editor
       {:class (s/join " " ["workspace"
                            (class-for editor :editor :level)])}
       [:svg#level-editor
        {:width (* tiles-wide tile-width)
         :height (* tiles-high tile-width)}]]

      [:ul.menu.brshs
       {:class (class-for editor :editor :tile)}
       [menu-item shape :shape :rect "Square"]
       [menu-item shape :shape :circle "Circle"]
       [menu-item shape :shape :line "Line"]]

      [:div
       {:class (s/join " " ["workspace"
                            (class-for editor :editor :tile)])}
       [:svg#tile-editor
        {:width (* tiles-wide tile-width)
         :height (* tiles-high tile-width)}
        [tile-component (tiles tile)]
        [sh/shape shape tile-coords]]]

      [:ul.menu
       (map #(menu-item tile :tile (% :id) (% :name))
            tiles)]]

     [:aside.asd
      [:h3 "Keys:"]
      [:dl.keys
       (mapcat (fn [[k {description :description}]]
                 [[:dt.key-name {:key (str k description)} k]
                  [:dd.key-desc {:key description} description]])
               kb/key-commands)]]]))
