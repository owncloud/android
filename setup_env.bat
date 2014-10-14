@echo off

if "%1" == "" goto initDefault :: It would be nice to force selection of a setup type
if "%1" == "default" goto initDefault
if "%1" == "ant" goto initForAnt
goto initInvalid

:initInvalid
echo "Input argument invalid. Provide argument [ant | default] to choose setup type. Leaving out the argument automatically chooses default."
goto exit

:initForAnt
echo "Executes ANT setup..."
if not exist .\actionbarsherlock (
    call git submodule init
    call git submodule update
    call android.bat update lib-project -p owncloud-android-library
    call android.bat update project -p .
    call android.bat update project -p oc_jb_workaround
    copy /Y third_party\android-support-library\android-support-v4.jar actionbarsherlock\library\libs\android-support-v4.jar
    call android.bat update test-project -p tests -m ..
)
goto exit

:initDefault
echo "Executes default setup..."
call git submodule init
call git submodule update
call android.bat update lib-project -p owncloud-android-library
call android.bat update project -p .
call android.bat update project -p oc_jb_workaround
call android.bat update test-project -p tests -m ..
goto exit

:exit
echo "Setup complete!"