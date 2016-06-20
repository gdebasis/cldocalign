#!/bin/bash

#script to index

if [ $# -lt 1 ]
then
    echo "usage $0 <en/fr/de>"
    exit
fi

code=$1
basecolldir=/scratch/dganguly/docaligner/data/coll
index_base_dir=/scratch/dganguly/docaligner/index/
coll=$basecolldir/$code
scriptdir=/scratch/dganguly/docaligner/scripts
builddir=/scratch/dganguly/docaligner/build/classes/

if [ ! -e $index_base_dir/$code ]
then
	mkdir $index_base_dir/$code
fi

cat > index.$code.properties << EOF1
# Common PBS variables
#PBS -l nodes=1:ppn=4
#PBS -N index_$code
#PBS -A dcu01
#PBS -m eba
#PBS -r n
#PBS -l walltime=60:00:00

coll=$coll
index=$index_base_dir/$code/
EOF1

cd $builddir
java indexer.TextDocIndexer $scriptdir/index.$code.properties > $scriptdir/log.$code.txt 2>&1

