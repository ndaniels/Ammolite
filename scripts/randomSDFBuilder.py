from random import choice, random
from os import listdir
from sys import argv


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
		grabbed = {}

		while len(molsToGrab) < numMols:
			fromFile = choice(fileOffsets)
			fromFileInd = fileOffsets.index(fromFile)
			at = choice(fromFile)
			if fromFileInd not in grabbed:
				molsToGrab.append( (fromFileInd, at))
				grabbed[fromFileInd] = {at:True}
			elif at not in grabbed[fromFileInd]:
				molsToGrab.append( (fromFileInd, at))
				grabbed[fromFileInd][at] = True
			# print("grabbed!")
		print("Finished grabbing mols")

		maxNumMolsInFile = 20*1000
		numMolsInFile = 0
		fileNum = 0

		for fileInd, offset in molsToGrab:

			if( numMolsInFile == 0):
				outFilename = outputFilename + "_{}".format(fileNum)
				outFile = open(outFilename,"w")
			
			if( numMolsInFile < maxNumMolsInFile):

				filename = filenames[fileInd]
				f = open(inputFolder +"/"+filename,'r')
				f.seek(offset)
				firstline = True
				for line in f:
					
					if "$$$$" not in line:
						outFile.write(line)
					else:
						outFile.write("$$$$\n")
						break
				f.close()
				numMolsInFile += 1

			else:
				outFile.close()
				fileNum += 1
				numMolsInFile = 0
				print( "Finished "+outFilename)

		outFile.close()

		print( "Finished "+outputFilename)

def main(args):
	if( len(args) == 1):
		print("usage: [source_files] [names...] [sizes...]")
	elif (len(args)  %2 == 0):
		print(args)
		inputFolder = args[1]
		print(inputFolder)
		numFilesToMake = (len(args) - 2) / 2
		sizes =  [int(val) for val in args[(2+numFilesToMake):]]
		print(sizes)
		names = args[2:(2+numFilesToMake)]
		print(names)
		buildRandomizedSDFs(sizes, inputFolder, names)


if __name__ == "__main__":
	main(argv)






