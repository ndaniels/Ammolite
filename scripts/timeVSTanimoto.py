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
		if grabbingData and 'clusters' in line:
			break
		if grabbingData:
			l = line.split()
			t = int( l[2])
			c = float(l[4])
			if t < 3000 and c > 0.5:
				time.append( t)
				coeff.append(c)
		if 'Time breakdown' in line:
			grabbingData = True
		
	return (time,coeff)

def makeGraphs( (time, coeff)):
	plt.figure("time vs coeff")
	plt.scatter( time, coeff)
	show()
		
def main():
	makeGraphs( parse(argv[1]))

if __name__ == '__main__':
	main()