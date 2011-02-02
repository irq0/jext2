#!/bin/sh
export LIBRARY_PATH="/usr/lib:/usr/local/lib:$HOME/opt/lib"
java -Xdebug \
    -Xnoagent \
    -Djava.compiler=NONE \
    -enableassertions \
    -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=0.0.0.0:5005 \
    -Djava.library.path="$LIBRARY_PATH" -jar dist/jext2-plusdepends.jar ${*}
