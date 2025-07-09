#!/bin/bash

check_license_in_file() {
    if ! head -n 20 $FILE | grep -q "This program is free software: you can redistribute it and/or modify"
    then
        if ! head -n 20 $FILE | grep -q "Licensed under the Apache License, Version 2.0"
        then
            echo "$FILE does not contain a current copyright header"
        fi
    fi
}


for DIRS in owncloudApp/src owncloudData/src owncloudDomain/src owncloudTestUtil/src
do
    for FILE in $(find $DIRS -name "*.java" -o -name "*.kt")
    do
        check_license_in_file
    done
done

./gradlew ktlintFormat
