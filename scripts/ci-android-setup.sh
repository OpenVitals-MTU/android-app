#!/usr/bin/env sh
set -eu

android_platform_dir="android-37.0"
android_build_tools_dir="37.0.0"
android_platform_package="platforms;${android_platform_dir}"
android_build_tools_package="build-tools;${android_build_tools_dir}"

sdk_root="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [ -z "$sdk_root" ] && [ -f local.properties ]; then
    sdk_root="$(sed -n 's/^sdk\.dir=//p' local.properties | head -n 1)"
fi

if [ -z "$sdk_root" ]; then
    echo "ANDROID_HOME or ANDROID_SDK_ROOT must be set." >&2
    exit 1
fi

if [ ! -d "$sdk_root/platforms/$android_platform_dir" ] ||
    [ ! -d "$sdk_root/build-tools/$android_build_tools_dir" ]; then
    yes | sdkmanager "$android_platform_package" "$android_build_tools_package"
else
    echo "Android SDK $android_platform_dir and build-tools $android_build_tools_dir are already available."
fi

test -d "$sdk_root/platforms/$android_platform_dir"
test -d "$sdk_root/build-tools/$android_build_tools_dir"
printf 'sdk.dir=%s\n' "$sdk_root" > local.properties
