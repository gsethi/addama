#!/bin/sh

LOGFILE=logs/jcrstart.log
PIDFILE=logs/jcr.pid


if [ -e "$PIDFILE" ]; then
  echo "JCR was not shut down properly. Please ensure process `cat $PIDFILE` is no longer running and remove $PIDFILE."
  exit
fi

touch $LOGFILE
rm $LOGFILE

pid=`$JAVA_HOME/bin/java -Xms512m -Xmx1024m -jar jcrstart-jar-with-dependencies.jar > $LOGFILE 2>&1 & echo $!`
echo $pid > $PIDFILE
echo "JCR process started `cat $PIDFILE`" 
