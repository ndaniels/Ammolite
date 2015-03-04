## ----echo=FALSE, results='hide',message=FALSE,warning=FALSE,error=FALSE-----------------------------------------------
options(width=120)
library(fmcsR) 
library(ChemmineOB)
library(knitcitations)
citep(c("10.1093/bioinformatics/btn186")) # first citatin always causes an error, so hide one here

## ----eval=FALSE-------------------------------------------------------------------------------------------------------
#  source("http://bioconductor.org/biocLite.R")
#  biocLite("fmcsR")

## ----quicktest1, eval=TRUE, fig=TRUE,fig.scap="Structures depictions of sample data."---------------------------------
library(fmcsR) 
data(fmcstest)
plot(fmcstest[1:3], print=FALSE) 

## ----quicktest2, eval=TRUE, fig=TRUE----------------------------------------------------------------------------------
test <- fmcs(fmcstest[1], fmcstest[2], au=2, bu=1) 
plotMCS(test,regenCoords=TRUE) 

## ----eval=TRUE, keep.source=TRUE--------------------------------------------------------------------------------------
library("fmcsR") # Loads the package 

## ----eval=FALSE, keep.source=TRUE-------------------------------------------------------------------------------------
#  library(help="fmcsR") # Lists functions/classes provided by fmcsR
#  library(help="ChemmineR") # Lists functions/classes from ChemmineR
#  vignette("fmcsR") # Opens this PDF manual
#  vignette("ChemmineR") # Opens ChemmineR PDF manual

## ----eval=FALSE, keep.source=TRUE-------------------------------------------------------------------------------------
#  ?fmcs
#  ?"MCS-class"
#  ?"SDFset-class"

## ----eval=TRUE, keep.source=TRUE--------------------------------------------------------------------------------------
data(fmcstest) 
sdfset <- fmcstest
sdfset 

## ----eval=FALSE, keep.source=TRUE-------------------------------------------------------------------------------------
#  write.SDF(sdfset, file="sdfset.sdf")
#  mysdf <- read.SDFset(file="sdfset.sdf")

## ----eval=TRUE, keep.source=TRUE--------------------------------------------------------------------------------------
mcsa <- fmcs(sdfset[[1]], sdfset[[2]]) 
mcsa 
mcsb <- fmcs(sdfset[[1]], sdfset[[3]]) 
mcsb 

## ----eval=TRUE, keep.source=TRUE--------------------------------------------------------------------------------------
fmcs(sdfset[1], sdfset[2], fast=TRUE)

## ----eval=TRUE, keep.source=TRUE--------------------------------------------------------------------------------------
slotNames(mcsa) 

## ----eval=TRUE, keep.source=TRUE--------------------------------------------------------------------------------------
stats(mcsa) # or mcsa[["stats"]] 
mcsa1 <- mcs1(mcsa) # or mcsa[["mcs1"]] 
mcsa2 <- mcs2(mcsa) # or mcsa[["mcs2"]] 
mcsa1[1] # returns SDFset component
mcsa1[[2]][1:2] # return first two index vectors 

## ----eval=TRUE, fig=TRUE, keep.source=TRUE----------------------------------------------------------------------------
mcstosdfset <- mcs2sdfset(mcsa, type="new")
plot(mcstosdfset[[1]], print=FALSE) 

## ----eval=TRUE, keep.source=TRUE--------------------------------------------------------------------------------------
mylist <- list(stats=stats(mcsa), mcs1=mcs1(mcsa), mcs2=mcs2(mcsa)) 
as(mylist, "MCS") 

## ----au0bu0, eval=TRUE, fig=TRUE--------------------------------------------------------------------------------------
plotMCS(fmcs(sdfset[1], sdfset[2], au=0, bu=0)) 

## ----au1bu1, eval=TRUE, fig=TRUE--------------------------------------------------------------------------------------
plotMCS(fmcs(sdfset[1], sdfset[2], au=1, bu=1)) 

## ----au2bu2, eval=TRUE, fig=TRUE--------------------------------------------------------------------------------------
plotMCS(fmcs(sdfset[1], sdfset[2], au=2, bu=2)) 

## ----au0bu013, eval=TRUE, fig=TRUE------------------------------------------------------------------------------------
plotMCS(fmcs(sdfset[1], sdfset[3], au=0, bu=0)) 

## ----eval=TRUE, keep.source=TRUE--------------------------------------------------------------------------------------
data(sdfsample) # Loads larger sample data set 
sdf <- sdfsample 
fmcsBatch(sdf[1], sdf[1:30], au=0, bu=0) 

## ----tree, eval=TRUE, fig=TRUE----------------------------------------------------------------------------------------
sdf <- sdf[1:7] 
d <- sapply(cid(sdf), function(x) fmcsBatch(sdf[x], sdf, au=0, bu=0, matching.mode="aromatic")[,"Overlap_Coefficient"]) 
d 
hc <- hclust(as.dist(1-d), method="complete")
plot(as.dendrogram(hc), edgePar=list(col=4, lwd=2), horiz=TRUE) 

## ----au0bu024, eval=TRUE, fig=TRUE------------------------------------------------------------------------------------
plotMCS(fmcs(sdf[3], sdf[7], au=0, bu=0, matching.mode="aromatic")) 

## ----sessionInfo,  print=TRUE-----------------------------------------------------------------------------------------
 sessionInfo()

## ----biblio, echo=FALSE, results='asis'-------------------------------------------------------------------------------
   bibliography()

