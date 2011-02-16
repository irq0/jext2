#!/bin/sh
export LIBRARY_PATH="/usr/lib:/usr/local/lib:$HOME/opt/lib"
valgrind --trace-children=yes java -Djava.library.path=/usr/lib:/usr/local/lib:/home/vogon/opt/lib -jar dist/jext2-plusdepends.jar ${*}
