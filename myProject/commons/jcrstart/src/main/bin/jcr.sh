#!/bin/sh

PIDFILE="logs/jcr.pid"
PRG=$0
PRGDIR=`dirname "$PRG"`

printHelp () {
    echo -e "-- JCR SCRIPT USAGE --";
    echo -e "  $0 --start   :  Starts the jcr in secure mode";
    echo -e "  $0 --debug   :  Starts the jcr in debug mode";
    echo -e "  $0 --stop    :  Stops the jcr";
}

init() {
  if [ -e $PIDFILE ]; then
     echo "JCR may currently be running (`cat $PIDFILE`). Please shut down before attemtping to start new instance."
     exit 1
  fi
}

startJCR() {
  init
  EXEC_SCRIPT="start.sh"
  echo -e "-- Starting the JCR securely --"
  if [ -e "$PIDFILE" ]; then
    echo "JCR was not shut down properly. Please ensure process `cat $PIDFILE` is no longer running and remove $PIDFILE."
  else
    if [ ! -x "$PRGDIR/$EXEC_SCRIPT" ]; then
      echo "Could not find $PRGDIR/$EXEC_SCRIPT or it is not executable"
    else
      exec "$PRGDIR/$EXEC_SCRIPT"
    fi
  fi
}

startDebug() {
  init
  EXEC_SCRIPT="debug.sh"
  echo "-- Starting the JCR in debug mode --"
  if [ ! -x "$PRGDIR/$EXEC_SCRIPT" ]; then
    echo "Could not find $PRGDIR/$EXEC_SCRIPT or it is not executable"
  else
    exec "$PRGDIR/$EXEC_SCRIPT"
  fi
}

stopJCR() {
  EXEC_SCRIPT="stop.sh"
  echo " -- Stopping JCR -- "
  if [ ! -x "$PRGDIR/$EXEC_SCRIPT" ]; then
    echo "Could not find $PRGDIR/$EXEC_SCRIPT or it is not executable"
  else
    exec sh "$PRGDIR/$EXEC_SCRIPT"
  fi
}


case "$1" in
    --start | start)
        startJCR
        ;;
    --debug | debug)
        startDebug
        ;;
    --stop | stop)
        stopJCR
        ;;
    *)
        printHelp
        exit 0

esac

