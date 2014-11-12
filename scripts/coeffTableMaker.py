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

						elif("START_DETAILED_MATCHES" in l[0]):
							inDetMatches = True

						elif("START_DETAILED_MISSES" in l[0]):
							inDetMisses = True

						elif(inDetMatches):
							if("END_DETAILED_MATCHES" in l[0]):
								inDetMatches = False
							else:
								idStr = l[0]
								matchSize = l[1]
								methodResult.detailedMatches[idStr] = matchSize

						elif(inDetMisses):
							if("END_DETAILED_MISSES" in l[0]):
								inDetMisses = False
							else:
								idStr = l[0]
								matchSize = l[1]
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
			pubIds.trim()
			pubIds = pubIds[1:-1]
			pubIds = pubIds.split(",")
			pubIds = [anId.trim() for anId in pubIds]

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
			l = line.trim().split(":")
			idStr = l[0].trim()
			size = int( l[1])
			table[idStr] = size
	return table

def hitsOverThresh(queryId, smsd, sizeTable, thresh):
	hits = []
	querySize = sizeTable[queryId]
	for result in smsd:
		resultSize = sizeTable[resultSize]
		for (idStr, matchSize) in result.detailedMatches:
			coeff = overlapCoeff(matchSize, resultSize, querySize)

			if coeff >= threshold:
				hits.append( (idStr, overlapCoeff))

		for (idStr, matchSize) in result.detailedMisses:
			coeff = overlapCoeff(matchSize, resultSize, querySize)

			if coeff >= threshold:
				hits.append( (idStr, overlapCoeff))

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

	for result in ammCoarse:
		for (idStr, matchSize) in result.detailedMatches:
			hits[idStr] = matchSize

		for (idStr, matchSize) in result.detailedMisses:
			hits[idStr] = matchSize

	return hits

def structIdsWithCoarseOverlapNumMols(structQueryId, structOverlapTable, structSizeTable, structIds):
	matches = []
	structQuerySize = structSizeTable[structQueryId]
	for (structId, numHits) in structIds.items():
		structTargetSize = structSizeTable[structID]
		coeff = overlapCoeff( structOverlapTable[structId], structQuerySize, structTargetSize)
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
		if thresh = thresholds[0]:
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

			maxAllowedMisses = totalNumOfMolsRepped * ( 1 - fracOfResults)
			missesSoFar = 0
			for i in range(len(structsSortedByCoeff)):
				nextMisses = missesSoFar + structsSortedByCoeff[i][1]
				if nextMisses < maxAllowedMisses:
					missesSoFar = nextMisses
				else:
					threshTableRow[fracOfResults] = structsSortedByCoeff[i][2]

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
			ammQueries.append(sR.query)

		assert len(ammQueries) == len(smsdQueries)

		for i in range( len(smsdQueries)):

			sQuery = smsdQueries[i]
			aQuery = ammQueries[i]
			relSMSDMethodResults = allSMSDSearchResults[i].methods.values()[0]
			relAmmMethodResults = coarseAmmSearchResults[i].methods.values()[0]
			valTable = oneQuery( sizeTable, structSizeTable, idTable, sQuery, aQuery, relSMSDMethodResults, relAmmMethodResults)
			printValTable( valTable)


def printValTable( table):
	colKey = table['column_key']
	print(colKey)
	rowKey = table['row_key']
	rowStrTemplate = "{0:2d} "
	for i, entry in enumerate(colKey):
		inBracketStr = '{}:{}d'.format(i+1,3*i+5)
		rowStrTemplate += " {" + inBracketStr +"} "

	for rowName in rowKey:
		row = table[rowName] # row is a dictionary with the elements of colKey as keys
		fmtRow = rowStrTemplate.format( rowName, *sorted(row.values()))
		print(fmtRow)

	print()









def overlapCoeff( overlap, a, b):
	if a < b:
		return (1.0*overlap) / a 
	return (1.0*overlap) / b


if __name__ == "__main__":
	args = sys.argv
	main( args[1:])





