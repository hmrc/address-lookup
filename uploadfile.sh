#!/bin/bash -e

collection=addressbase
root=http://localhost:9000
linesPerFile=100000
drop=
password=
limit=1000000

# not using /tmp because of the risk of full partitions
tmpdir=~/uploadtmp
here=$PWD
scriptdir=$here/$(dirname $0)

function usage
{
   echo "Usage: $0 [-c name] [-p password] [-r root] [-s chunksize] [-m limit] [-d] file ..."
   echo "  -c   collection name [$collection]"
   echo "  -p   password"
   echo "  -r   server root [$root]"
   echo "  -s   size of chunks [$linesPerFile]"
   echo "  -m   upload the first available chunks only, up to the limit, then stop."
   echo "  -d   drop the existing collection before uploading new data - WARNING use with care!"
   exit 1
}

function splitInterruptHandler
{
    echo
    echo "Interrupted splitting." >&2
    cd $here
    rm -r $tmpdir
    exit 1
}

function uploadInterruptHandler
{
    echo
    echo
    echo "Interrupted uploading." >&2
    echo -n "Remaining files: "
    ls | wc -l
    exit 1
}

if [ $# -lt 1 ]; then
    usage
fi

OPTIND=1 # good practice to set this explicitly
while getopts "dc:m:p:r:s:" opt; do
  case $opt in
    d) drop=true ;;
    m) limit=$OPTARG;;
    c) collection=$OPTARG ;;
    p) password=$OPTARG ;;
    r) root=$OPTARG ;;
    s) linesPerFile=$OPTARG ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
      ;;
    :)
      echo "Option -$OPTARG requires an argument." >&2
      exit 1
      ;;
    *) usage ;;
  esac
done
shift "$((OPTIND-1))" # Shift off the options and optional --.

if [ -z "$password" ]; then
  read -sp "Password: " password
  echo
fi

upload2=$root/admin/upload2

[ -z "$drop" ] || rm -rf $tmpdir

chunk=0

for file in $@; do
  name=$(basename $file)

  case "$file" in
    *.gz)  CAT='gunzip -c' ;;
    *.zip) CAT='unzip -p' ;;
    *)     CAT='cat' ;;
  esac

  if [ ! -d $tmpdir ]; then
    mkdir $tmpdir
    cd $tmpdir
    trap 'splitInterruptHandler' INT

    splitter=$scriptdir/bin/$(uname -s)-$(uname -m)/splitter
    if [ -x $splitter ]; then
      echo "Splitting $file (fast) ..."
      # the splitter program works faster by directly writing gzipped files
      $splitter -i -chunk $linesPerFile -gz -outdir . $here/$file

    else
      echo "Splitting $file (slowly) ..."
      $CAT "$here/$file" | split -a 6 -l $linesPerFile -
      for f in $(find . -type f); do
        mv $f $f.csv
      done

      echo -n "Gzipping chunks "
      ls | wc -l
      # need a loop here because OSX/FreeBSD is a bit rubbish
      for csv in $(find . -type f -name \*.csv); do
        gzip $csv
      done
    fi
    trap - INT
  else
    cd $tmpdir
  fi

  trap 'uploadInterruptHandler' INT

  if [ -n "$drop" ]; then
    curl -if --user "admin:$password" -X DELETE $upload2/$collection
  fi

  for f in $(find . -type f); do
    echo $f
    n=$(echo $f | sed 's#/#-#g')
    curl -if --user "admin:$password" -X POST \
         -H 'Content-Type: text/plain; charset=UTF-8' -H 'Content-Encoding: gzip' \
         --data-binary @$f $upload2/$collection/$name:$n && rm -v $f
    chunk=$[$chunk + 1]
    if [ $chunk -gt $limit ]; then
      echo Quit after chunk $limit as requested.
      exit 0
    fi
  done

  cd $here
  rmdir $tmpdir 2> /dev/null ||:
  if [ -d $tmpdir ]; then
      echo Incomplete upload. These chunks remain:
      ls $tmpdir
  else
      echo Upload complete. The total number of records uploaded should have been
      $CAT $file | wc -l
  fi
done
