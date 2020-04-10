# !/bin/sh
function join() {
  local IFS=$1
  shift
  echo "$*"
}

MAIN_CLASS=#MAIN_CLASS#
USED_CONFIG_LOCATION=#USED_CONFIG_LOCATION#
USED_CONFIG_PROFILE=#USED_CONFIG_PROFILE#
ADDITIONAL_SYSTEM_PROPERTIES=#ADDITIONAL_SYSTEM_PROPERTIES#
ADDITIONAL_VM_ARGUMENTS=#ADDITIONAL_VM_ARGUMENTS#
APPLICATION_ARGUMENTS=#APPLICATION_ARGUMENTS#
APPLICATION_NAME=#APPLICATION_NAME#

BIN_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

ROOT_DIR=$(cd $BIN_DIR/.. && pwd)

APP_DIR=$ROOT_DIR/app
LIB_DIR=$ROOT_DIR/lib
CFG_DIR=$ROOT_DIR/cfg
LCF_URL=$CFG_DIR/log4j2.xml

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

if [ "x$ADDITIONAL_VM_ARGUMENTS" != "x" ]; then
	SERVER_OPTS="$SERVER_OPTS $ADDITIONAL_VM_ARGUMENTS"
fi

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

if [ "x$USED_CONFIG_LOCATION" = "x" ]; then
	USED_CONFIG_LOCATION="filesystem:$CFG_DIR"
fi

SERVER_OPTS="$SERVER_OPTS -cp $CLASSPATH -Dcorant.config.location=$USED_CONFIG_LOCATION"

if [ -f "$LCF_URL" ]; then
    SERVER_OPTS="$SERVER_OPTS -Dlog4j.configurationFile=$LCF_URL"
fi

if [ "x$USED_CONFIG_PROFILE" != "x" ]; then
	SERVER_OPTS="$SERVER_OPTS -Dcorant.config.profile=$USED_CONFIG_PROFILE"
fi

if [ "x$APPLICATION_NAME" != "x" ]; then
	SERVER_OPTS="$SERVER_OPTS -Dcorant.application-name=$APPLICATION_NAME"
fi

if [ "x$ADDITIONAL_SYSTEM_PROPERTIES" != "x" ]; then
	SERVER_OPTS="$SERVER_OPTS $ADDITIONAL_SYSTEM_PROPERTIES"
fi

SERVER_OPTS="$SERVER_OPTS -Dcorant.application.root-dir=filesystem:$ROOT_DIR"

exec $JAVA $SERVER_OPTS $MAIN_CLASS $APPLICATION_ARGUMENTS $*
