import sys

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
			if(not inQuery and l[0] == "fine_threshold:"):
				fineThresh = l[1]
				coarseThresh = l[3]
			elif( not inQuery and l[0] == "START_QUERY"):
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

def collectRuntimeAves( results):
	methodTimes = {}
	for result in results:
		for methodName, method in result.methods.items():
			if( methodName not in methodTimes):
				methodTimes[methodName] = [method.time, 1]
			else:
				methodTimes[methodName][0] += method.time
				methodTimes[methodName][1] += 1
	averageTimes = {}
	for name, [totalTime, number] in methodTimes.items():
		aveTime = totalTime / number
		averageTimes[name] = aveTime
		print("{}, average runtime: {}".format(name, aveTime))

def collectAveNumResults( results):
	methodTimes = {}
	for result in results:
		for methodName, method in result.methods.items():
			if( methodName not in methodTimes):
				methodTimes[methodName] = [len(method.matches),1]
			else:
				methodTimes[methodName][0] += len(method.matches)
				methodTimes[methodName][1] += 1
	averageTimes = {}
	for name, [totalResults, number] in methodTimes.items():
		aveResults = float(totalResults) / number
		averageTimes[name] = aveResults
		print("{}, average number of results: {}".format(name, aveResults))



def main( searchFiles):
	results = []
	for filename in searchFiles:
		results += parseSearchTestResults( filename)
	collectRuntimeAves(results)
	collectAveNumResults( results)

if __name__ == "__main__":
	args = sys.argv
	main( args[1:])






