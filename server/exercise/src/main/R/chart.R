data <- scale(read.csv("/Users/janmachacek/x.csv", col.names=c('x', 'y', 'z')))
data.ts <- ts(data)

plot(data.ts)

#library(scatterplot3d)
#scatterplot3d(data, highlight.3d=TRUE)
