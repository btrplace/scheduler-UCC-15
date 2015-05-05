#!/bin/bash 

CSV_FILE="$1"

gnuplot <<PLOT

set terminal png

set title "Energy consumption"
set xlabel "Time (minutes)"
set ylabel "Power (Watts)"

# Seconds to minutes
set timefmt "%s"
set xdata time
set xtics format "%M"

#set xrange ["0":"3000"]
#set yrange [2500:3000]

set datafile separator ";"

set output "energy.png"

plot '$CSV_FILE' using 1:2 with lines lw 2 lc 2 title "Consumption", '$CSV_FILE' using 1:3 with lines lw 1 lc 1 title "Budget";

PLOT

eog energy.png &
