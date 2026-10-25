#!/bin/sh
set -eu
cd "$(dirname "$0")"
mkdir -p build/test-classes
find src/main/java src/test/java -name '*.java' -print > build/test-sources.txt
javac --release 17 -encoding UTF-8 -Xlint:all -Werror -d build/test-classes @build/test-sources.txt
exec java -Dfile.encoding=UTF-8 -cp build/test-classes catalog.CatalogTest
