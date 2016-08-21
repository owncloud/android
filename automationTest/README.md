** Work in progress

This project contains a set of automatic tests operating in the UI level.

Tests are to be run with the tool Appium. Check [here][0] to install it and all its dependencies (including Maven).

* You will need to modify the constants in automationTest/src/test/java/com/owncloud/android/test/ui/testSuites/Config.java to assign appropiate values for your test server and accounts.

* You will need to include the ownCloud.apk to test in automationTest/src/test/resources/.
* To run the test succesfully, you will need to have an gmail account active in the device. This email account should have an email which subject UploadFile and a test.jpg file attached.
* To run the test succesfully, you will need a folder called ocAutomation. Inside that folder should be several files: año.pdf, doc.txt, docümento.txt, image.jpg, test and video.mp4
* Take into account that the device should be in English

To run the tests from command line, plug a device to your computer or start and emulator. Then type 

      mvn clean tests

To run only one category of the test

      mvn clean -Dtest=RunSmokeTests test

The project may also be imported in Eclipse, with the appropiate plug-ins, and run from it.

[0]: http://appium.io/slate/en/master/?java#about-appium
