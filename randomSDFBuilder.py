from random import choice, random
from os import listdir


def buildRandomizedSDF( numMols, inputFolder, outputFilename):
	filenames = [filename for filename in listdir(inputFolder) if filename[-3:] == "sdf"]
	
	
	fileOffsets = [] 
	for filename in filenames:
		f = open(inputFolder +"/"+filename,'r')
		print "findings offsets in " + filename
		molOffsets = []
		offset = 0
		setOff = 0
		for line in f:
			offset += len(line)

			if "$$$$" in line:
				molOffsets.append(setOff)
				setOff = offset
			

		fileOffsets.append(molOffsets)
		f.close()
	
	molsToGrab = []

	while len(molsToGrab) < numMols:
		fromFile = choice(fileOffsets)
		fromFileInd = fileOffsets.index(fromFile)
		at = choice(fromFile)
		molsToGrab.append( (fromFileInd, at))

	outFile = open(outputFilename,"w")

	for fileInd, offset in molsToGrab:
		filename = filenames[fileInd]
		f = open(inputFolder +"/"+filename,'r')
		f.seek(offset)
		firstline = True
		for line in f:
			
			if "$$$$" not in line:
				outFile.write(line)
			else:
				break
		f.close()

	outFile.close()

def main():
	buildRandomizedSDF(1000,"../pubchem/sdf", "randomOut_1000")

if __name__ == "__main__":
	main()






