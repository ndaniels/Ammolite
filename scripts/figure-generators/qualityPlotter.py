import matplotlib.pyplot as plt
from pylab import savefig
from os import listdir
import sys

def graphData( X, Y, name, xLabel=None, YLabel = None):
	plt.figure(name)
	if xLabel:
		plt.xlabel(xLabel)
	if yLabel:
		plt.ylabel(yLabel)
	plt.scatter(X, Y)
	savefig(name + ".png")

def overlapCoeff(a,b,overlap):
	return (1.0 * overlap) / min((a,b))

def tanimotoCoeff(a,b,overlap):
	return (1.0 * overlap)/(a+b-overlap)

def parseFile(fileName):
	lines = []
	with open(fileName) as f:
		lines = [line for line in f]
	lines = lines[1:]
	numMatches = [int(l.split()[-1]) for l in lines[::2]]
	matchDetails = []

	for i,l in enumerate(lines[1::2]):
		if l != "\n":

			l = l.split("(")
			l = [el for el in l if el not in [""," "]]
			thisMatch = []
			for phrase in l:
				phrase = phrase.strip()
				phrase = phrase.split(", ")
				phrase[-1] = phrase[-1][:-1]
				phrase = [int(el) for el in phrase]
				thisMatch.append(phrase)
			if len(thisMatch) != numMatches[i]:
				print( thisMatch)
				print( numMatches[i])
				assert False
			matchDetails.append(thisMatch)
	

	aveOverlaps = []
	aveTanimotos = []
	for mSet in matchDetails:
		sumOverlap = 0
		sumTanimoto = 0
		for m in mSet:
			sumOverlap += overlapCoeff(m[2],m[3],m[1])
			sumTanimoto += tanimotoCoeff(m[2],m[3],m[1])
		aveOverlaps.append( sumOverlap  / len(mSet))
		aveTanimotos.append( sumTanimoto / len(mSet))


	return (numMatches, aveOverlaps, aveTanimotos)

def parseSet( folderName):
	fileNames = listdir(folderName)
	fileNames = [f for f in fileNames if f[0] != "."]
	info = [parseFile( folderName +"/"+f ) for f in fileNames]
	stats = [ [], [], []]
	for sub in info:
		if len(sub[0]) > 0:
			stats[0].append( 1.0 * sum(sub[0])/len(sub[0])) # numMatches
		else:
			stats[0].append(0)
		if len(sub[1]) > 0:
			stats[1].append( 1.0 * sum(sub[1])/len(sub[1])) # overlap coeff
		else:
			stats[1].append(0)
		if len(sub[2]) > 0:
			stats[2].append( 1.0 * sum(sub[2])/len(sub[2])) # tanimoto coeff
		else:
			stats[2].append(0)


	compressionData = []
	for fName in fileNames:
		compressionData.append( [float(el) for el in fName.split("_")[1::2]])

	plotReady = []
	cRatioToColor = {0.4:'b', 0.5:'g', 0.6:'r', 0.7:'c', 0.8:'m', 0.9:'y'}
	for i, (threshold, cBound, cRatio) in enumerate(compressionData):
		if threshold < 0.65:
			plottable = [stats[0][i], stats[1][i], stats[2][i], cBound, cRatioToColor[cRatio]]
			plotReady.append( plottable)

	byCol = { 'b':([],[],[],[]), 'g':([],[],[],[]), 'r':([],[],[],[]), 'c':([],[],[],[]), 'm':([],[],[],[]), 'y':([],[],[],[])}
	for (numMatches,oCoeff, tCoeff, cBound, color) in plotReady:
		byCol[color][0].append(numMatches)
		byCol[color][1].append(oCoeff)
		byCol[color][2].append(tCoeff)
		byCol[color][3].append(cBound)

	plt.figure("Coefficients")
	for color, sets in byCol.items():
		if color not in []:
			plt.plot(sets[3], sets[1], c=color, marker='o')
			plt.plot(sets[3], sets[2], c=color, marker='^')
	savefig("coeffs.png")

	plt.figure("Number of Results")
	for color, sets in byCol.items():
		if color not in []:
			plt.plot(sets[3], sets[0], c=color, marker='o')
	savefig("results.png")

	plt.show()


if __name__ == "__main__":
	args = sys.argv
	parseSet(args[1])

