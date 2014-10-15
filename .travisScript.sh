#!/usr/bin/env bash

# Copyright (c) 2013 Embark Mobile
# Licensed under the MIT License.
# https://github.com/embarkmobile/android-sdk-installer

set +e

#detecting os
os=linux
if [[ `uname` == 'Darwin' ]]; then
    os=osx
fi

# Constants
if [[ $os == 'linux' ]]; then
    SDK_FILE=android-sdk_r23.0.2-linux.tgz
elif [[ $os == 'osx' ]]; then
    SDK_FILE=android-sdk_r23.0.2-macosx.zip
fi
SDK_URL=http://dl.google.com/android/$SDK_FILE

DEFAULT_INSTALL=platform-tools
WAIT_FOR_EMULATOR_URL=https://github.com/embarkmobile/android-sdk-installer/raw/version-2/wait_for_emulator
ACCEPT_LICENSES_URL=https://github.com/embarkmobile/android-sdk-installer/raw/version-2/accept-licenses

# Defaults
INSTALLER_DIR=$HOME/.android-sdk-installer
INSTALL=""
LICENSES="android-sdk-license-5be876d5"

for i in "$@"
do
case $i in
    --dir=*)
    INSTALLER_DIR=`echo $i | sed 's/[-a-zA-Z0-9]*=//'`
    ;;
    --install=*)
    INSTALL=`echo $i | sed 's/[-a-zA-Z0-9]*=//'`
    ;;
    --accept=*)
    LICENSES=`echo $i | sed 's/[-a-zA-Z0-9]*=//'`
    ;;
    *)
    # unknown option
    ;;
esac
done

# Expand the path
if [[ $os == 'linux' ]]; then
    INSTALLER_DIR=`readlink -f "$INSTALLER_DIR"`
elif [[ $os == 'osx' ]]; then
    INSTALLER_DIR=`stat -f "$INSTALLER_DIR"`
fi

TOOLS_DIR=$INSTALLER_DIR/tools

echo "Installing SDK in $INSTALLER_DIR"

mkdir -p $INSTALLER_DIR
mkdir -p $TOOLS_DIR

echo "Downloading SDK"
wget -c -O $INSTALLER_DIR/$SDK_FILE $SDK_URL
echo "Extracting SDK"

if [[ $os == 'linux' ]]; then
    tar xzf $INSTALLER_DIR/$SDK_FILE --directory $INSTALLER_DIR
    export ANDROID_HOME=$INSTALLER_DIR/android-sdk-linux
elif [[ $os == 'osx' ]]; then
    unzip -q -d $INSTALLER_DIR $INSTALLER_DIR/$SDK_FILE
    export ANDROID_HOME=$INSTALLER_DIR/android-sdk-macosx
fi

# Download scripts
# Mac on Travis has issues with SSL certs if we don't use the -3 / -ssl3 option
curl -L -o $TOOLS_DIR/wait_for_emulator $WAIT_FOR_EMULATOR_URL
chmod +x $TOOLS_DIR/wait_for_emulator
curl -L -o $TOOLS_DIR/accept-licenses $ACCEPT_LICENSES_URL
chmod +x $TOOLS_DIR/accept-licenses


# Setup environment file
echo "export ANDROID_HOME=$ANDROID_HOME" > $INSTALLER_DIR/env
echo "export PATH=$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools:$TOOLS_DIR:\$PATH" >> $INSTALLER_DIR/env

# Install components
ALL_INSTALL=$DEFAULT_INSTALL,$INSTALL

echo "Installing $ALL_INSTALL"
$TOOLS_DIR/accept-licenses "$ANDROID_HOME/tools/android update sdk --no-ui -a --filter $ALL_INSTALL" "$LICENSES"
