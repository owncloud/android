#!/bin/bash

DIRECTORY="actionbarsherlock"


function initDefault {
    git submodule init
    git submodule update
    android update lib-project -p owncloud-android-library
    android update project -p .
    android update project -p oc_jb_workaround
    android update test-project -p tests -m ..
}

function initForAnt {
    if [ ! -d "$DIRECTORY" ]; then

        mkdir actionbarsherlock
        git submodule add --force git://github.com/JakeWharton/ActionBarSherlock.git ./actionbarsherlock
        git submodule init
        git submodule update
        cd actionbarsherlock/
        git checkout 9598f2b
        cd ../
        android update project -p actionbarsherlock/library -n ActionBarSherlock
        android update lib-project -p owncloud-android-library
        android update project -p .
        android update project -p oc_jb_workaround
        cp third_party/android-support-library/android-support-v4.jar actionbarsherlock/library/libs/android-support-v4.jar
        android update test-project -p tests -m ..
    fi
}

 if [ -z "$1" ]; then
              initDefault
              exit
 else
              echo "Creating environment for Ant"
              initForAnt
              exit
 fi




