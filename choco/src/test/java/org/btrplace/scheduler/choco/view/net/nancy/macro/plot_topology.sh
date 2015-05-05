#!/bin/bash

DOT_FILE="$1"

dot -Tpng "$DOT_FILE" > topology.png

eog topology.png &
