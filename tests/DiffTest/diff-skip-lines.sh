#!/usr/bin/env bash

##############################################################################
# diff-skip-lines.sh
#
#   Custom diff command which ignores lines based on a sed expression
#
# Usage:
#   bash diff-skip-lines.sh sed-expression input-file-1 input-file-2
# Example:
#   # remove lines 6 and 7 then diff
#   bash diff-skip-lines.sh '6,7d' file1.txt file2.txt
#
# Actual linux command:
#   diff -q <(sed -e "6,7d" file1.txt) <(sed -e "6,7d" file2.txt)
##############################################################################

expr1="${1}"
expr2="${expr1}"
input1=$2
input2=$3

diff -q <(sed -e "${expr1}" "${input1}") <(sed -e "${expr2}" "${input2}")
