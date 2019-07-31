#!/bin/sh

function join() {
  local IFS=$1
  shift
  echo "$*"
}

MAIN_CLASS=#MAIN_CLASS#

BIN_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

ROOT_DIR=$(cd $BIN_DIR/.. && pwd)

APP_DIR=$ROOT_DIR/app
LIB_DIR=$ROOT_DIR/lib
CFG_DIR=$ROOT_DIR/cfg

CLASSPATH=$(join ':' $LIB_DIR/*.jar $APP_DIR/*.jar $CFG_DIR/*.*)

# Setup the JVM
if [ "x$JAVA" = "x" ]; then
    if [ "x$JAVA_HOME" != "x" ]; then
        JAVA="$JAVA_HOME/bin/java"
    else
        JAVA="java"
    fi
fi

SERVER_OPTS=""

while [ "$#" -gt 0 ]
do
    case "$1" in
      -cp)
          CLASSPATH="$CLASSPATH:$2"
          shift
          ;;
      -classpath)
          CLASSPATH="$CLASSPATH:$2"
          shift
          ;;
      --)
          shift
          break;;
      *)
          SERVER_OPTS="$SERVER_OPTS $1"
          ;;
    esac
    shift
done

SERVER_OPTS="$SERVER_OPTS -cp $CLASSPATH -Dcorant.config.location=$CFG_DIR"
exec $JAVA $SERVER_OPTS $MAIN_CLASS $*