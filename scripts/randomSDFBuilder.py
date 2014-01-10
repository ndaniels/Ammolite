from random import choice, random
from os import listdir


def buildRandomizedSDFs( numMolsList, inputFolder, outputFilenameList):
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

	for i in range( len( numMolsList)):
		numMols = numMolsList[i]
		outputFilename = outputFilenameList[i]

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
					outFile.write("$$$$")
					break
			f.close()

		outFile.close()
		print( "Finished "+outputFilename)

def main():
	sizes = [1000, 10*1000, 200*1000, 1000*1000]
	names = ["1k_random_molecules.sdf", "10k_random_molecules.sdf", "200k_random_molecules.sdf","1M_random_molecules.sdf"]
	buildRandomizedSDFs(sizes, "../pubchem/sdf", names)

if __name__ == "__main__":
	main()






