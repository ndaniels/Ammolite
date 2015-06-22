# Ammolite, Aligned Molecule Matching

Ammolite is a software tool for finding structurally similar molecules. Ammolite is especially good for searching large molecule databases, like pubchem, quickly. Among its potential uses Ammolite would help researchers with novel molecules to predict the properties of their molecule by finding known molecules with similar structures.

Ammolite matches pairs of molecules based on the amount their structures overlap. This measurement is quantified by the tanimoto coefficient. Two molecules with a tanimoto coefficient of 1 are identical while lower coefficients (down to 0) indicate increasingly different molecules. Typical searches with Ammolite look for molecules matching a query with tanimoto coefficient at least 0.7-0.9.

For efficient search Ammolite requires that a molecule database be properly compressed. Compression is quite a slow process so pre-compressed versions of the pubchem database are available for download at WEBSITE. 

For more information about the algorithm behind Ammolite see PAPER


## How to use the Ammolite command line tool

The command line version of Ammolite supports three basic commands: compress, search, and examine.

### Compress:

The compress command is used to compress a database of molecules.

The syntax is as follows

./Ammolite compress --source list/of/sdf/files/to/compress --target name/of/new/database

If an existing Ammolite database is given as the target the SDF files in source will be appended to the existing database.

Optionally a maximum number of threads can be specified using

--threads <number of threads>

To use JVM settings other than the default (Usually to increase the RAM available to the JVM) replace './Ammolite' with

java <java arguments> -jar path/to/Ammolite.jar compress <ammolite compress arguments>

#### A sample compress command:

java -Xmx360g -jar Ammolite.jar compress -s all/pubchem/sdf/*.sdf -t all-pubchem-ammolite-compression 

Compression of large databases is resource intensive and is only recommended on machines with a large amount of RAM and CPU. Compression of a ~50M molecule database took about 10 days on a machine with 20 processing cores and 360GB of RAM. 

### Search: 

The search command is used to search an Ammolite database or a set of SDF files.

Ammolite search outputs results as a CSV file with the following structure:

Query_ID, Target_ID, Size_of_Query, Size_of_Target, Size_of_Overlap, Tanimoto_Coefficient

Optionally Ammolite search can also write a series of SDF files detailing the structure of the overlaps between query and target molecules.

The test command requires only three arguments:

*  -d, --database  <arg>...   Path to the database. If using linear search this may
                             include multiple files and SDF files. Otherwise this must be a compressed Ammolite-database.
*  -q, --queries  <arg>...    SDF files of queries.
*  -t, --threshold  <arg>     The minimum tanimoto coefficient for search results

Optional arguments are:

*  -l, --linear-search        Search the database exhaustively using linear search.
*  -o, --out-file  <arg>      By default Ammolite will write its results to stdout. Instead a file may be specified.
*  -w, --write-sdfs           Make SDF files detailing the overlap between targets and queries.

#### The difference between linear-search and regular search:

By default Ammolite uses compressive acceleration to search only a subset of a large database. This results in signifigant speed gains but has a slight loss of accuracy and requires that databases be compressed before search. 

If search speed is not a concern (for small sets of molecules - up to 100k depending on the situation) or if accuracy is paramount linear-search may be preferable. 

linear-search does not require a special database format; target files may be given as SDF files. 

#### A sample search command:

./Ammolite search -d path/to/Ammolite-Database.adb -q path/to/queries.sdf -o ammolite-search-results.csv -t 0.9 

Arguments can be supplied directly to the jvm by replacing ./Ammolite with

java <java arguments> -jar Ammolite.jar search <ammolite test arguments>

Typically search requires much less RAM than compression but a search of the entire pubchem database can still benefit from up to 80GB of RAM. This can be supplied to the JVM with the argument 

java -Xmx80g <Ammolite>

### Examine:

The examine command is useful for listing some basic statistics about a database. It is relatively simple to use.

./Ammolite examine an-ammolite-database.gad

## How to use the Ammolite java API

Ammolite provides a small API for researchers who want to incorporate molecule search into their programs. Use of the Ammolite API requires a some familiarity with programming in java.

Ammolite is based off of the Chemistry Development Kit (CDK) and Small Molecule Subgraph Detection (SMSD). The CDK provides a huge amount of functionality for computational chemistry. A developer familiar with the CDK should have an easy time incorporating Ammolite into their programs. 

Ammolite provides three methods which may be useful for API use in the edu.mit.csail.ammolite.search package.

SearchHandler.handleSearch(arguments ...) which mirrors the command line usage

Ammolite.search(arguments ...) which allows for compressively-accelerated search from CDK objects stored in memory

SMSDSearcher.search(arguments ...) which allows for linear-search from CDK objects stored in memory.

To use either Ammolite.search() or SMSDSearcher.search developers will have to implement an instance of the IResultHandler interface. There are two examples of the result hanlder interface in the ammolite.search package. Implementation of a custom instance should be relatively easy.


## Development Notes:

Ammolite is a new piece of software and may have a few bugs. To report bugs or suggest changes feel free to contact us.

Users should also feel free to contact us for help using either the command line tool or the API.

You may contact us by email at:

dcdanko@mit.edu (preffered) or ndaniels@csail.mit.edu

