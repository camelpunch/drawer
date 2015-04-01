#!/bin/bash

set -ex

lein clean
lein cljsbuild once min
cd resources/public
gzip -k --best js/compiled/drawer.js
cf push
