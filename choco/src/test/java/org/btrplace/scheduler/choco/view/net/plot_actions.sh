#!/bin/bash 

CSV_FILE="$1"
R_SCRIPT="gantt_script.R"

R --no-save --args "$CSV_FILE" < $R_SCRIPT

eog actions.png
