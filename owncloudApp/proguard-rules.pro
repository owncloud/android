# App-specific R8/ProGuard keep rules for owncloudApp.
#
# R8 shrinks the release APK by removing unused code and renaming classes/members to short
# names. That breaks any code path where a class is looked up by its original name at runtime
# (reflection, framework inflation, serialization). Each section below targets one such path.
#
# Library consumer rules (Room, Coroutines, Moshi, Koin, OkHttp) are bundled with each
# library's AAR and applied automatically by R8 — no duplication needed here.

# --- WorkManager workers ---
# WorkManager stores the worker class name as a String in the WorkRequest and re-creates the
# instance at runtime using Class.forName(). If R8 renames or removes a worker class the job
# silently fails with a ClassNotFoundException. Keeping the constructors is enough: WorkManager
# calls the two-argument (Context, WorkerParameters) constructor via reflection.
-keep class com.owncloud.android.workers.** { public <init>(...); }

# --- Moshi JSON models ---
# Moshi's KSP code generator produces a JsonAdapter for every @JsonClass-annotated class at
# compile time. The adapter references field names by their original String values, so renaming
# a field or its enclosing class causes a MissingKotlinParameterException or silent null at
# runtime. Keeping all members (fields + accessors) prevents that. This rule matches all
# annotated classes in owncloudComLibrary without enumerating them individually.
-keep @com.squareup.moshi.JsonClass class * { *; }

# --- Custom Views inflated from XML ---
# The Android LayoutInflater reads the fully-qualified class name from the XML attribute and
# calls the constructor via reflection (typically the two-argument Context+AttributeSet form).
# R8 cannot infer this usage from bytecode alone, so each class must be named explicitly.
# Only constructors are kept; the rest of each class is still subject to obfuscation.
-keep class com.owncloud.android.presentation.files.SortOptionsView { public <init>(...); }
-keep class com.owncloud.android.presentation.security.passcode.NumberKeyboard { public <init>(...); }
-keep class com.owncloud.android.presentation.security.passcode.SquareFrameLayout { public <init>(...); }
-keep class com.owncloud.android.ui.preview.ViewPagerWorkAround { public <init>(...); }

# --- Settings fragments and custom Preference subclasses loaded by name ---
# androidx.preference inflates app:fragment values from settings.xml using
# Fragment.instantiate(context, className) at runtime. R8 cannot infer this from bytecode,
# so renaming any of these fragments causes a ClassNotFoundException when tapping the item.
# LargePreferenceCategory is a custom Preference subclass in settings_picture/video_uploads.xml
# and is also inflated by the preference framework via reflection.
-keep class com.owncloud.android.presentation.settings.security.SettingsSecurityFragment { public <init>(...); }
-keep class com.owncloud.android.presentation.settings.logging.SettingsLogsFragment { public <init>(...); }
-keep class com.owncloud.android.presentation.settings.automaticuploads.SettingsPictureUploadsFragment { public <init>(...); }
-keep class com.owncloud.android.presentation.settings.automaticuploads.SettingsVideoUploadsFragment { public <init>(...); }
-keep class com.owncloud.android.presentation.settings.advanced.SettingsAdvancedFragment { public <init>(...); }
-keep class com.owncloud.android.presentation.settings.more.SettingsMoreFragment { public <init>(...); }
-keep class com.owncloud.android.presentation.settings.LargePreferenceCategory { public <init>(...); }

