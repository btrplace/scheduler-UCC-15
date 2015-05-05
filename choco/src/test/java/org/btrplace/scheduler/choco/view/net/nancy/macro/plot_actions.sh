#!/bin/bash 

CSV_FILE="$1"

R --no-save --args "$CSV_FILE" <<PLOT

library(ggplot2)

args <- commandArgs(trailingOnly = TRUE)
file=args[1]

df <- read.csv(c(file), header=TRUE, sep=',')

#df\$VMLabel <- df\$VM
df\$NODELabel <- df\$NODE

#df\$VM <- as.numeric(df\$VM)
df\$VM <- factor(df\$VM, levels=unique(as.character(df\$VM)))

p <- ggplot(df, aes(colour=NODELabel))

p <- p + theme_bw()
p <- p + geom_segment(aes(x=Start,xend=End,y=VM,yend=VM),size=1)
p <- p + geom_point(aes(x=Start,y=VM),size=2)
p <- p + geom_point(aes(x=End,y=VM),size=2)
#p <- p + scale_x_continuous(breaks=round(seq(0,max(df\$End),by=5),1))
p <- p + scale_x_continuous(breaks=round(seq(min(df\$Start),max(df\$End),by=300),1))
#p <- p + scale_x_continuous(breaks=round(seq(min(df\$Start),max(df\$End),by=60),1),labels=c(0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24))

#p <- p + geom_text(aes(x=(Start+End)/2,
#                       y=VM+0.25,
#                       label=VMLabel),
#                   fontface="bold")

p <- p + theme(
             legend.position="None",
             panel.grid.major = element_blank(),
             axis.text.y = element_blank())
p <- p + xlab("Duration (s)")
p <- p + ylab("Tasks")

# Title
p <- p + ggtitle("Actions Gantt chart")
p <- p + theme(plot.title = element_text(lineheight=2, face=quote(bold)))

#p
#ggsave(p, file = "./plot.eps", width = 7, height = 7, device=cairo_ps)
ggsave(p, file = "./actions.png", width = 7, height = 7)

PLOT

eog actions.png &
