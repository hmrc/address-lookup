#!/bin/bash -e
# Makes a selection of web requests to the address-lookup service
# in order to determine whether it is alive and responsive.
#
# Usage:
#    smokeTest.sh [root]
#

root=${1:-http://localhost:9000}

function get
{
    echo "curl -iSLf -H 'X-LOCALHOST-Origin: developer' $root$1"
    curl -iSLf -H 'X-LOCALHOST-Origin: developer' $root$1
}

get /ping

for p in FX9%207ED FX1A%207GA FX97%204TU; do
    echo "===== $p ======"
    get /v1/uk/addresses.json?postcode=$p
    echo
done

echo Success.
