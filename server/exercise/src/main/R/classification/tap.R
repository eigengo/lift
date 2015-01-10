library(e1071)
library(emuR)
library(FactoMineR)
library(ggplot2)
library(grid)
library(gridExtra)

########################################################################################################################
#
# Data Tagging (for Supervised Learning)
#
########################################################################################################################

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

# User interaction function. Used to determine if the current sample window should be tagged (with a label) or not.
#
# @param data       time series data over which we will move our sampling window
# @param startIndex start index to sample window
# @param endIndex   end index to sample window
# @param label      label to tag (user) classified data with
labelInput = function(data, startIndex, endIndex, label) {
  graphWindowedData(data, startIndex, endIndex)
  cat("Does this (blue) window contain a Tap event? (y/N): ")
  answer = scan(what=character(), nmax=1, quiet=TRUE)
  if (toString(answer) == "y") {
    label
  } else {
    ""
  }
}

# Function to split an input CSV file (holding accelerometer data) into a (saved) collection of sampling windows. Used
# for DEBUGGING.
#
# @param input CSV file name holding accelerometer data
# @param size  size of sampling window
# @param inc   increment by which we move sampling window (default is 10 events)
splitAndClassifyInput = function(input, size, inc = 10) {
  csv = read.csv(file=input, col.names=c("x", "y", "z"))
  baseFilename = sub("\\.csv", "", input)
  for (window in windowSampling(csv, size, inc)) {
    label = labelInput(csv, as.integer(window[1]), as.integer(window[2]), "-tap")
    write.table(window[3], file=paste(baseFilename, "-", window[1], "-", window[2], label, ".csv", sep=""), sep=",", row.names=FALSE, col.names=FALSE)
  }
}

########################################################################################################################
#
# Feature Extraction
#
########################################################################################################################

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

# Function used to build and create file containing example feature vectors. Each row of the CSV data file represents a
# (user) labeled sample window encoded as a feature vector (extracted using DCT).
#
# @param input  CSV file name holding accelerometer data
# @param size   size of sampling window
# @param approx (positive) integer describing the number of coefficients (i.e. level of approximation) that the underlying
#               DCT algorithm should use
# @param tag    used to tag (user) classified data with
# @param inc    increment by which we move sampling window (default is 10 events)
buildFeatureVectors = function(input, size, approx, tag, inc = 10) {
  if (approx >= 3 * size) {
    cat("`approx` needs to be below 3 * `size`")
    return()
  }
  csv = read.csv(file=input, col.names=c("x", "y", "z"))
  baseFilename = sub("\\.csv", "", input)
  result = NULL
  for (window in windowSampling(csv, size, inc)) {
    label = labelInput(csv, as.integer(window[1]), as.integer(window[2]), tag)
    windowData = as.data.frame(window[3])
    xLabels = lapply(rep(1:size), function(index) { paste("x", index, sep="") })
    yLabels = lapply(rep(1:size), function(index) { paste("y", index, sep="") })
    zLabels = lapply(rep(1:size), function(index) { paste("z", index, sep="") })
    featureData = data.frame(t(windowData["x"]), t(windowData["y"]), t(windowData["z"]))
    names(featureData) = c(xLabels, yLabels, zLabels)
    feature = as.data.frame(featureVector(featureData, approx))
    rbind(result, data.frame(label, t(feature))) -> result
  }
  write.table(na.omit(result), file=paste(baseFilename, "-", tag, "-", approx, "-features", ".csv", sep=""), sep=",", row.names=FALSE, col.names=FALSE)
}

########################################################################################################################
#
# Dimensional Reduction of Feature Set
#
########################################################################################################################

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

