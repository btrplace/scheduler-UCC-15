#!/bin/bash

INPUT_FILE="$1"

dot -Tpng "$INPUT_FILE" > topology.png

eog topology.png
