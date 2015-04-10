(ns drawer.dev
    (:require
     [drawer.core :as core]
     [figwheel.client :as fw]))

(fw/start {:websocket-url "ws://localhost:3449/figwheel-ws"
           :on-jsload (fn []
                        (core/stop)
                        (core/mount-root!))})

(core/mount-root!)
