#!/bin/sh
export TPTP_AC_HOME=$HOME/agntctrl
export JAVA_PROFILER_HOME=$TPTP_AC_HOME/plugins/org.eclipse.tptp.javaprofiler
export JARDIRS="/usr/share/java /usr/local/share/java $HOME/opt/share/java $HOME/agntctrl/lib $HOME/agntctrl/bin $JAVA_PROFILER_HOME"

for dir in $JARDIRS; do
        [ -d "$dir" ] &&  CLASSES="$(find $dir -depth -type f -iname *.jar -printf :%p)"
            export CLASSPATH=".$CLASSES"
        done
export LIBRARY_PATH="/usr/lib:/usr/local/lib:$HOME/opt/lib:$HOME/agntctrl/lib:$JAVA_PROFILER_HOME"
export LD_LIBRARY_PATH="$LD_LIBRARY_PATH:$LIBRARY_PATH"

java -Xdebug \
    -Xnoagent \
    -Djava.compiler=NONE \
    '-agentlib:JPIBootLoader=JPIAgent:server=enabled;CGProf' \
    -Djava.library.path="$LIBRARY_PATH" -jar dist/jext2-plusdepends.jar ${*}
#    -enableassertions \
