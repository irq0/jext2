#!/bin/sh
export JARDIRS="/usr/share/java /usr/local/share/java $HOME/opt/share/java $HOME/agntctrl/lib $HOME/agntctrl/bin $JAVA_PROFILER_HOME"

for dir in $JARDIRS; do
        [ -d "$dir" ] &&  CLASSES="$(find $dir -depth -type f -iname *.jar -printf :%p)"
            export CLASSPATH=".$CLASSES"
        done
export LIBRARY_PATH="/usr/lib:/usr/local/lib:$HOME/opt/lib:$HOME/agntctrl/lib:$JAVA_PROFILER_HOME"

java \
    -Xrunhprof:cpu=times,thread=y,depth=12,cutoff=0 \
    -Djava.library.path="$LIBRARY_PATH" -jar dist/jext2-plusdepends.jar ${*}
