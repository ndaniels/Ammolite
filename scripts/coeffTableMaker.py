import sys
from glob import glob

class MethodResult:
	def __init__(self, method):
		self.method = method
		self.time = 0.0
		self.matches = []
		self.detailedMatches = {}
		self.detailedMisses = {}

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
		inDetMatches = False
		inDetMisses = False
		for line in f:
			l = line.split()
			if len(l) > 0:
				if(not inQuery):
					if( l[0] == "fine_threshold:"):
						fineThresh = l[1]
						coarseThresh = l[3]
						
					elif( l[0] == "WALL_CLOCK"):
						pass

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

						elif("START_DETAILED_MATCHES" in l[0]):
							inDetMatches = True

						elif("START_DETAILED_MISSES" in l[0]):
							inDetMisses = True

						elif(inDetMatches):
							if("END_DETAILED_MATCHES" in l[0]):
								inDetMatches = False
							else:
								idStr = l[0]
								matchSize = int(l[1])
								methodResult.detailedMatches[idStr] = matchSize

						elif(inDetMisses):
							if("END_DETAILED_MISSES" in l[0]):
								inDetMisses = False
							else:
								idStr = l[0]
								matchSize = int(l[1])
								methodResult.detailedMisses[idStr] = matchSize



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

def parseIDTable(tableName):
	table = {}
	with open(tableName) as t:
		for line in t:
			t = line.split(":")
			structId = t[0]
			pubIds = t[1]
			pubIds = pubIds.strip()
			pubIds = pubIds[1:-1]
			pubIds = pubIds.strip()
			pubIds = pubIds.split(",")
			pubIds = [anId.strip() for anId in pubIds if len(anId) > 0]


			table[structId] = pubIds
	return table

def flipTable( table):
	flippedTable = {}
	for structID, pubIdList in table.items():
		for pubId in pubIdList:
			flippedTable[pubId] = structID
	return flippedTable

def parseSizeTable( tableName):
	table = {}
	with open(tableName) as t:
		for line in t:
			l = line.strip().split(":")
			idStr = str(l[0].strip())
			size = int( l[1])
			table[idStr] = size
	return table

def hitsOverThresh(queryId, smsd, sizeTable, threshold):
	hits = []
	querySize = sizeTable[queryId]
	result = smsd

	for (idStr, matchSize) in result.detailedMatches.items():

		coeff = overlapCoeff(matchSize, sizeTable[idStr], querySize)

		if coeff >= threshold:
			hits.append( (idStr, coeff))

	for (idStr, matchSize) in result.detailedMisses.items():
		coeff = overlapCoeff(matchSize, sizeTable[idStr], querySize)

		if coeff >= threshold:
			hits.append( (idStr, coeff))

	return hits

def hitsOverThreshSecond( hits, thresh):
	newHits = []
	for (idStr, overlapCoeff) in hits:
		if overlapCoeff >= thresh:
			newHits.append((idStr, overlapCoeff))
	return newHits

def structIdsOfHits( hits, idTable):
	structIds = {}
	for (pubId, overlapCoeff) in hits:
		structId = idTable[pubId]
		if not structId in structIds:
			structIds[structId] = 1
		else:
			structIds[structId] += 1

	return structIds

def structOverlapTable(ammCoarse):
	hits = {}

	result = ammCoarse
	for (idStr, matchSize) in result.detailedMatches.items():
		hits[idStr.strip()] = matchSize

	for (idStr, matchSize) in result.detailedMisses.items():
		hits[idStr.strip()] = matchSize

	return hits

def structIdsWithCoarseOverlapNumMols(structQueryId, structOverlapTable, structSizeTable, structIds):
	matches = []
	structQuerySize = structSizeTable[structQueryId]
	for (structId, numHits) in structIds.items():
		structTargetSize = structSizeTable[structId.strip()]
		coeff = overlapCoeff( structOverlapTable[structId.strip()], structQuerySize, structTargetSize)
		matches.append((structId, numHits, coeff))
	sortedMatches = sorted(matches, key=lambda match: match[2])
	return sortedMatches

