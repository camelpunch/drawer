(ns drawer.keybindings
  (:require [drawer.commands :as c]))

(def key-commands
  {"E" {:transition #(c/switch-to-next % :editor [:level :tile])
        :description "Switch to next editor"}
   "B" {:transition #(c/switch-to-next % :shape [:rect :circle :line])
        :description "Switch to next brush"}
   "T" {:transition c/switch-to-next-tile
        :description "Switch to next tile"}})
