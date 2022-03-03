#!/bin/bash

check_directory() {
    if ! head -n 20 $FILE | grep -q "Permission is hereby granted, free of charge, to any person obtaining a copy"
    then
        echo "$FILE does not contain a current copyright header"
    fi
}

for FILE in $(find owncloudComLibrary/src -name "*.java" -o -name "*.kt")
do
    check_directory
done

./gradlew ktlintFormat