# Function that applies PCA to the (previously) extracted feature vectors. In doing so, user interaction is used to
# determine (from data tables and data visualisations) the level of approximation (i.e. number of approximating
# coefficients that DCT will use).
#
# @param inputList list of CSV file names holding accelerometer data
# @param size      size of sampling window
# @param approx    (positive) integer describing the number of coefficients (i.e. level of approximation) that the underlying
#                  DCT algorithm should use
# @param tag       used to tag (user) classified data with
# @returns         PCA model loadings to be used to translate from example space into training space
reduceFeatureDimensions = function(inputList, size, approx, tag) {
  labels = lapply(rep(1:approx), function(index) { paste("f", index, sep="") })
  featureVectors = NULL
  for (input in inputList) {
    baseFilename = sub("\\.csv", "", input)
    fvInput = paste(baseFilename, "-", tag, "-", approx, "-features", ".csv", sep="")
    feature = read.csv(file=fvInput)
    featureData = data.frame(feature)
    names(featureData) = c("tag", t(labels))
    rbind(featureVectors, featureData) -> featureVectors
  }
  names(featureVectors) = c("tag", t(labels))
  featureVectors = na.omit(featureVectors)
  pca = PCA(featureVectors[,2:(approx+1)])
  print(pca$eig)
  cat("How many PCA dimensions do you wish to keep? ")
  reduction = scan(what=integer(), nmax=1, quiet=TRUE)
  pca = PCA(featureVectors[,2:(approx+1)], ncp=reduction, graph=FALSE)
  result = data.frame(featureVectors[,1], pca$ind$coord)
  write.table(result, file=paste("svm", "-", tag, "-", approx, "-reduced-", reduction, "-features", ".csv", sep=""), sep=",", row.names=FALSE, col.names=FALSE)
  loadings(pca)
}

########################################################################################################################
#
# Machine Learning
#
########################################################################################################################

# Function used to train a support vector machine (SVM).
#
# @param tag          tag that data has been (potentially) labeled with (for training)
# @param approx       (positive) integer describing the number of coefficients (i.e. level of approximation) that the underlying
#                     DCT algorithm should use
# @param reduction    number of dimensions kept during the PCA (dimensionality) reduction phase
# @param replications
# @returns            trained SVM model
trainSVM = function(tag, approx, reduction, replications = 10) {
  labels = lapply(rep(1:reduction), function(index) { paste("f", index, sep="") })
  data = read.csv(file=paste("svm", "-", tag, "-", approx, "-reduced-", reduction, "-features", ".csv", sep=""))
  names(data) = c("tag", t(labels))
  sampleSize = nrow(data)
  testData = sample(sampleSize, trunc(sampleSize/3))
  testSet = data[testData,]
  trainingSet = data[-testData,]
  cost = 100
  gamma = 1
  svm.model = svm(tag ~ ., data = trainingSet, cost = 100, gamma = 1, probability = TRUE)
  print("SVM confusion matrix and model accuracy rates:")
  svm.pred = predict(svm.model, testSet[,2:ncol(data)])
  svmTable = table(pred = svm.pred, true = testSet[,1])
  print(svmTable)
  print(classAgreement(svmTable))

  svm.model
}

# TODO: document
tuneSVM = function() {
  # TODO: implement
}

# Used to classify events in a given accelerometer data file (the `input`) using an SVM classifier.
#
# @param input       CSV file containing accelerometer data within which we need to classify events
# @param pcaLoadings PCA loadings - used to translate from example space into SVM trained data space
# @param svm         trained SVM model
# @param tag         label used to classify data
# @param size        sample window size (used to sample input for classification purposes)
# @param approx      level of approximation
# @param inc         distance by which sampling window will iteratively move
classify = function(input, pcaLoadings, svm, tag, size, approx, inc = 10) {
  csv = read.csv(file=input, col.names=(c("x", "y", "z")))
  labels = lapply(rep(1:approx), function(index) { paste("f", index, sep="") })

  for (window in windowSampling(csv, size, inc)) {
    feature = featureVector(window[3], approx)
    featureData = data.frame(feature)
    names(featureData) = c(t(labels))
    svmFeature = featureData %*% pcaLoadings
    pred = predict(svm, svmFeature, decision.values = TRUE, probability = TRUE)
    if (toString(attr(pred, "decision.values")[1,]) == tag) {
      print(paste("Tap present at: ", window[1], "; ending at: ", window[2], " (probability = ", attr(pred, "probabilities")[1,], ")"))
    }
  }
}
