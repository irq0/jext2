#!/bin/sh
export LIBRARY_PATH="/usr/lib:/usr/local/lib:$HOME/opt/lib"
java -Xdebug \
    -Xnoagent \
    -Djava.compiler=NONE \
    -enableassertions \
    -Dcom.sun.management.jmxremote.port=5002 \
    -Dcom.sun.management.jmxremote.authenticate=false \
    -Dcom.sun.management.jmxremote.ssl=false \
    -Djava.rmi.server.hostname=192.168.171.131 \
    -Djava.library.path="$LIBRARY_PATH" -jar dist/jext2-plusdepends.jar ${*}
