#!/bin/bash 

CSV_FILE="$1"

gnuplot -persist <<PLOT

set terminal png

set title "Energy consumption"
set xlabel "Time (s)"
set ylabel "Power (Watts)"

set datafile separator ","

set output "energy.png"

#set xtics 50

plot '$CSV_FILE' using 1:2 with lines title "Consumption", '$CSV_FILE' using 1:3 with lines title "Budget";

quit
PLOT

eog energy.png
