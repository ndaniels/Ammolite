import sys

def fix( filename):

	f = file(filename, 'r')
	fixed = file(filename+'.fixed', 'w')
	for line in f:
		if "$$$$" in line:
			end = line.split("$$$$")[1]
			fixed.write("$$$$\n"+end)
		else:
			fixed.write(line)
	f.close()
	fixed.close()

if __name__ == "__main__":
	for filename in sys.argv[1:]:
		fix( filename)




