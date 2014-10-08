import sys
from glob import glob

class MethodResult:
	def __init__(self, method):
		self.method = method
		self.time = 0.0
		self.matches = []

class SearchResult:
	def __init__(self, query, fineThresh, coarseThresh):
		self.query = query
		self.fineThresh = fineThresh
		self.coarseThresh = coarseThresh
		self.methods = {}

	def addMethodResult(self, methodResult):
		self.methods[methodResult.method] = methodResult

def parseSearchTestResults( searchTestResults):

	allResults = []

	with open(searchTestResults) as f:
		inQuery = False
		inMethod = False
		for line in f:
			l = line.split()
			if len(l) > 0:
				if(not inQuery):
					if( l[0] == "fine_threshold:"):
						fineThresh = l[1]
						coarseThresh = l[3]
						print("~"*70)
						print("Fine threshold: {0} Coarse threshold; {1}".format(fineThresh, coarseThresh))
					elif( l[0] == "WALL_CLOCK"):
						print(line)

					elif(l[0] == "START_QUERY"):
						inQuery = True
						result = SearchResult(l[1], fineThresh, coarseThresh)

				elif( inQuery):
					if(l[0] == "END_QUERY"):
						inQuery = False
						allResults.append( result)
					elif("START_METHOD" == l[0]):
						inMethod = True
						methodResult = MethodResult(l[1])
					elif(inMethod):
						if("time" in l[0]):
							methodResult.time = float( l[1])

						elif("matches" in l[0]):
							methodResult.matches = l[1:]

						if(l[0] == "END_METHOD"):
							inMethod = False
							result.addMethodResult(methodResult)

	return allResults

def aggregateResultsByMethod( results):
	resultsByMethod = {}
	for result in results:
		for methodName, methodResult in result.methods.items():
			if( methodName not in resultsByMethod):
				resultsByMethod[ methodName] = [ methodResult,]
			else:
				resultsByMethod[ methodName].append( methodResult)
	return resultsByMethod

def arbAverage( results, getter, outname=None):
	averages = {}
	resultsByMethod = aggregateResultsByMethod( results)
	for methodName, methodResults in resultsByMethod.items():
		total, num = 0, 0
		for methodResult in methodResults:
			total += getter(methodResult)
			num += 1
		a = float(total) / num
		averages[ methodName] = a
		if( outname != None):
			print("{}, average {}: {:,}".format(methodName, outname, a))
	return averages


def arbDetail(results, getter):
	for r in results:
		name = r.query
		for mName, m in r.methods.items(): 
			item = getter(m)
			print("{} {}: {:,}".format(name, mName, item))

def getRuntime( methodResult):
	return methodResult.time

def getNumberOfResults( methodResult):
	return len(methodResult.matches)


def main( searchFiles):
	filenames = []
	for name in searchFiles:
		filenames += glob(name)

	for filename in filenames:
		results = parseSearchTestResults( filename)
		print("\nAverage runtimes:")
		arbAverage(results, getRuntime, "running time")
		print("\nAverage number of results:")
		arbAverage(results, getNumberOfResults, "number of results")
		print("\nDetailed runtimes:")
		arbDetail(results, getRuntime)
		print("\nDetailed number of results:")
		arbDetail(results, getNumberOfResults)

if __name__ == "__main__":
	args = sys.argv
	main( args[1:])






