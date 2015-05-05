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

set datafile separator ","

set output "energy.png"

#plot 'energy.csv' using 1:2 with lines lw 2 lc 2 title "Consumption", '$CSV_FILE' using 1:3 with lines lw 1 lc 1 title "Budget";
plot 'energy.csv' using 1:2 with lines lw 2 lc 1 title "No capping", \
     'energy-5200.csv' using 1:2 with lines lw 2 lc 2 title "5.2 kW capping", \
     'energy-5150.csv' using 1:2 with lines lw 2 lc 3 title "5.15 kW capping", \
     'energy-5100.csv' using 1:2 with lines lw 2 lc 4 title "5.1 kW capping", \
     'energy-5050.csv' using 1:2 with lines lw 2 lc 5 title "5.05 kW capping", \
     'energy-5000.csv' using 1:2 with lines lw 2 lc 6 title "5 KW capping";

PLOT

eog energy.png &
