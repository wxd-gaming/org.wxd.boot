#!/bin/bash

. /etc/rc.d/init.d/functions

MEM=4g
JAR=`pwd`/server-center.jar
PID=`ps -ef | grep ${JAR} | grep -v grep | awk '{print $2}'`

exists()
{
	PID=`ps -ef | grep ${JAR} | grep -v grep | awk '{print $2}'`
		if [ ${#PID} -ne 0 ]; then
			return 1
		fi
	return 0
}

start()
{
	echo "startting"
	exists
	if [ $? -ne 0 ]; then
		action $"failed $prog: " /bin/false
		return 1
	fi

	JAVA_PARAM="-Xms${MEM} -Xmx${MEM} -Xss512k -Dfile.encoding=UTF-8 -XX:InitialCodeCacheSize=64m -XX:ReservedCodeCacheSize=500m -Dsun.jnu.encoding=UTF-8 -XX:CICompilerCount=2 -XX:+HeapDumpOnOutOfMemoryError -XX:-OmitStackTraceInFastThrow -XX:MaxDirectMemorySize=512m -XX:MaxGCPauseMillis=100 -Djdk.attach.allowAttachSelf=true -Xlog:gc*:gc.log:time,level,tags -XX:+UseG1GC -server -jar"

	nohup /usr/local/openjdk-21/bin/java ${JAVA_PARAM} ${JAR} > nohup.out 2>&1 &

	action $"sucess, java process start $prog: " /bin/true
	return 0
}

stop()
{
	echo "stopping"
	exists
	if [ $? -ne 0 ]; then
		kill -15 ${PID}
	fi

	for (( i=0; i<3000; i++)); do
		exists
		if [ $? -ne 0 ]; then
			echo "java process is exists, try again after 1 sec..."
			sleep 2
		else
			break
		fi
	done

	exists
	if [ $? -ne 0 ]; then
		action $"failed $prog: " /bin/false
		return 1
	fi
	action $"sucess $prog: " /bin/true
	return 0
}

restart()
{
	stop
	if [ $? -eq 0 ]; then
		start
	fi
}

###########################################
if [ $# -ne 1 ]; then
	echo "$0 start|restart|stop"
	exit
fi

case "$1" in
"start") start
exit
;;
"restart") restart
exit
;;
"stop") stop
exit
;;
*) echo "$0 start|restart|stop"
exit
;;
esac
