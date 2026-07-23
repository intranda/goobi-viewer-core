#!/bin/bash

###
#
# Do a git pull on the given directory.
#
# The script expects one parameter: the path to the git repository of the theme
#
# Git pull is done quitetly. If an error occurs exit directly. If it is successfull
# generate an XML output with git branch, git hash and git commit message. This is 
# used on the developer page in the Goobi viewer backend.
#
###


## Check if the repository path parameter is provided
if [ -z "$1" ]; then
    echo "ERROR: Repository path not provided." >&2
    exit 1
fi

REPOSITORYPATH="$1"


## Check if the repository path is non-empty and points to an existing directory
if [ -z "${REPOSITORYPATH}" ] || [ ! -d "${REPOSITORYPATH}" ]; then
    echo "ERROR: Invalid repository path or directory does not exist." >&2
    exit 1
fi


## Pull quietly and exit directly with error message on error stream from git pull command if pull fails
PULLOUTPUT=$(git -C "${REPOSITORYPATH}" pull -q);
if [ $? -ne 0 ]; then
  exit 1
fi


## If pulling had no errors, store more information in variables and generate an XML output
GITBRANCH=$(git -C ${REPOSITORYPATH} branch --show-current)
GITHASH=$(git -C ${REPOSITORYPATH} rev-parse --short=7 HEAD)
GITCOMMITMESSAGE=$(git -C ${REPOSITORYPATH} log --no-walk --pretty=format:%s)

sed -e "s|GITBRANCH|${GITBRANCH}|g" -e "s|GITHASH|${GITHASH}|g" -e "s|GITCOMMITMESSAGE|${GITCOMMITMESSAGE}|g" << EOF
<?xml version="1.0" encoding="UTF-8" ?>
<themepull>
  <branch>GITBRANCH</branch>
  <revision>GITHASH</revision>
  <message>GITCOMMITMESSAGE</message>
</themepull>
EOF

