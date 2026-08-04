#!/usr/bin/env sh
set -eu

test -f gradle/wrapper/gradle-wrapper.jar
chmod +x gradlew scripts/ci-gradle.sh scripts/ci-android-setup.sh

scripts/ci-android-setup.sh
exec scripts/ci-gradle.sh "$@"
