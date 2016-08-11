#!/bin/bash


start=`date +%s`

#script to align

if [ $# -lt 2 ]
then
    echo "usage $0 src(<en/fr>) tgt(<en/fr>)"
    exit
fi




src=$1
tgt=$2
basedir=/home/pintu/gitupload/cldocalign-master
datadir=$basedir/data
index_base_dir=$basedir/index
scriptdir=$basedir/scripts
builddir=$basedir/build/classes
resultsdir=$basedir/results



cd $scriptdir


cat > align.$src.$tgt.sh << EOF1

cp /dev/null  align.init.$src.$tgt.properties
echo "index=$index_base_dir/$src/" >> align.init.$src.$tgt.properties
echo "translated.index=$index_base_dir/$tgt/" >> align.init.$src.$tgt.properties
echo "dict=$datadir/dict/$src.$tgt.dict" >> align.init.$src.$tgt.properties

echo "out.align.file=$resultsdir/$src.$tgt.align.txt" >> align.init.$src.$tgt.properties
echo "ref.align.file=$datadir/refalignments/$src.$tgt.align.ref" >> align.init.$src.$tgt.properties

echo "querysel.q_to_d_ratio=0.6" >> align.init.$src.$tgt.properties

echo "querysel.lambda=0.9" >> align.init.$src.$tgt.properties
echo "qry.translation=true" >> align.init.$src.$tgt.properties
echo "numtranslated_words=7" >> align.init.$src.$tgt.properties

echo "retrieve.temporal_search_window=10" >> align.init.$src.$tgt.properties
echo "retrieve.max_per_day=50" >> align.init.$src.$tgt.properties
echo "retrieve.temporal_constraint=false" >> align.init.$src.$tgt.properties


echo "wvecs.txt=$basedir/envec.pp.txt" >> align.init.$src.$tgt.properties
echo "wvecs.index=$basedir/wvecs/en-index" >> align.init.$src.$tgt.properties
echo "wvecs.clusterids.basedir=$basedir/wvecs/cluster-ids" >> align.init.$src.$tgt.properties

#if you do not want ot use word-vector approach, simply set the following value to 1.0, which means it will use only text similarity and will be faster
echo "simscore.textsim=1.0" >> align.init.$src.$tgt.properties 

echo "retrieve.vocabcluster.numclusters=50" >> align.init.$src.$tgt.properties

cd $builddir
java retriever.CrossLingualAligner $scriptdir/align.init.$src.$tgt.properties > $scriptdir/log.align.$src.$tgt.txt 2>&1

EOF1


chmod +x align.$src.$tgt.sh

echo "Aligning the documents...."
./align.$src.$tgt.sh

end=`date +%s`

runtime=$((end-start))

echo "Alignment done in $runtime seconds"
