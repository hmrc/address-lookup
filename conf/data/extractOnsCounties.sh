#!/bin/bash -e
# Data sourced from
#  * https://geoportal.statistics.gov.uk/geoportal/catalog/search/browse/browse.page
#  * find the most recent ONS Postcode Directory or National Statistics Postcode Lookup data
#
# Requires:
#  http://codeinthehole.com/writing/csvfilter-a-python-command-line-tool-for-manipulating-csv-data/

if [ "$1" = "" ]; then
    echo "Usage: $0 csv-file"
    exit 1
fi

FILE=$1
V=$(basename $FILE .csv)

case $V in
    ONSPD_*)
        # For ONSPD:
        #  col2 is UK postcode, normal format
        #  col4 is termination date (differentiates active and inactive postcodes)
        #  col5 is the county code
        #  col14 is the country code
        COLS=2,4,5
        KEEP=0,2
        ;;

    NSPL_*)
        # For NSPL:
        #  col2 is UK postcode, normal format
        #  col4 is termination date (differentiates active and inactive postcodes)
        #  col10 is the county code
        #  col15 is the country code
        COLS=2,4,10
        KEEP=0,2
        ;;

    *)
      echo Support for $V is not yet implemented. Please review this script.
      exit 1 ;;
esac

# filter to include only the interesting columns
# remove CR characters (leaving the newlines)
# remove rows with pseudo-codes (all 9s)
# include rows with blank cells (these have no termination date) followed by E (only English data is valid at present)
# remove the middle column (the termination date)
# remove rows that have a blank county
csvfilter -f $COLS $FILE \
  | grep -v 99999999 \
  | grep ,,E \
  | csvfilter -f $KEEP \
  | tr -d '\015' \
  | egrep -v ',$' \
  | gzip \
  > $V-filtered.active.csv.gz

