#!/bin/sh
export LIBRARY_PATH="/usr/lib:/usr/local/lib:$HOME/opt/lib"
java -server -Djava.library.path="$LIBRARY_PATH" -jar dist/jext2-plusdepends.jar ${*}
