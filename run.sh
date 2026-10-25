#!/bin/sh
set -eu
cd "$(dirname "$0")"
mkdir -p build/classes
find src/main/java -name '*.java' -print > build/main-sources.txt
javac --release 17 -encoding UTF-8 -Xlint:all -Werror -d build/classes @build/main-sources.txt
exec java -Dfile.encoding=UTF-8 -cp build/classes catalog.Main
