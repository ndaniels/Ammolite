

def testFastSearch( querySet, database):

	command = "java -jar build/jar/MolSearch.jar -v -1 search -d {} -q {} --sdf {}"
	sdfName = querySet + "_searching_" + database
	command.format(querySet, database, sdfName)
	