def oneQuery( sizeTable, structSizeTable, idTable, queryId, structQueryId, smsd, ammCoarse):
	thresholds = sorted( [0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0])
	resultFracs = [0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9]
	allHits  = hitsOverThresh( queryId, smsd, sizeTable, thresholds[0])

	threshTable = {}
	threshTable['column_key'] = [rFrac for rFrac in resultFracs + [1.0] ]
	threshTable['row_key'] = [thresh for thresh in thresholds ]
	for thresh in thresholds:


		if thresh == thresholds[0]:
			relevantHits = allHits
		else:
			relevantHits = hitsOverThreshSecond( relevantHits, thresh)


		structIdsOfRelevantHits = structIdsOfHits( relevantHits, idTable)
		overlapInAmmCoarse = structOverlapTable( ammCoarse)
		structsSortedByCoeff = structIdsWithCoarseOverlapNumMols(structQueryId, overlapInAmmCoarse, structSizeTable, structIdsOfRelevantHits)

		threshTableRow = {}
		threshTableRow[1.0] = structsSortedByCoeff[0][2]

		totalNumOfMolsRepped = sum([el[1] for el in structsSortedByCoeff])
		for fracOfResults in resultFracs:
			# get at least 'fracOfResults' results

			minMatchesNeeded = totalNumOfMolsRepped * fracOfResults

			matchesSoFar = 0
			for i in range(len(structsSortedByCoeff)):
				nextMatches = matchesSoFar + structsSortedByCoeff[i][1]
				if nextMatches < minMatchesNeeded:
					matchesSoFar = nextMatches
				else:

					threshTableRow[fracOfResults] = structsSortedByCoeff[i][2]
					break;

		threshTable[thresh] = threshTableRow

	return threshTable

def main(args):
	if len(args) != 5:
		print("usage: [structSizeTable] [sizeTable] [smsdResults] [ammCoarseResults] [idTable]")

	else:
		sizeTable = parseSizeTable( args[1])
		structSizeTable = parseSizeTable( args[0])
		revIdTable = parseIDTable(args[4])
		idTable = flipTable( revIdTable)

		allSMSDSearchResults = parseSearchTestResults( args[2])
		smsdQueries = []
		for sR in allSMSDSearchResults:
			smsdQueries.append( sR.query)

		coarseAmmSearchResults = parseSearchTestResults(args[3])[::2]
		ammQueries = []
		for sR in coarseAmmSearchResults:
			ammQueries.append(sR.query +"_STRUCT")

		assert len(ammQueries) == len(smsdQueries)



		for i in range( len(smsdQueries)):

			sQuery = smsdQueries[i]
			aQuery = ammQueries[i]
			print( "{} {}".format(sQuery, aQuery))
			relSMSDMethodResults = allSMSDSearchResults[i].methods.values()[0]
			print(allSMSDSearchResults[i].query)
			relAmmMethodResults = coarseAmmSearchResults[i].methods.values()[0]
			valTable = oneQuery( sizeTable, structSizeTable, idTable, sQuery, aQuery, relSMSDMethodResults, relAmmMethodResults)
			printValTable( valTable)


def printValTable( table):
	
	colKey = table['column_key']
	print("Fraction of Results")

	rowKey = table['row_key']
	rowStrTemplate = "{0:2} | "
	for i, entry in enumerate(colKey):
		inBracketStr = '{}:{}f'.format(i+1,i+3)
		rowStrTemplate += " {" + inBracketStr +"} "

	fmtRow = rowStrTemplate.format( "key", *colKey)
	print(fmtRow)
	print("-"*115)

	for rowName in rowKey:
		row = table[rowName] # row is a dictionary with the elements of colKey as keys
		fmtRow = rowStrTemplate.format( rowName, *sorted(row.values()))
		print(fmtRow)

	print("\n")









def overlapCoeff( overlap, a, b):
	if a < 1 or b < 1:
		return 1.0
	if a < b:
		return (1.0*overlap) / a 
	return (1.0*overlap) / b


if __name__ == "__main__":
	args = sys.argv
	main( args[1:])





