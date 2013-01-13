set ECLIPSE_ADT_PATH=

git submodule init
git submodule update
%ECLIPSE_ADT_PATH%\tools\android.bat update project -p actionbarsherlock\library --target 1
%ECLIPSE_ADT_PATH%\tools\android.bat update project -p . --target 1
cp third_party\android-support-library\android-support-v4.jar actionbarsherlock\library\libs\android-support-v4.jar 
cd tests
%ECLIPSE_ADT_PATH%\tools\android.bat update test-project -m .. -p .
