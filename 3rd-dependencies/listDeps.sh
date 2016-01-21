# This script
cd ..
mvn dependency:list -DexcludeGroupIds=org.eclipse.hawkbit -Dsort=true -DoutputFile=dependencies.txt
find . -name dependencies.txt|while read i; do cat $i;done|grep '.*:.*:compile'|sort|uniq > 3rd-dependencies/compile.txt
find . -name dependencies.txt|while read i; do cat $i;done|grep '.*:.*:test'|sort|uniq > 3rd-dependencies/test.txt
find . -name dependencies.txt|while read i; do cat $i;done|grep '.*:.*:provided'|sort|uniq > 3rd-dependencies/provided.txt
find . -name dependencies.txt|while read i; do rm $i;done
cd 3rd-dependencies/
