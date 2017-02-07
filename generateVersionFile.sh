#!/bin/sh -e
# Creates the project_version.properties and/or project_version.txt to provide traceability from built artefacts back to source code.

if [ "$1" = "-f" ]; then
    FORCE=y
    shift
else
    FORCE=
fi

if [ -z "$3" ]; then
    echo "Usage: $0 rootProjectDir projectDir filename-base [main|gen]"
    echo "Example: $0 . dev/agent agent"
    exit 1
fi

rootProjectDir=$1
projectDir=$2
filename=$3
point=${4:-main}

mainDir="${projectDir}/src/$point"
resourcesDir="${mainDir}/resources"
webappDir="${projectDir}/src/$point/webapp"

resourcesFile="${resourcesDir}/${filename}_version.properties"
webappFile="${webappDir}/${filename}_version.txt"

[ -n "$FORCE" ] && rm -f $resourcesFile $webappFile

[ ! -d $resourcesDir -o -f $resourcesFile ] && [ ! -d $webappDir -o -f $webappFile ] && exit 0 # no further action needed

echo "Writing $resourcesFile $webappFile ..."

appendVersionTextFile()
{
    mkdir -p $resourcesDir
    echo "$1" >> $resourcesFile

    if [ -d $webappDir ]; then
        echo "$1" >> $webappFile
    fi
}

appendVersionTextFile "version.buildDir = ${projectDir}"
appendVersionTextFile "version.buildTag = ${BUILD_TAG:-DEVELOPER BUILD}"
appendVersionTextFile "version.buildUrl = ${BUILD_URL}"

gitVersionTxt="${rootProjectDir}/gitVersion.txt"

if [ -f $gitVersionTxt ]; then
    for line in $(cat $gitVersionTxt); do
        appendVersionTextFile "version.$line"
    done

elif [ -d ${rootProjectDir}/.git -o -d ${rootProjectDir}/../.git ]; then
    appendVersionTextFile "version.commit = $(git rev-parse HEAD | sed 's/:/_/g')"
    appendVersionTextFile "version.branch = $(git remote)/$(git rev-parse --abbrev-ref HEAD)"
    appendVersionTextFile "version.timestamp = $(date --iso-8601=seconds)"

elif [ -d ${rootProjectDir}/.hg -o ${rootProjectDir}/../.hg ]; then
    appendVersionTextFile "version.commit = $(hg tip --template '{node}\n')"
    appendVersionTextFile "version.branch = origin/$(hg bookmark | fgrep '*' | sed 's/^\s*\*\s*//' | sed 's/\s.*$//')"
    appendVersionTextFile "version.timestamp = $(date --iso-8601=seconds)"

else
    pwd
    echo "Failed: not a versioned workspace."
    exit 1
fi
