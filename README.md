# cldocalign

cldocalign
=========
A bilingual comparable document alignment tool

Project Homepage: https://github.com/gdebasis/cldocalign


GENERAL INFO
------------
cldocalign is a tool to align parallel texts (i.e. a source-language text and a target-language text) at document level. It does not require any translation of either of the source or the target language documents and therefore saves significant amount of time.


USAGE INSTRUCTIONS
------------------
You need to download following external jars:

commons-math3-3.6.1.jar (http://commons.apache.org/proper/commons-math/download_math.cgi)
deeplearning4j-examples-0.4-rc0-SNAPSHOT.jar (http://deeplearning4j.org/)
lucene-analyzers-common-5.3.1.jar (http://repo1.maven.org/maven2/org/apache/lucene/)
lucene-core-5.3.1.jar (http://repo1.maven.org/maven2/org/apache/lucene/) 
lucene-highlighter-5.3.1.jar (http://repo1.maven.org/maven2/org/apache/lucene/)
lucene-queries-5.3.1.jar (http://repo1.maven.org/maven2/org/apache/lucene/)


Create a directory called "lib" inside the main project directory (i.e., "~/cldocalign-master" ) and copy the above jars here.

Set the paths of these jars in your "bashrc" file

From within the main project directory, run "ant" to compile the code

The build should be successful (if not, please check all the path settings, for example, JAVA_HOME, CLASSPATH etc.)

Extract the files stored in the folder "~/euronews-data"

You will find the English and French documents stored in "en" and "fr" folders respectively.

The French-to-English dictionary ("dict.fr-en.txt") is also provided with their translation probablities

create the directories "index" and "results" to store the index and outputs

go to the "script" directory

set all the paths in "index.sh"

run this script as follows:

chmod +x index.sh 

./index.sh <lang_id>    (in this case you will have to run this script twice with lang_id=en and then lang_id=fr)

set all the paths in "align.sh"

run this script as follows:

./align.sh <source> <target> (in this case, we consider "fr" to be the source and "en" to be the target, if you want to change the direction, you need to change the direction of alignments in the reference set, i.e., in "~/cldocalign-master/euronews-data/dict.fr-en.txt")

