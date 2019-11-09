#!/usr/bin/env bash

pwd

if [[ -z "$CRYPT_PASS" ]]
then
   echo "\$CRYPT_PASS is empty"
   exit 1
else
   echo "\$CRYPT_PASS available"
fi

cd signing

# to encrypt
#openssl aes-256-cbc -a -salt -k "$CRYPT_PASS" -in GooglePlay.p12 -out GooglePlay.p12.enc
#openssl aes-256-cbc -a -salt -k "$CRYPT_PASS" -in release.keystore -out release.keystore.enc

# Ubuntu 18.04 (openssl 1.1.0g+) needs -md md5
# https://askubuntu.com/questions/1067762/unable-to-decrypt-text-files-with-openssl-on-ubuntu-18-04/1076708
openssl aes-256-cbc -a -d -md md5 -k "$CRYPT_PASS" -in GooglePlay.p12.enc -out GooglePlay.p12
openssl aes-256-cbc -a -d -md md5 -k "$CRYPT_PASS" -in release.keystore.enc -out release.keystore

cd ..