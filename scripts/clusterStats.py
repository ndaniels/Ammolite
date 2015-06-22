import yaml
from sys import argv

def mean( aDict):
	summed = 0.0
	els = 0
	for key, val in aDict.items():
		els += 1
		summed += len(val)
	return summed / els

def stdDev(aDict, mu):
	squaredDiffs = 0.0
	els = 0
	for key, val in aDict.items():
		els += 1
		root = len(val) - mu
		squaredDiffs += root * root

	return squaredDiffs / els


def main(args):
	filename = args[0]
	with open(filename, 'r') as stream:
		clusterDict = yaml.load(stream)
		mu = mean( clusterDict)
		sD = stdDev( clusterDict, mu)
		print("Mean: {}".format(mu))
		print("Standard Dev: {}".format(sD))



if __name__ == '__main__':
	main(argv[1::])