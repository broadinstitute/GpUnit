#!/usr/bin/env bash

############################################################
# gpunit.sh
#   GpUnit wrapper script as a convenience for invoking
# ant directly from the command line.
#
# Setup:
#   # make a symlink
#   ln -s gpunit.sh gpunit
#   
#   # add ${GPUNIT_HOME}/bin to your path
#   export PATH=${PATH}:${GPUNIT_HOME}/bin
#
# Example usage:
#   # 'ant -p' equivalent
#   gpunit -p
#
#   # ant help equivalent
#   gpunit help
#
#   # ant gpunit equivalent   
#   gpunit gpunit
############################################################

# bash (unofficial strict mode) shell options
#   see: https://www.gnu.org/software/bash/manual/html_node/The-Set-Builtin.html
#   see: http://redsymbol.net/articles/unofficial-bash-strict-mode/
set -euo pipefail
IFS=$'\n\t'

# set GPUNIT_HOME if not already set, default=../
: ${GPUNIT_HOME="$( cd "$( dirname "${BASH_SOURCE[0]}" )" ; cd ../ && pwd )"}

# check for 'ant' command
command -v 'ant' || { ec=$?; echo "ant command not found"; exit $ec; }

_ant_args=();
if [[ $# -ge 1 ]]; then
  _ant_args=( "$@" )
fi

ant -f ${GPUNIT_HOME}/build.xml "${@:-}"
