#!/bin/sh

LOGFILE=logs/jcrstart_debug.log
PIDFILE=logs/jcr.pid

touch $LOGFILE
rm $LOGFILE

pid=`$JAVA_HOME/bin/java -Xms512m -Xmx1024m -Xdebug -Xrunjdwp:transport=dt_socket,server=n,suspend=n,address=5005 -jar jcrstart-jar-with-dependencies.jar > $LOGFILE 2>&1 & echo $!`
echo $pid > $PIDFILE
echo "JCR process started `cat $PIDFILE`"
