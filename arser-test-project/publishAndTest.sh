#! /bin/bash
#
# Exit, if one Command fails.
set -e

trap bashtrap SIGINT SIGTERM

bashtrap() {
	echo "Exit"
	exit 1;
}

# BASEDIR=$PWD # Caller directory, current directory
BASEDIR=$(dirname "$0") # Script directory
cd "$BASEDIR" || exit

export GRADLE_USER_HOME=/tmp/.gradle-arser

echo -e "\\n\\033[46;1;31mAPI\\033[0m"
gradle -p api/ clean publish

echo -e "\\n\\033[46;1;31mIMPL\\033[0m"
gradle -p impl/ clean build --refresh-dependencies
