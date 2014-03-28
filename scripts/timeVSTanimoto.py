import matplotlib.pyplot as plt
from pylab import polyfit, show
from sys import argv

def parse( filename):
	f = open(filename)
	lineNum = 0
	time = []
	coeff = []
	grabbingData = False
	for line in f:
		if grabbingData:
			l = line.split()
			time.append( int( l[2]))
			coeff.append(float(l[4]))
		if 'Time breakdown' in line:
			grabbingData = True
		if 'clusters' in line:
			break
	return (time,coeff)

def makeGraphs( (time, coeff)):
	plt.figure("time vs coeff")
	plt.scatter( time, coeff)
	show()
		
def main():
	makeGraphs( parse(argv[0]))

if __name__ == '__main__':
	main()