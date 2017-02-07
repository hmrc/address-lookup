#!/bin/bash
FILES=./uploadData/*

curl -X DELETE "localhost:9000/admin/upload2/addressbase"

for f in $FILES
do
  echo
  echo "Processing $f file..."
  while( ! curl -X POST -H 'Content-Type: text/plain; charset=UTF-8' --data-binary @$f "localhost:9000/admin/upload2/addressbase/addressbase" ) ; do 
    echo
    echo "curl failed $f"
  done
done
