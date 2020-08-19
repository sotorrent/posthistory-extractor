#!/bin/bash

for file in *.csv
do
  sed -e 's/\&#xD;/\r/g' "$file" > tmp.csv && sed -e 's/\&#xA;/\n/g' tmp.csv > "$file" && rm tmp.csv
done
