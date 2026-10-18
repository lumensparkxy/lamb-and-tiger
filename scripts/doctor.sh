#!/bin/sh
set -eu
: "${JAVA_HOME:?Set JAVA_HOME to a compatible JDK}"
: "${ANDROID_HOME:?Set ANDROID_HOME to your Android SDK}"
"$JAVA_HOME/bin/java" -version
for task_path in "$ANDROID_HOME/platforms/android-37/android.jar" "$ANDROID_HOME/platforms/android-37.0/android.jar"; do
  if [ -f "$task_path" ]; then printf 'Compile SDK found: %s\n' "$task_path"; break; fi
done
[ -f "$task_path" ] || { printf 'Missing compile SDK 37\n'; exit 1; }
[ -d "$ANDROID_HOME/build-tools/36.0.0" ] || { printf 'Missing Build Tools 36.0.0\n'; exit 1; }
"$ANDROID_HOME/platform-tools/adb" devices -l
./gradlew --version
