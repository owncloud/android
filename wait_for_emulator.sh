#!/bin/bash

bootanim=""
failcounter=0
checkcounter=0
until [[ "$bootanim" =~ "stopped" ]]; do
   bootanim=`adb -e shell getprop init.svc.bootanim 2>&1`
   echo "($checkcounter) $bootanim"
   if [[ "$bootanim" =~ "not found" ]]; then
      let "failcounter += 1"
      if [[ $failcounter -gt 300 ]]; then
        echo "Failed to start emulator"
        exit 1
      fi
   fi
   let "checkcounter += 1"
   sleep 1
done
echo "Done"
