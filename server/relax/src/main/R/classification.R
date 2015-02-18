library(e1071)
library(dtt)
library(ggplot2)
library(grid)
library(gridExtra)
library(pracma)

debug = FALSE

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
# @returns    list of sampling windows extracted from the time series data (sample window defined by data start and stop
#             indexes)
windowSampling = function(data, size, inc = 10) {
  dataSize = dim(data)[1]
  startIndexes = seq(1, dataSize - size, by=inc)
  lapply(startIndexes, function(index) { list(index, index+size-1) })
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

########################################################################################################################
#
# Feature Extraction
#
########################################################################################################################

# Function used to build and create file containing example feature vectors. Each row of the CSV data file represents a
# (user) labeled sample window encoded as a feature vector (extracted using DCT).
#
# @param input  CSV file name holding accelerometer data
# @param size   size of sampling window
# @param tag    used to tag (user) classified data with
# @param inc    increment by which we move sampling window (default is 10 events)
extractFeatures = function(inputList, size, tag, inc = 10) {
  xLabels = lapply(rep(1:size), function(index) { paste("x", index, sep="") })
  yLabels = lapply(rep(1:size), function(index) { paste("y", index, sep="") })
  zLabels = lapply(rep(1:size), function(index) { paste("z", index, sep="") })
  result = NULL
  for (input in inputList) {
    csv = read.csv(file=input, col.names=c("x", "y", "z"))
    for (window in windowSampling(csv, size, inc)) {
      startIndex = as.integer(window[1])
      endIndex = as.integer(window[2])
      label = labelInput(csv, startIndex, endIndex, tag)
      windowData = csv[startIndex:endIndex,]
      feature = as.data.frame(mvdct(as.matrix(windowData)))
      taggedFeature = data.frame(c(label, t(feature["x"]), t(feature["y"]), t(feature["z"])))
      names(taggedFeature) = c("feature")
      row.names(taggedFeature) = c("tag", xLabels, yLabels, zLabels)

      rbind(result, t(taggedFeature)) -> result
    }
  }
  write.table(result, file=paste("svm", "-", tag, "-features", ".csv", sep=""), sep=",", row.names=FALSE, col.names=FALSE)
}

########################################################################################################################
#
# SVM Machine Learning
#
########################################################################################################################

# Function used to calculate an N-fold cross validation cost.
#
# @param tag        tag that data has been (potentially) labeled with (for training)
# @param data       training data we are to use
# @param testData   randomised indexes into the data parameter
# @param buckets    number of buckets or folds to be used
# @param costParam  
# @param gammaParam 
parameterCost = function(tag, data, testData, buckets, costParam, gammaParam) {
  sampleSize = nrow(data)
  bucketCount = (sampleSize %/% buckets)-1
  extraCount = sampleSize %% buckets
  result = 0
  for (n in 1:bucketCount) { 
    testIndexes = testData[(n*buckets):((n+1)*buckets)]
    if (n <= extraCount) {
      testIndexes = c(testIndexes, testData[((bucketCount+1)*buckets) + n])
    }
    testSet = data[testIndexes,]
    trainingSet = data[-testIndexes,]
    svm.model = svm(tag ~ ., data = trainingSet, cost = costParam, gamma = gammaParam)
    svm.pred = predict(svm.model, testSet[,2:ncol(data)])
    svmTable = table(pred = svm.pred, true = testSet[,1])
    result = result + ((svmTable[1,1] + svmTable[2,2])/sum(svmTable))
  }

  result / bucketCount
}

# Function used to train a support vector machine (SVM). Trained SVM model is saved to a file.
#
# @param tag        tag that data has been (potentially) labeled with (for training)
# @param size       size of sampling window
# @param buckets    number of buckets or folds to be used
# @param repeats    how many attempts at fitting a model for a given cost and gamma parameter
# @param costParam
# @param gammaParam
trainSVM = function(tag, size, buckets = 10, repeats = 10, costParams = 10^(0:4), gammaParams = 10^(0:-4)) {
  data = read.csv(file=paste("svm", "-", tag, "-features", ".csv", sep=""))
  xLabels = lapply(rep(1:size), function(index) { paste("x", index, sep="") })
  yLabels = lapply(rep(1:size), function(index) { paste("y", index, sep="") })
  zLabels = lapply(rep(1:size), function(index) { paste("z", index, sep="") })
  names(data) = c("tag", xLabels, yLabels, zLabels)

  sampleSize = nrow(data)

  # Perform a grid search and locate the SVM model with the highest cost (in terms of an N-fold cross validation)
  bestCostParam = NULL
  bestGammaParam = NULL
  maxCost = NULL
  for (iter in 1:repeats) {
    testData = sample(sampleSize, sampleSize)
    for (costParam in costParams) {
      for (gammaParam in gammaParams) {
        cost = parameterCost(tag, data, testData, buckets, costParam, gammaParam)
        if (debug) {
          print(paste(costParam, gammaParam, cost))
        }
        if (is.null(bestCostParam)) {
          bestCostParam = costParam
          bestGammaParam = gammaParam
          maxCost = cost
          print(paste("[Initial step] SVM model (", costParam, ",", gammaParam, ") has ", buckets, "-fold cross correlation value of ", cost, sep=""))
        }
        if (cost > maxCost) {
          bestCostParam = costParam
          bestGammaParam = gammaParam
          maxCost = cost
          print(paste("[Search step] SVM model (", costParam, ",", gammaParam, ") has ", buckets, "-fold cross correlation value of ", cost, sep=""))
        }
      }
    }
  }
  print(paste("Tuned SVM model (", bestCostParam, ",", bestGammaParam, ") has ", buckets, "-fold cross correlation value of ", maxCost, sep=""))

  svm.model = svm(tag ~ ., data = data, cost = bestCostParam, gamma = bestGammaParam, probability = TRUE)
  saveRDS(svm.model, paste("svm-model", "-", tag, "-features", ".rds", sep=""))
  write.svm(svm.model, svm.file = paste("svm-model", "-", tag, "-features", ".libsvm", sep=""), scale.file = paste("svm-model", "-", tag, "-features", ".scale", sep=""))
}

# Function implementing a radial basis function - for reusing trained model in non-R environments
#
# @param x     input vector
# @param y     input vector (for efficiency, calling context calculates this RBF component)
# @param gamma "reach" (user defined parameter)
radial_kernel = function(x, y, gamma) {
  exp(-gamma * sum((x - y) * (x - y)))
}

# Taylor series approximation to the radial basis function - for reusing trained model in non-R environments on large data sets
#
# @param x      input vector
# @param y      input vector (for efficiency, calling context calculates this RBF component)
# @param gamma  "reach" (user defined parameter)
# @param degree degree to which taylor series will be expanded (around zero)
taylor_radial_kernel = function(x, y, gamma, degree = 2) {
  taylor_expansion = taylor(f = exp, x0 = 0, n = degree)
  exp(-gamma * sum(x * x)) * exp(-gamma * sum(y * y)) * polyval(taylor_expansion, gamma * sum(2 * x * y))
}

# Function that generates a function implementing data classification - for reusing trained model in non-R environments
#
# NOTE: following values need to be extracted from (R) SVM models for portability to non-R environments:
#   x.scale; tot.nSV; SV; gamma; coefs; rho; probA; probB
#
# @param tag label or tag this SVM has been classified for (used to load previously stored RDS SVM object)
# @param rbf radial basis function that we are to use (allows for approximation via taylor series etc.)
svm_predict = function(tag, rbf = radial_kernel) { 
  function(svm, data, probability = TRUE) {
    scaled_data = (data - svm$x.scale$"scaled:center") / svm$x.scale$"scaled:scale"
    result = sum(sapply(1:svm$tot.nSV, function(j) { rbf(svm$SV[j,], scaled_data, svm$gamma) * svm$coefs[j] })) - svm$rho
    if (probability) {
      probability = 1 / (1 + exp(svm$probA * result + svm$probB))
      attr(result, "probabilities") = matrix(c(probability, 1-probability), nrow = 1, ncol=2, dimnames=list(list("feature"), list("", tag)))
    }
    result
  }
}

# Used to classify events in a given accelerometer data file (the `input`) using an SVM classifier (which is loaded from
# a previously saved RDS file).
#
# @param input         CSV file containing accelerometer data within which we need to classify events
# @param tag           label used to classify data
# @param size          sample window size (used to sample input for classification purposes)
# @param inc           distance by which sampling window will iteratively move
# @param threshold     probability threshold - over this value and we classify window as being labeled by `tag`
# @param model.predict function to be used in making model predictions (default is to use e1071 `predict` function)
classify = function(input, tag, size, inc = 10, threshold = 0.75, model.predict = predict) {
  svm = readRDS(paste("svm-model", "-", tag, "-features", ".rds", sep=""))
  csv = read.csv(file=input, col.names=(c("x", "y", "z")))
  xLabels = lapply(rep(1:size), function(index) { paste("x", index, sep="") })
  yLabels = lapply(rep(1:size), function(index) { paste("y", index, sep="") })
  zLabels = lapply(rep(1:size), function(index) { paste("z", index, sep="") })

  graphData = data.frame(t=rep(1:nrow(csv)), csv)
  xGraph = ggplot() +
    geom_line(mapping=aes(x=t, y=x), data=graphData) +
    xlab("time")
  yGraph = ggplot() +
    geom_line(mapping=aes(x=t, y=y), data=graphData) +
    xlab("time")
  zGraph = ggplot() +
    geom_line(mapping=aes(x=t, y=z), data=graphData) +
    xlab("time")

  for (window in windowSampling(csv, size, inc)) {
    startIndex = as.integer(window[1])
    endIndex = as.integer(window[2])
    windowData = csv[startIndex:endIndex,]
    featureVector = as.data.frame(mvdct(as.matrix(windowData)))
    feature = data.frame(c(t(featureVector["x"]), t(featureVector["y"]), t(featureVector["z"])))
    names(feature) = c("feature")
    row.names(feature) = c(xLabels, yLabels, zLabels)

    pred = model.predict(svm, t(feature), probability = TRUE)
    probability = as.data.frame(attr(pred, "probabilities"))
    if (debug) {
      print(paste(window[1], format(round(probability[tag], 2), nsmall=2)))
    }
    if (probability[tag] >= threshold) {
      print(paste(tag, " present at: ", startIndex, "-", endIndex, " (probability = ", probability[tag], ")", sep=""))

      xGraph = xGraph +
        annotate("rect", xmin=startIndex, xmax=endIndex, ymin=min(csv["x"]), ymax=max(csv["x"]), fill="blue", alpha=0.2)
      yGraph = yGraph +
        annotate("rect", xmin=startIndex, xmax=endIndex, ymin=min(csv["y"]), ymax=max(csv["y"]), fill="blue", alpha=0.2)
      zGraph = zGraph +
        annotate("rect", xmin=startIndex, xmax=endIndex, ymin=min(csv["z"]), ymax=max(csv["z"]), fill="blue", alpha=0.2)
    }
  }

  grid.arrange(xGraph, yGraph, zGraph, main="SVM classified `tap` events")
}

########################################################################################################################
#
# Main REPL for training an SVM and classifying arbitrary user supplied input files (containing accelerometer data)
#
########################################################################################################################

# @param inputList TODO:
# @param size      TODO:
# @param tag       TODO:
# @param inc       TODO:
main = function(inputList, size, tag, inc = 10) {
  extractFeatures(inputList, size, tag, inc)

  trainSVM(tag, size)
}
