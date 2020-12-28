#!/bin/sh
cd rdash-core
gradle --no-daemon build
cd ../rdash-se
gradle --no-daemon build
cd ..
cp rdash-se/build/libs/rdash-se-master.jar RDASH.jar
cd rdash-core/build/classes/java/main
zip -r ../../../../../RDASH.jar *
