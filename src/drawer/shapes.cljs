(ns drawer.shapes)

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

(defn shape [shape-name coords]
  (let [x (coords :x)
        y (coords :y)
        w 100
        h 100]
    [shape-name ((shapes shape-name) x y w h)]))
