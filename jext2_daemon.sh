#!/bin/sh
export SCRIPTPATH=$(dirname $0)
export JEXT2_PIDFILE="$(tempfile)"
export LIBRARY_PATH="/usr/lib:/usr/local/lib:$HOME/opt/lib"
export JAVA_COMMAND="java \
-Ddaemon.pidfile="$JEXT2_PIDFILE" \
-Djava.library.path="$LIBRARY_PATH" \
-jar $SCRIPTPATH/dist/jext2-plusdepends.jar "


function launch_daemon() 
{
    /bin/sh <<EOF
${*} >&- &
pid=\$!
echo \${pid}
EOF
}

pid=$(launch_daemon $JAVA_COMMAND -d ${*})
sleep 3

if ps -p "${pid}" 2>&1 > /dev/null; then
    echo ${pid} > $JEXT2_PIDFILE
else
    $SCRIPTPATH/jext2.sh -h
    echo "Starting jext2 in daemon mode failed!"
fi
