require(grid)
library(ggplot2)

options(echo=FALSE)

path = "/home/vkherbac/Documents/btrplace/scheduler-network/choco/src/test/java/org/btrplace/scheduler/choco/view/net/nancy/macro/capping"
path = "."

df1 <- read.csv(paste(c(path,"energy.csv"), collapse='/'), header=TRUE, sep=",")
df2 <- read.csv(paste(c(path,"energy-5200.csv"), collapse='/'), header=TRUE, sep=",")
df3 <- read.csv(paste(c(path,"energy-5000.csv"), collapse='/'), header=TRUE, sep=",")

p <- ggplot()

p <- p + geom_line(data=df1, aes(x=TIME, y=POWER, colour="no"), linetype="solid", size=1)
p <- p + geom_line(data=df2, aes(x=TIME, y=POWER, colour="5200"), linetype="solid", size=1)
p <- p + geom_line(data=df3, aes(x=TIME, y=POWER, colour="5000"), linetype="solid", size=1)
#p <- p + geom_point(colour="red", size=4, shape=21, fill="white")

##################### Axis ##################
p <- p + xlab("Time (min.)")
p <- p + ylab("Power (kW)")

# watts to kW
p <- p + scale_y_continuous(limits=c(2800,6000),labels=function(x)x/1000, breaks=c(3000, 3500, 4000, 4500, 5000, 5500, 6000))
# sec to min
p <- p + scale_x_continuous(limits=c(-30,1140),
                            breaks=c(-30,0,60,120,180,240,300,360,420,480,540,600,660,720,780,840,900,960,1020,1080,1140),
                            labels=c("", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19"))
#############################################


#################### Theme ####################
p <- p + theme_bw() +
  
  #eliminates background, gridlines, and chart border
  theme(
    plot.background = element_blank()
    ,panel.grid.major = element_blank()
    ,panel.grid.minor = element_blank()
    ,panel.border = element_blank()
    ,panel.background = element_blank()
  )

# Show the border
p <- p + theme(panel.background = element_rect(linetype = "solid", colour = "black"))

# Font size
p <- p + theme(text = element_text(size=20))

# Axis label
p <- p + theme(axis.text = element_text(size = 18))
p <- p + theme(axis.title.x = element_text(size = 20, angle = 0, vjust = -0.5))
p <- p + theme(axis.title.y = element_text(size = 20, angle = 90, vjust = 1.5))
###############################################


############### Legend ################
p <- p + theme(legend.key.width=unit(3,"line"))
p <- p + theme(legend.text = element_text(size = 18))
#p <- p + theme(legend.title = element_text(size = 18, face="plain", lineheight=0.3))
p <- p + theme(legend.title = element_blank())
p <- p + theme(legend.background = element_rect(colour = "NA", fill="transparent"))
p <- p + theme(legend.position = c(1, 1), legend.justification = c(1, 1))
p <- p + scale_colour_discrete(name="Capping type \n", breaks=c("no","5200","5000"), labels=c(" No capping", " 5.2 kW capping"," 5 kW capping"))
#######################################

#p

# Save file
ggsave(p, file="capping.pdf", width=10, height=6)

