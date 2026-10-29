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
# compile time. The adapter references class and field names by their original String values, so
# renaming them causes a MissingKotlinParameterException or silent null at runtime. -keepnames
# preserves those names while still allowing R8 to remove unused members — unlike -keep { *; }
# which would block all shrinking on every annotated class.
-keepnames @com.squareup.moshi.JsonClass class *

# --- Domain models serialized to Room via Moshi reflection ---
# AppRegistryProvider is a domain-layer class with no @JsonClass annotation. It is serialized to
# JSON by KotlinJsonAdapterFactory and stored as a plain string in the app_providers Room column.
# Without this rule R8 renames its fields, making stored JSON unstable across releases and causing
# app providers to silently disappear after an app update.
-keep class com.owncloud.android.domain.appregistry.model.AppRegistryProvider { *; }

# --- HTTP logging models serialized by Moshi reflection ---
# LogRequest, LogResponse and all nested data classes (Request, RequestInfo, Response,
# ResponseInfo, Reply, Body) in the logging package have no @JsonClass annotation.
# LogInterceptor serializes them via KotlinJsonAdapterFactory inside intercept(), which runs
# whenever the user enables HTTP logs (set_httpLogs preference, accessible in release).
# intercept() has no try/catch, so a renaming-induced serialization failure crashes all network
# requests the moment the user turns logging on to diagnose a problem.
-keep class com.owncloud.android.lib.common.http.logging.** { *; }


# --- Settings fragments and custom Preference subclasses loaded by name ---
# androidx.preference inflates app:fragment values from settings.xml using
# Fragment.instantiate(context, className) at runtime. R8 cannot infer this from bytecode,
# so renaming any subclass causes a ClassNotFoundException when tapping the item.
# A wildcard rule covers all current and future PreferenceFragmentCompat subclasses without
# requiring per-class enumeration. LargePreferenceCategory extends Preference (not
# PreferenceFragmentCompat) so it needs its own explicit rule.
-keep class * extends androidx.preference.PreferenceFragmentCompat { public <init>(...); }
-keep class com.owncloud.android.presentation.settings.LargePreferenceCategory { public <init>(...); }

