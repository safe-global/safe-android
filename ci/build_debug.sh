#!/bin/bash
# fail if any commands fails
set -e

./gradlew clean assembleDebug createDebugTestCoverage -PdisablePreDex --stacktrace
android-wait-for-emulator
adb shell settings put global window_animation_scale 0 &
adb shell settings put global transition_animation_scale 0 &
adb shell settings put global animator_duration_scale 0 &
adb shell input keyevent 82 &
./gradlew connectedCheck -PdisablePreDex --stacktrace
