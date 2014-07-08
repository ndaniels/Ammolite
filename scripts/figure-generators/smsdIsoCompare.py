import matplotlib.pyplot as plt
from pylab import polyfit, poly1d, show, savefig
import sys

def isNumber( s):
	try:
		float(s)
		return True
	except ValueError:
		return False

def makeGraph(X,Y, xName, yName, name="NoName"):
	fig = plt.figure()
	ax = fig.add_subplot(111)
	superName = "Comparison of {} and {}".format(xName,yName)
	outname = "{} from {}.png".format(superName,name)
	fig.suptitle(superName)
	ax.scatter(X,Y)

	fit = polyfit(X,Y,1)
	fit_fn = poly1d(fit) # fit_fn is now a function which takes in x and returns an estimate for y
	ax.plot(X,Y, 'yo', X, fit_fn(X), '--k')

	ax.plot(range(40))
	ax.set_xlabel('Size of MCS found by {}'.format(xName))
	ax.set_ylabel('Size of MCS found by {}'.format(yName))
	ax.text(1, 1, "y = {}*x + {}".format(fit[0], fit[1]))
	fig.savefig(outname)


def buildIsoSMSDComparison( filename, outname="SMSD-IsoRank-comparison"):
	X, Y, xName, yName = [], [], "", ""
	with open( filename) as f:
		inComparison = False
		nameLine = False
		for line in f:
			if line.split()[0] == "COMPARISON_DELIMITER":
				if inComparison:
					makeGraph( X, Y, xName, yName, filename)
				inComparison = True
				nameLine = True
				X, Y = [], []
			elif inComparison:
				l = line.split()
				if nameLine:
					xName, yName = l[0], l[1]
					nameLine = False
				else:
					X.append( float( l[0]))
					Y.append( float( l[1]))

	makeGraph( X, Y, xName, yName, filename)







if __name__ == "__main__":
	args = sys.argv
	if(len(args) == 2):
		buildIsoSMSDComparison(args[1])
	else:
		buildIsoSMSDComparison(args[1], args[2])
