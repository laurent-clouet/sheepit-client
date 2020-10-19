#!/bin/bash -e

# Requires xmllint (part of `libxml2-utils`), svn, gpg, git (duh), curl and p7zip
# JAVA_HOME must be set to a java 8 JDK
#
# Usage: ./create_signed_release.sh $PATH_TO_KEYSTORE $PASSWORD $CERTIFICATE_ALIAS



echo ""
echo "### Setup ###"

keystore="$1"
password="$2"
cert_alias="$3"
git_url="https://github.com/laurent-clouet/sheepit-client"
svn_trunk_url="$git_url/trunk"
pwd=`pwd`
tmp_dir=`mktemp -d`
jvm_name="jdk-11.0.6+10-jre"

wrapper_dir="$tmp_dir/wrapper"
git clone $git_url $wrapper_dir
cd $wrapper_dir
git checkout wrapper
cd $pwd

echo ""
echo "### Generate version ###"

svn_info=`svn info $svn_trunk_url --xml`
echo "$svn_info"
echo ""
svn_revision=`echo $svn_info | xmllint --xpath "string(/info/entry/@revision)" -`
echo "Revision: $svn_revision"
version="6.$svn_revision.0"
echo "Version: $version"


echo ""
echo "### Create jar ###"

signed_jar="$pwd/sheepit-client-$version.jar"
echo $version > ./resources/VERSION
./gradlew shadowJar
unsigned_jar="$pwd/build/libs/sheepit-client-all.jar"
echo "Unsigned .jar: $unsigned_jar"
echo "Signing"
jarsigner -tsa http://timestamp.digicert.com -keystore $keystore -storepass $password -signedjar $signed_jar $unsigned_jar $cert_alias


echo ""
echo "### Build exe ###"

exe="$pwd/sheepit-$version.exe"
7z x -o$tmp_dir $wrapper_dir/$jvm_name.zip
mv $tmp_dir/$jvm_name $tmp_dir/jre
rm -rf $tmp_dir/jre/include $tmp_dir/jre/src.zip
cp -f $signed_jar $tmp_dir/jre/sheepit-client.jar

cd $tmp_dir/jre
7z a $tmp_dir/application.7z .
cd $tmp_dir
cp $wrapper_dir/config.cfg config.cfg
cp $wrapper_dir/starter.sfx starter.sfx
cat starter.sfx config.cfg application.7z > $exe
cd $pwd

echo ""
echo ".jar: $signed_jar"
echo ".exe: $exe"
