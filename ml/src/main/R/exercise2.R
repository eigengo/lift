# data <- scale(read.csv("/Users/janmachacek/accel-1413803254.927871.csv", col.names=c('x', 'y', 'z'))) # chest
raw <- scale(read.csv("/Users/janmachacek/Eigengo/lift/ml/src/main/resources/chest1-1.csv", col.names=c('x', 'y', 'z'))) # arms
data <- ts(raw)

# K-Means Cluster Analysis
fit <- kmeans(data, 4)
# get cluster means 
aggregate(data, by=list(fit$cluster), FUN=mean)
# append cluster assignment
data <- data.frame(data, fit$cluster)

library(cluster) 
clusplot(data, fit$cluster, color=TRUE, shade=TRUE, labels=0, lines=0)
