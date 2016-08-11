#!/bin/bash

start=`date +%s`

#script to index

if [ $# -lt 1 ]
then
    echo "usage $0 <en/fr/de>"
    exit
fi

code=$1
basedir=/home/pintu/gitupload/cldocalign-master
scriptdir=$basedir/scripts
builddir=$basedir/build/classes
basecolldir=$basedir/data/euronews
coll=$basecolldir/$code
index_base_dir=$basedir/index


if [ ! -e $index_base_dir ]
then
	mkdir $index_base_dir
fi


if [ ! -e $index_base_dir/$code ]
then
	mkdir $index_base_dir/$code
fi



cat > $scriptdir/index.$code.properties << EOF1

coll=$coll
index=$index_base_dir/$code/

EOF1

cd $builddir

java indexer.TextDocIndexer $scriptdir/index.$code.properties #> $scriptdir/log.$domainname.$code.txt 2>&1


end=`date +%s`

runtime=$((end-start))

echo "run time is= $runtime seconds"
