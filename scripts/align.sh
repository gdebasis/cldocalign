#!/bin/bash

#script to index

if [ $# -lt 2 ]
then
    echo "usage $0 src(<en/fr/de>) tgt(<en/fr/de>)"
    exit
fi

src=$1
tgt=$2
datadir=/scratch/dganguly/docaligner/data/
basecolldir=$datadir/coll
index_base_dir=/scratch/dganguly/docaligner/index/
scriptdir=/scratch/dganguly/docaligner/scripts
builddir=/scratch/dganguly/docaligner/build/classes/
resultsdir=/scratch/dganguly/docaligner/results/

for (( nterms=10; nterms<=50; nterms+=10 ))
do

cat > align.$src.$tgt.$nterms.sh << EOF1
# Common PBS variables
#PBS -l nodes=1:ppn=4
#PBS -N align_$src.$tgt.$nterms
#PBS -A dcu01
#PBS -m eba
#PBS -r n
#PBS -l walltime=60:00:00

cd $scriptdir
cp /dev/null  align.init.$src.$tgt.$nterms.properties
echo "index=$index_base_dir/$src/" >> align.init.$src.$tgt.$nterms.properties
echo "translated.index=$index_base_dir/$tgt/" >> align.init.$src.$tgt.$nterms.properties
echo "dict=$datadir/dict/$src.$tgt.dict" >> align.init.$src.$tgt.$nterms.properties

echo "out.align.file=$resultsdir/$src.$tgt.$nterms.align.txt" >> align.init.$src.$tgt.$nterms.properties
echo "ref.align.file=$datadir/refalignments/$src.$tgt.align.ref" >> align.init.$src.$tgt.$nterms.properties

echo "querysel.ntopterms=$nterms" >> align.init.$src.$tgt.$nterms.properties
echo "querysel.lambda=0.1" >> align.init.$src.$tgt.$nterms.properties
echo "qry.translation=true" >> align.init.$src.$tgt.$nterms.properties
echo "numtranslated_words=3" >> align.init.$src.$tgt.$nterms.properties

echo "retrieve.temporal_search_window=0" >> align.init.$src.$tgt.$nterms.properties
echo "retrieve.max_per_day=50" >> align.init.$src.$tgt.$nterms.properties

cd $builddir
java retriever.CrossLingualAligner $scriptdir/align.init.$src.$tgt.$nterms.properties > $scriptdir/log.align.$src.$tgt.$nterms.txt 2>&1

EOF1


chmod +x align.$src.$tgt.$nterms.sh 
qsub align.$src.$tgt.$nterms.sh 

done
