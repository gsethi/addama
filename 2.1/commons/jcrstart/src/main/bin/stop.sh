#!/bin/sh

PIDFILE="logs/jcr.pid"

  if [ -e "$PIDFILE" ]; then
    if [ ! -z "$PIDFILE" ]; then
      echo "Stopping jcr process `cat $PIDFILE`"
      kill `cat $PIDFILE`
      rm $PIDFILE
    else
      echo "Failed to stop jcr process. $PIDFILE may not have a process id."
    fi
  else
    echo "$PIDFILE not found. Check process list to ensure jcr is not running."
  fi
