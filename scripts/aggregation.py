from subprocess import call
from os import mkdir, listdir
import sys

def buildCompressedDB(sdfName, dbName):
	command = "./Ammolite compress -s {} -t {}".format(sdfName, dbName)
	print( "Running: "+ command)
	call(command, shell=True)


def buildAggregate(dbName, aggName, compressionRatio):
	command = "./Ammolite aggcompress -l -s {} -t {} -r {}".format(dbName, aggName, compressionRatio)
	print( "Running: "+ command)
	call(command, shell=True)

def buildAggs(path, name):
	folderName = name + "-aggs"
	mkdir(folderName)
	dbName =  folderName + "/" +name 
	buildCompressedDB(path,dbName)
	dbName =  dbName + ".adb"
	for cRatio in [0.6,0.7,0.8,0.9]:
		aggName = "{}/{}_{}".format(folderName,name,cRatio)
		buildAggregate(dbName, aggName, cRatio)
	return folderName

" ./Ammolite aggsearch -c 1k_pseudolinear_7.clusters.adb -q molecule_sets/smalls/100_random_molecules.sdf -t 0.8 --target DEV-TEST 2>&1 | tee 100_over_1k.log"

def searchAggregate( aggregate, queries, threshold, compBound):
	logName = "{}_{}_{}".format(aggregate.split(".")[0], queries.split("/")[-1], threshold)
	command = "./Ammolite aggsearch -c {} -q {} -t {} -s {} --target DEV-TEST 2>&1 | tee {}".format(aggregate, queries, threshold, compBound, logName)
	print( "Running: "+ command)
	call( command, shell=True)

def searchAggs( folderName, queries):
	files = listdir( folderName)
	files = [ f for f in files if f.split(".")[-2] == "clusters"]
	for f in files:
		for thresh in [0.6, 0.7, 0.8, 0.9]:
			for compBound in [0.4, 0.5, 0.6, 0.7, 0.8]:
				fName = "{}/{}".format(folderName, f)
				searchAggregate(fName, queries, thresh, compBound)



if __name__ == "__main__":
	args = sys.argv
	folderName = buildAggs(args[1], args[2])
	searchAggs(folderName, args[1])