data <- scale(read.csv("/Users/janmachacek/Eigengo/lift/ml/src/main/resources/accel-1413803254.927871.csv", nrows=10000)) # chest day
# data <- scale(read.csv("/Users/janmachacek/Eigengo/lift/ml/src/main/resources/accel-1413542383.400994.csv")) # arms day

# Determine number of clusters
wss <- (nrow(data)-1) * sum(apply(data, 2, var))
count <- 10
for (i in 2:count) wss[i] <- sum(kmeans(data, centers=i)$withinss)
plot(1:count, wss, type="b", xlab="Number of Clusters", ylab="Within groups sum of squares")

# Ward Hierarchical Clustering
d <- dist(data, method="euclidean") # distance matrix
fit <- hclust(d, method="ward") 
plot(fit) # display dendogram
groups <- cutree(fit, k=5) # cut tree into 5 clusters
# draw dendogram with red borders around the 5 clusters 
rect.hclust(fit, k=5, border="red")