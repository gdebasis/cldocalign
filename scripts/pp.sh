#!/bin/bash

if [ $# -lt 1 ]
then
        echo "usage $0 <file>"
        exit
fi

filename=$1

cat $filename | awk '{if (index($2,"-")==0 && index($2,".")==0) print $0}' > $filename.pp

