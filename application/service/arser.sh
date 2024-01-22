#!/bin/bash
#

#BASEDIR=$PWD #Verzeichnis des Callers, aktuelles Verzeichnis
BASEDIR=$(dirname $0) #Verzeichnis des Skripts

PID_FILE="arser.pid"
LOG_DIR="$BASEDIR/../logs"

if [ ! -e "$LOG_DIR" ]; then
    mkdir "$LOG_DIR"
fi

start()
{
    local IS_RUNNING="0";

    if [ -f "$PID_FILE" ]; then
        local PID=$(cat "$PID_FILE");

        if [ "$(ps -e | grep -c $PID)" == "1" ]; then
            echo "arser already running with PID: $PID";
            IS_RUNNING="1";
        fi
    fi

    if [ "$IS_RUNNING" == "0" ]; then
        echo -n "Starting arser";

        java -cp "$BASEDIR/../libs/*:$BASEDIR/../resources" de.freese.arser.core.ArserLauncher >> "$LOG_DIR/console.log" 2>&1 &

        echo $! > $PID_FILE && chmod 600 $PID_FILE;
        echo " with PID: $(cat $PID_FILE)";
    fi
}

stop() {
    if [ -f "$PID_FILE" ]; then
        local PID=$(cat "$PID_FILE");
        echo "Stopping arser with PID: $PID";
        kill -15 $PID;
        rm "$PID_FILE";
    else
        echo "Can not stop arser - no PID_FILE found!";
    fi

  #top -u arser -n 1
}

status() {
    local IS_RUNNING="0";

    if [ -f "$PID_FILE" ]; then
        local PID=$(cat "$PID_FILE");

        if [ "$(ps -e | grep -c $PID)" == "1" ]; then
            IS_RUNNING="1";
        fi
    fi

    if [ "$IS_RUNNING" == "1" ]; then
        echo "arser already running with PID: $PID";
    else
        echo "arser is not running or PID_FILE not found";
    fi
}

case "$1" in
    start)
        start
        ;;
    stop)
        stop
        ;;
    restart)
        $0 stop
        $0 start
        ;;
    status)
        status
        ;;
    *)
        echo "Usage: $0 {start|stop|restart|status}"
        ;;
esac
