from sys import argv
import os


def splitSDF( inFilename, splitSize):

	with open( inFilename) as inF:
		numMols = 1
		chunkNum = 0
		location = inFilename.split("/")[:-1]
		location = "/".join(location)+"/"
		reducedFilename = inFilename.split("/")[-1]
		reducedFilename = reducedFilename.split(".")[0]
		foldername = location + reducedFilename + "_source_files/"

		if not os.path.exists(foldername):
			os.makedirs(foldername)

		outF = open(foldername + reducedFilename + "_" + str(chunkNum) + ".sdf", "w")
		for line in inF:
			if numMols <= splitSize:
				outF.write(line)
			else:
				chunkNum += 1
				numMols = 1
				outF.close()
				outF = open(foldername + reducedFilename + "_" + str(chunkNum) + ".sdf", "w")
				outF.write(line)

			if "$$$$" in line:
				numMols += 1

		outF.close()

def main(arg):
	splitSDF(arg[1], int(arg[2]))

if __name__ == '__main__':
	main(argv)