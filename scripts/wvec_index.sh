#!/bin/bash

start=`date +%s`

#script for wordVec indexing

basedir=/home/pintu/gitupload/cldocalign-master


scriptdir=$basedir/scripts
builddir=$basedir/build/classes

wvec_dir=$basedir/wvecs
wvecs_en_index=$basedir/wvecs/en_index
wvecs_clusterids_basedir=$basedir/wvecs/cluster_ids



if [ ! -e $wvec_dir ]
then
	mkdir $wvec_dir
fi


if [ ! -e $wvecs_clusterids_basedir ]
then
	mkdir $wvecs_clusterids_basedir
fi


cat > $scriptdir/wvec_index.properties << EOF1

wvecs.txt=$basedir/en.vec.pp.txt
wvecs.index=$wvecs_en_index
wvecs.clusterids.basedir=$wvecs_clusterids_basedir
retrieve.vocabcluster.numclusters=50

EOF1

cd $scriptdir

chmod +x wvec_index.properties

cd $builddir

java wvec.WordVecsIndexer $scriptdir/wvec_index.properties #> $scriptdir/log.$domainname.txt 2>&1


end=`date +%s`

runtime=$((end-start))

echo "Time taken for wordVec indexing is $runtime seconds"


