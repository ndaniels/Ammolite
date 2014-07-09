from subprocess import call
import sys

def doSearch(database, queries, coarse, fine, logname):
	command = "./Ammolite test-search -d {} -q {} -t {} -p {} > {}"
	command = command.format(database,queries, fine, coarse, logname)
	call(command,shell=True)

def main(inName, day):
	if inName[-4:] in [".adb", ".sdf"] :
		inName = inName[:-4]

	database = inName + ".adb"
	queries = inName + ".sdf"
	basicName = "Search_"+day+"_{}_{}.test"

	for fine in [0.7, 0.8, 0.9]:
		for coarse in [0.1, 0.2, 0.3, 0.4, 0.5, 0.6]:
			name = basicName.format(fine,coarse)
			doSearch(database, queries, coarse, fine, name)



if __name__ == "__main__":
	args = sys.argv
	main(args[1], args[2])


