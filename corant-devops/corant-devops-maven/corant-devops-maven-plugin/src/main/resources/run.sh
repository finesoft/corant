# !/bin/sh
MAIN_CLASS=#MAIN_CLASS#
ADDITIONAL_VM_ARGUMENTS=#ADDITIONAL_VM_ARGUMENTS#
APPLICATION_NAME=#APPLICATION_NAME#

BIN_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

ROOT_DIR=$(cd $BIN_DIR/.. && pwd)

CLASSPATH="$ROOT_DIR/lib/*:$ROOT_DIR/app/*"

JAVA="java "

CORANT_JVM_OPTS=" -cp $CLASSPATH $ADDITIONAL_VM_ARGUMENTS"

if test -n "$APPLICATION_NAME"
then
	CORANT_JVM_OPTS="$CORANT_JVM_OPTS -Dcorant.application-name=$APPLICATION_NAME"
fi

exec $JAVA $CORANT_JVM_OPTS $* $MAIN_CLASS
