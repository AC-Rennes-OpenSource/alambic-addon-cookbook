#!/bin/bash

#----------------------------------------------
# README
# Ce script a pour objectif de service de présentation
#----------------------------------------------

#----------------------------------------------
# Variables
#----------------------------------------------
VERBOSE=0
RECIPE=""
SOME_PARAMETER=""

#----------------------------------------------
# functions
#----------------------------------------------
logger() {
  if [ "ERROR" = $1 ] || [ $VERBOSE -eq 2 ] || [ $1 = "INFO" -a $VERBOSE -eq 1 ]
  then
    echo "[$1]" $2
  fi
}

usage() {
  echo "Usage: \"$0 -p <some parameter> [-v (verbose) -d (debug)]\""
}

finally() {
  logger "INFO" "Fin de traitement du script"
  exit $1
}

#---------------------------------------------
# Options
#---------------------------------------------
if [ $# -ge 1 ]
then
  # parse the command options
  while getopts "vdb:r:p:" opt
  do
    case $opt in
      v)
        # enable verbose simple logs
        VERBOSE=1
        ;;
      d)
        # enable debug logs
        VERBOSE=2
        ;;
      b)
        # the cook book's name
        COOK_BOOK=$OPTARG
        ;;
      r)
        # the recipe's name
        RECIPE=$OPTARG
        ;;
      p)
        # some parameter
        SOME_PARAMETER=$OPTARG
        ;;
      \?)
        logger "ERROR" "Invalid argument: -$OPTARG" >&2
        usage
        finally 1
        ;;
    esac
  done
fi

logger "INFO" "Début de l'atelier de cuisine"
if [ $VERBOSE -eq 2 ]
then
  ./runner.sh -f ../jobs/${COOK_BOOK} -j "${RECIPE}" -p "${SOME_PARAMETER}" -d
else
  ./runner.sh -f ../jobs/${COOK_BOOK} -j "${RECIPE}" -p "${SOME_PARAMETER}" -v
fi

finally 0