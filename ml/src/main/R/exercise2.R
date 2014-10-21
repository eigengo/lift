# data <- scale(read.csv("/Users/janmachacek/accel-1413803254.927871.csv", col.names=c('x', 'y', 'z'))) # chest
data <- scale(read.csv("/Users/janmachacek/accel-1413542383.400994.csv", col.names=c('x', 'y', 'z'))) # arms

# K-Means Cluster Analysis
fit <- kmeans(data, 4)
# get cluster means 
aggregate(data, by=list(fit$cluster), FUN=mean)
# append cluster assignment
data <- data.frame(data, fit$cluster)

library(cluster) 
clusplot(data, fit$cluster, color=TRUE, shade=TRUE, labels=0, lines=0)
