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
#
# Customization:
#   This script automatically uses the 'gpunit.properties' file
# from the current working directory, if one exists.
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

_gpunit_properties_arg=

if [[ -e "gpunit.properties" ]]; then
  _gpunit_properties_arg="-Dgpunit.properties=`pwd`/gpunit.properties"
fi

# for debugging ...
#   echo "command: ant -f ${GPUNIT_HOME}/build.xml ${_gpunit_properties_arg:-} ${@:-}"

ant -f ${GPUNIT_HOME}/build.xml "${_gpunit_properties_arg:-}" "${@:-}"
