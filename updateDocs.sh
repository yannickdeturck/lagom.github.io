#!/bin/bash

if [ $# -lt 2 ]
then
    echo "Usage: $0 <path/to/lagom/checkout> <docsversion>"
    echo
    echo "For example:"
    echo "$0 ../lagom 1.2.x"
    exit 1
fi

WEBSITE=`pwd`
LAGOM=`readlink -f $1`
VERSION=$2

cd $LAGOM
sbt unidoc
cd docs
sbt markdownStageSite

cd $WEBSITE
rm -rf src/docs/$VERSION
mkdir -p src/docs/$VERSION
cp -r $LAGOM/docs/target/markdown-site/* src/docs/$VERSION

