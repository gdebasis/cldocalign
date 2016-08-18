# cldocalign
=========
A bilingual comparable document alignment tool

Project Homepage: https://github.com/gdebasis/cldocalign


GENERAL INFO
------------
cldocalign is a tool to align parallel texts (i.e. a source-language text and a target-language text) at document level. It does not require any translation of either of the source or the target language documents and therefore saves significant amount of time.

REQUIREMENTS
------------
i) Ant version at least 1.8.0 or higher

ii) JDK 1.8


USAGE INSTRUCTIONS
------------------
You need to download the required jars from the following link:

http://computing.dcu.ie/~plohar/cldocalign-libs.tar.gz

extract the compressed file and copy the extracted "lib" folder in the main project directory (i.e., "~/cldocalign-master" )

Set the paths of these jars in your "~/.bashrc" file

Extract the following files in the "euronews-data" directory to get the following resources:

i) Extract "dict.fr-en.txt.tar.gz" to get French-to-English dictionary ("dict.fr-en.txt")

ii) Extract "en.tar.gz" and "fr.tar.gz" to get a collection of English and French documents and

iii) Extract "fr-en.align.ref.tar.gz" to get the reference alignment from French to English ("fr-en.align.ref") as we consider French to be the source language and English to be the target language.


From within the main project directory, run "ant" to compile the code

The build should be successful (if not, please check all the path settings, for example, JAVA_HOME, CLASSPATH etc.)


To index the documents, go to the "script" directory

Set all the paths in "index.sh"

Run this script as follows:

chmod +x index.sh 

./index.sh <lang_id>  (in this case you will have to run this script twice with lang_id=en and then lang_id=fr)


The above instructions describes normal text indexing. In order to use word-vector indexing, open the file "wvec_index.sh" and set the paths

Run this script as follows:

chmod +x wvec_index.sh

./wvec_index.sh


It will create "en_index" and cluster_ids (in this case, numclusters=50, as the script will set this value in the "wvec_index.properties"; however, you may change the value if you want, for example numclusters=20 or other values in the "wvec_index.sh" file)


The next step is to align the documents. Go to the "script" directory

Set all the paths in "align.sh"

Run this script as follows:

chmod +x align.sh

./align.sh fr en  (it will align the each French document to the corresponding English document)

The final output will also be evaluated (check the log file to see the precision, recall and F-score)
The alignment process usually takes approximately 12 minutes in a Dell laptop with Intel core-i5 processor and 8-GB RAM. It takes much less time (around 5 minutes in the same machine) when the textSim variable is set to 1.0

***Note: in the "align.sh" script, you can try different combinations of values of the parameters such as "querysel.q_to_d_ratio", "querysel.lambda", "numtranslated_words" etc.
