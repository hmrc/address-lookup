#!/bin/bash -e
cd $(dirname $0)
find . -type d -name target      | xargs rm -rf
find . -type d -name lib_managed | xargs rm -rf
