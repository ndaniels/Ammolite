# How to use Ammolite

The current version of Ammolite supports three basic commands useful for testing.

These commands are: compress, test, and examine.

## Compress:

The compress command is used to compress a database of molecules.

The syntax is as follows

./Ammolite compress --source list/of/sdf/files/to/compress --target name/of/new/database

Short options may also be used.

Optionally a number of threads can be specified using

--threads 12

To use JVM settings other than the default (Usually to increase the RAM available to the JVM) replace './Ammolite' with

java <java arguments> -jar path/to/Ammolite.jar compress <ammolite compress arguments>

### A sample compress command:

java -Xmx360g -jar Ammolite.jar compress -s all/pubchem/sdf/*.sdf -t all-pubchem-ammolite-compression 

Compression of large databases is resource intensive and is only recommended on machines with a large amount of RAM and CPU. Compression of a ~50M molecule database took about 10 days on a machine with 20 processing cores and 360GB of RAM. 

Several sample databases are available for download.

## Test:

The test command is used to search an Ammolite database.

The test command requires a number of arguments:

* -d, --database  A path to the database to be searched. Ammolite databases uses .gad as an extension
* -q, --queries  A list of sdf files to search against the database
* -o, --out  A location and file name for where to write the search results
* --description  A brief description of the test being conducted. This may be ommitted
* -t, --threshold  A minimum tanimoto coefficient for search matches
* -c, --coarse-threshold  A minimum tanimoto coefficient for coarse search matches. Usually 0.2 less than the value of threshold
* -A, --Ammolite  Tell the program to run an ammolite search
* -S, --SMSD  Tell the program to run an SMSD search

### A sample test command:

./Ammolite test -d path/to/Ammolite-Database.gad -q path/to/queries.sdf -o path/to/output/file -t 0.8 -c 0.6 -A

Arguments can be supplied to the jvm by replacing ./Ammolite with

java <java arguments> -jar Ammolite.jar test <ammolite test arguments>

Typically search requires much less RAM than compression but a search of the entire pubchem database can still require up to 80GB of RAM. This can be supplied to the JVM with the argument 

java -Xmx80g <Ammolite>

## Examine:

The examine command is useful for listing some basic statistics about a database. It is relatively simple to use.

./Ammolite examine an-ammolite-database.gad


## Development Notes:

The first release version of Ammolite will have two new major features.

It will support appending new files to an already compressed database.

It will have a dedicated search commmand with a simpler interface and different output. 

We want to integrate Ammolite into as many workflows as possible and would greatly appreciate suggestions about how we could format search results. If you have other suggestions for features please let us know as well. 

You may contact us by email at

dcdanko@mit.edu (preffered) or ndaniels@csail.mit.edu

