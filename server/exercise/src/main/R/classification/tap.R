library(emuR)
library(FactoMineR)
library(ggplot2)
library(grid)
library(gridExtra)

# Function to extract a collection of moving windows (of size `size`) from a example (time series) data.
#
# @param data time series data over which we will move our sampling window
# @param size size of sampling window
# @param inc  increment by which we move sampling window (default is 10 events)
# @returns    list of sampling windows extracted from the time series data (sample window includes data start and stop
#             indexes)
windowSampling = function(data, size, inc = 10) {
  dataSize = dim(data)[1]
  startIndexes = seq(1, dataSize, by=inc)
  lapply(startIndexes, function(index) { list(index, index+size-1, data[index:(index+size-1),]) })
}

# Function that overlays (in blue) current sample window on the example (time series) data.
#
# @param data       time series data over which we will move our sampling window
# @param startIndex start index to sample window
# @param endIndex   end index to sample window
graphWindowedData = function(data, startIndex, endIndex) {
  graphData = data.frame(t=rep(1:dim(data)[1]), data)
  xGraph = ggplot() +
    geom_line(mapping=aes(x=t, y=x), data=graphData) +
    annotate("rect", xmin=startIndex, xmax=endIndex, ymin=min(data["x"]), ymax=max(data["x"]), fill="blue", alpha=0.2) +
    xlab("time")
  yGraph = ggplot() +
    geom_line(mapping=aes(x=t, y=y), data=graphData) +
    annotate("rect", xmin=startIndex, xmax=endIndex, ymin=min(data["y"]), ymax=max(data["y"]), fill="blue", alpha=0.2) +
    xlab("time")
  zGraph = ggplot() +
    geom_line(mapping=aes(x=t, y=z), data=graphData) +
    annotate("rect", xmin=startIndex, xmax=endIndex, ymin=min(data["z"]), ymax=max(data["z"]), fill="blue", alpha=0.2) +
    xlab("time")
  grid.arrange(xGraph, yGraph, zGraph, main="Does this (blue) window contain a Tap event?")
}

# Function to split an input CSV file (holding accelerometer data) into a (saved) collection of sampling windows.
#
# @param input
# @param size
# @param inc
splitAndClassifyInput = function(input, size, inc = 10) {
  csv = read.csv(file=input, col.names=c("x", "y", "z"))
  baseFilename = sub("\\.csv", "", input)
  for (window in windowSampling(csv, size, inc)) {
    graphWindowedData(csv, as.integer(window[1]), as.integer(window[2]))
    cat("Does this (blue) window contain a Tap event? (y/N): ")
    answer = scan(what=character(), nmax=1, quiet=TRUE)
    if (toString(answer) == "y") {
      write.table(window[3], file=paste(baseFilename, "-", window[1], "-", window[2], "-tap", ".csv", sep=""), sep=",", row.names=FALSE, col.names=FALSE)
    } else {
      write.table(window[3], file=paste(baseFilename, "-", window[1], "-", window[2], ".csv", sep=""), sep=",", row.names=FALSE, col.names=FALSE)
    }
  }
}

# Function to extract a feature vector from a given set of example (time series) data. Internally, we extract feature
# vectors using discrete cosine transform (DCT) algorithm.
#
# @param data   time series data that we wish to extract a feature vector
# @param approx (positive) integer describing the number of coefficients (i.e. level of approximation) that the underlying
#               DCT algorithm should use
# @returns      feature vector that approximates the input (time series) data
featureVector = function(data, approx) {
  dct(data, approx, fit=FALSE)
}

# Function to calculate loadings (i.e. how to map [normalised] feature vectors into the [reduced] PCA space) from a
# principle component analysis (PCA) result data structure.
#
#   K' = K %*% W (where K is a feature vector, W is the loading matrix and K' is the reduced feature vector)
#
# @param pca PCA data structure (result of using FactorMineR's PCA function)
# @returns   loadings matrix
loadings = function(pca) {
  sweep(pca$var$coord, 2, sqrt(pca$eig[1:ncol(pca$var$coord), 1]), FUN="/")
}

# TODO: add in SVM training code!
