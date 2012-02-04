#!/bin/bash
export SCRIPTPATH=$(dirname $0)
export JEXT2_PIDFILE="$(tempfile)"
export LIBRARY_PATH="/usr/lib:/usr/local/lib:$HOME/opt/lib"
export JAVA_COMMAND="java \
-Ddaemon.pidfile="$JEXT2_PIDFILE" \
-Djava.library.path="$LIBRARY_PATH""

export COMMANDLINE=${*}
if [ "$1" = "-server" ]; then
    JAVA_COMMAND="$JAVA_COMMAND -server"
    COMMANDLINE=${*:2}
elif [ "$1" = "-client" ]; then
    JAVA_COMMAND="$JAVA_COMMAND -client"
    COMMANDLINE=${*:2}
fi

JAVA_COMMAND="$JAVA_COMMAND -jar $SCRIPTPATH/dist/jext2-plusdepends.jar"


function launch_daemon()
{
    /bin/sh <<EOF
nohup ${*} > /dev/null 2>&1 &
pid=\$!
echo \${pid}
EOF
}

pid=$(launch_daemon $JAVA_COMMAND --daemon $COMMANDLINE)
sleep 3

if ps -p "${pid}" 2>&1 > /dev/null; then
    echo ${pid} > $JEXT2_PIDFILE
else
    $SCRIPTPATH/jext2.sh -h
    echo "Starting jext2 in daemon mode failed!"
    echo "NOTE: $0 doesn't report errors back to the console. Please use jext.sh to get an error message!"
fi

