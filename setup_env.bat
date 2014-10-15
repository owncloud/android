@echo off

if "%1" == "gradle" goto initDefault
if "%1" == "maven" goto initDefault
if "%1" == "ant" goto initForAnt
goto initInvalid

:initInvalid
echo "Input argument invalid. Provide argument [ant | maven | gradle] to choose setup type."
goto exit

:initForAnt
echo "Executing Ant setup..."
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
call git submodule init
call git submodule update
call android.bat update lib-project -p owncloud-android-library
call android.bat update project -p .
call android.bat update project -p oc_jb_workaround
call android.bat update test-project -p tests -m ..
goto exit

:exit
echo "Setup complete!"