import matplotlib.pyplot as plt
import numpy as np
import sys

class SMSDResult:

	def __init__(self, line):
		self.parseLine( line)

	def parseLine(self, line):
		l = line.split()
		self.id1 = l[0]
		self.id2 = l[1]
		self.size1 = int(l[2])
		self.size2 = int(l[3])
		self.overlapSize = int(l[4])
		self.time = int(l[5])

	def overlapCoeff(self):
		return float(self.overlapSize) / min(self.size1, self.size2)

class AmmoliteCoarseResult:
	def __init__(self, line):
		self.parseLine( line)

	def parseLine(self, line):
		l = line.split()
		self.id1 = l[0]
		self.id2 = l[1]
		self.actualSize1 = int(l[2])
		self.actualSize2 = int(l[3])
		self.compressedSize1 = int(l[4])
		self.compressedSize2 = int(l[5])
		self.overlapSize = int(l[6])
		self.time = int(l[7])

	def overlapCoeff(self):
		return float(self.overlapSize) / min(self.compressedSize1, self.compressedSize2)

class ResultTable:
	def __init__(self):
		self.dic = {}

	def addResult(self, result):
		ids = sorted([result.id1, result.id2])
		if ids[0] in self.dic:
			self.dic[ids[0]][ids[1]] = result
		else:
			subDic = {ids[1]:result}
			self.dic[ids[0]] = subDic

	def getResult(self, id1, id2):
		ids = sorted([id1,id2])
		if ids[0] in self.dic:
			if ids[1] in self.dic[ids[0]]:
				d = self.dic[ids[0]]
				return d[ ids[1]]
		return None




def parseFiles( smsdFilename, ammFilename):
	smsdResults = []
	with open(smsdFilename) as smsd:
		inData = False
		for line in smsd:
			if not inData:
				l = line.split()
				if "BEGIN_DATA" in l:
					inData = True
			elif inData:
				l = line.split()
				if "END_DATA" in l:
					inData = False
				else:
					res = SMSDResult( line)
					smsdResults.append(res)

	ammResults = []
	with open(ammFilename) as amm:
		inData = False
		for line in amm:
			if not inData:
				l = line.split()
				if "BEGIN_DATA" in l:
					inData = True
			elif inData:
				l = line.split()
				if "END_DATA" in l:
					inData = False
				else:
					res = AmmoliteCoarseResult( line)
					ammResults.append(res)

	return (smsdResults, ammResults)

def getAveNumSMSDMatches(fine, smsdResults):
	matchTable = {}
	resTable = ResultTable()
	smsdMatches = 0
	for sRes in smsdResults: 
		if sRes.overlapCoeff() >= fine:
			smsdMatches += 1
			resTable.addResult(sRes)

	aveSMSDMatches = float(smsdMatches) / len(smsdResults)

	return aveSMSDMatches, resTable

def getAveNumAmmMatches(coarse, fine, resTable, ammResults):

	ammMatches = 0
	for aRes in ammResults:
		if aRes.overlapCoeff() >= coarse:
			sRes = resTable.getResult( aRes.id1, aRes.id2)
			if sRes != None:
				ammMatches += 1

	aveAmmMatches = float(ammMatches) / len(ammResults)

	return aveAmmMatches

def getTime(smsdResults, ammResults, fineCutoff):

	smsdTime, n = 0, 0
	for sRes in smsdResults:
		smsdTime += sRes.time
		n += 1
	print("Comparisons for smsd: {}".format(n))
	smsdTime /= n

	resolution = 5
	evalPoints = [n/float(resolution) for n in range(resolution)]

	ammTimes = []
	for coarseCutoff in evalPoints:
		ammTime, n = 0, 0
		numCoarseHits = 0
		coarseComparisons, fineComparisons = 0, 0
		coarseMatches = ResultTable()
		for aRes in ammResults:
			coarseComparisons += 1
			ammTime += aRes.time
			n += 1
			if aRes.overlapCoeff() >= coarseCutoff:
				coarseMatches.addResult(aRes)
				numCoarseHits += 1
		for sRes in smsdResults:
			if coarseMatches.getResult(sRes.id1, sRes.id2) != None:
				ammTime += sRes.time
				fineComparisons += 1

		attemptHitRatio = numCoarseHits / float(coarseComparisons)
		ammTime /= n
		ammTimes.append(ammTime)
		print("Coarse comparisons: {} Number of coarse hits: {} Ratio: {} Coarse Cutoff: {}".format(coarseComparisons, fineComparisons, numCoarseHits, coarseCutoff))

	makeGraph(evalPoints, [t/float(smsdTime) for t in ammTimes], "Coarse Threshold", "Time Ratio", "Comparison of Time" )



def getData(smsdResults, ammResults):
	pts = []
	k = 25
	data = np.zeros((k,k))
	Y = []
	X = []
	for i,fine in enumerate( [n/float(k) for n in range(0,k)]):
		if fine > 0.4:
			aveSMSD, resTable = getAveNumSMSDMatches(fine, smsdResults)
			for j,coarse in enumerate( [n/float(k) for n in range(0,k)]):
				if coarse <= fine:
					aveAmm = getAveNumAmmMatches(coarse,fine,resTable,ammResults)
					ratio = aveAmm / aveSMSD
					data[i,j] = ratio
					Y.append( ratio)
					X.append( coarse / fine)

	return data, [n/float(k) for n in range(0,k)], [n/float(k) for n in range(0,k)], X, Y

def buildHeatMap( data, column_labels, row_labels, rowName, colName, outname="Heat Map"):


	fig, ax = plt.subplots()
	heatmap = ax.pcolor(data, cmap=plt.cm.jet)

	# put the major ticks at the middle of each cell
	ax.set_xticks(np.arange(data.shape[0])+0.5, minor=False)
	ax.set_yticks(np.arange(data.shape[1])+0.5, minor=False)

	ax.set_xticklabels(row_labels, minor=False)
	ax.set_yticklabels(column_labels, minor=False)

	ax.set_xlabel(rowName)
	ax.set_ylabel(colName)

	fig.savefig(outname)
	plt.show()

def makeGraph(X,Y, xName, yName, name="NoName"):
	fig = plt.figure()
	ax = fig.add_subplot(111)
	superName = "Comparison of {} and {}".format(xName,yName)
	outname = "{} from {}.png".format(superName,name)
	fig.suptitle(superName)
	ax.scatter(X,Y)

	ax.set_xlabel('{}'.format(xName))
	ax.set_ylabel('{}'.format(yName))
	fig.savefig(outname)

def main( args):
	smsdFilename = args[1]
	ammFilename = args[2]
	smsdResults, ammResults = parseFiles(smsdFilename, ammFilename)

	if len(args) == 3:	
		data, cols, rows, X, Y = getData(smsdResults, ammResults)
		buildHeatMap(data, cols, rows, "coarse threshold", "fine threshold")
		makeGraph(X,Y, "coarse_fine_ratio", "smsd_amm_ratio")
	if len(args) == 4:
		fine = float( args[3])
		getTime(smsdResults, ammResults, fine)

if __name__ == "__main__":
	main(sys.argv)











