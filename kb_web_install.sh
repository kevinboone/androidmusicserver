#!/bin/bash
# This script installs the corresponding Web page and sources on
# my Web server. No interest to anybody except me.

WEBSOURCE=/home/kevin/docs/kzone5/source
WEBTARGET=/home/kevin/docs/kzone5/target
VERSION=0.0.2

(cd ..; zip -r $WEBTARGET/androidmusicserver-${VERSION}.zip androidmusicserver/)
cp *.html $WEBSOURCE
cp *.png $WEBTARGET
cp bin/androidmusicserver-debug.apk $WEBTARGET/androidmusicserver-${VERSION}.apk
(cd $WEBSOURCE/..; ./make.pl androidmusicserver)


