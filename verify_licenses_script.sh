#!/bin/bash

check_license_in_file() {
    if ! head -n 20 $FILE | grep -q "Permission is hereby granted, free of charge, to any person obtaining a copy"
    then
        echo "$FILE does not contain a current copyright header"
    fi
}

for FILE in $(find owncloudComLibrary/src -name "*.java" -o -name "*.kt")
do
    check_license_in_file
done

./gradlew ktlintFormat
