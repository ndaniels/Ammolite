import matplotlib.pyplot as plt
from pylab import polyfit, show


def parse( filename):

	f = open( filename)
	lineNum = 0;
	molOverlap = []
	repOverlap = []
	molTanimoto = []
	repTanimoto = []

	rawMolOverlap = []
	rawRepOverlap = []
	rawMolTanimoto = []
	rawRepTanimoto = []

	for line in f:
		if lineNum >= 2:
			l = line.split()
			if( lineNum < 20 ):
				print( l)
			if lineNum % 2 == 0:
				rawMolOverlap.append( float(l[6]))
				rawMolTanimoto.append( float(l[7]))
			if lineNum % 2 == 1:
				rawRepOverlap.append( float(l[6]))
				rawRepTanimoto.append( float(l[7]))
		lineNum += 1

	for i, val in enumerate( rawMolOverlap):
		if val > .6:
			molOverlap.append(val)
			repOverlap.append( rawRepOverlap[i])

	for i, val in enumerate( rawMolTanimoto):
		if val > .2:
			molTanimoto.append(val)
			repTanimoto.append( rawRepTanimoto[i])


	assert len(molOverlap) == len(repOverlap)

	print("{} pairs of molecules".format(len(molOverlap)))

	return ( molOverlap, repOverlap, molTanimoto, repTanimoto)

def makeGraphs( (molOverlap, repOverlap, molTanimoto, repTanimoto)):
	plt.figure("Overlap")
	plt.scatter(molOverlap,repOverlap)
	show()
	plt.figure("Tanimoto")
	plt.scatter(molTanimoto,repTanimoto)
	show()

def main():
	pOut = parse( "molecules_overlap_cyclic.test")
	oM, oB = polyfit(pOut[0], pOut[1], 1)
	tM, tB = polyfit(pOut[2], pOut[3], 1)
	print("rep_overlap = {} * overlap + {}".format(oM, oB))
	print("rep_tanimoto = {} * tanimoto + {}".format(tM, tB))
	oC = polyfit(pOut[0], pOut[1], 0)
	tC = polyfit(pOut[2], pOut[3], 0)
	print("rep_overlap = {} * overlap".format(oM, oB))
	print("rep_tanimoto = {} * tanimoto + {}".format(tM, tB))
	makeGraphs( pOut)

if __name__ == '__main__':
	main()
