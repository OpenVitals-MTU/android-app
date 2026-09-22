#!/usr/bin/env sh
set -eu

is_supported_jdk() {
    java_home="$1"
    if [ -z "$java_home" ] || [ ! -x "$java_home/bin/java" ]; then
        return 1
    fi

    "$java_home/bin/java" -version 2>&1 | head -n 1 | grep -Eq 'version "(17|21)\.'
}

find_supported_jdk() {
    for java_home in "$@"; do
        if is_supported_jdk "$java_home"; then
            printf '%s\n' "$java_home"
            return 0
        fi
    done

    for java_bin in /usr/lib/jvm/*/bin/java /opt/*/bin/java; do
        java_home="${java_bin%/bin/java}"
        if is_supported_jdk "$java_home"; then
            printf '%s\n' "$java_home"
            return 0
        fi
    done

    return 1
}

install_jdk17() {
    runner=""
    if [ "$(id -u)" -ne 0 ]; then
        if ! command -v sudo >/dev/null 2>&1; then
            echo "JDK 17 is required, but sudo is unavailable for installation." >&2
            return 1
        fi
        runner="sudo"
    fi

    if command -v apt-get >/dev/null 2>&1; then
        $runner apt-get update
        $runner env DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends openjdk-17-jdk-headless
        return 0
    fi

    if command -v apk >/dev/null 2>&1; then
        $runner apk add --no-cache openjdk17
        return 0
    fi

    echo "JDK 17 is required, but this image has no supported package manager." >&2
    return 1
}

JAVA_HOME="$(
    find_supported_jdk \
        "${OPENVITALS_CI_JAVA_HOME:-}" \
        "${JAVA_HOME:-}" \
        /usr/lib/jvm/java-17-openjdk-amd64 \
        /usr/lib/jvm/java-17-openjdk \
        /usr/lib/jvm/temurin-17-jdk-amd64 \
        /usr/lib/jvm/temurin-17-jdk \
        /usr/lib/jvm/java-21-openjdk-amd64 \
        /usr/lib/jvm/java-21-openjdk \
        /opt/java/openjdk \
    || true
)"

if [ -z "$JAVA_HOME" ]; then
    install_jdk17
    JAVA_HOME="$(
        find_supported_jdk \
            /usr/lib/jvm/java-17-openjdk-amd64 \
            /usr/lib/jvm/java-17-openjdk \
            /usr/lib/jvm/temurin-17-jdk-amd64 \
            /usr/lib/jvm/temurin-17-jdk
    )"
fi

export JAVA_HOME
export PATH="$JAVA_HOME/bin:$PATH"

shared_gradle_home=/woodpecker/cache/gradle

# A step that signs gets its own, empty Gradle home. Every pipeline can write the shared
# one, pull requests included. An init.d script or a gradle.properties planted there would
# run with the keystore, its password and the release token in scope.
signing_build=false
if [ -n "${OPENVITALS_RELEASE_STORE_FILE:-}" ] || [ -n "${OPENVITALS_RELEASE_KEYSTORE_BASE64:-}" ]; then
    signing_build=true
fi

if [ -z "${GRADLE_USER_HOME:-}" ]; then
    if [ "$signing_build" = true ] && [ -n "${CI_WORKSPACE:-}" ]; then
        GRADLE_USER_HOME="$CI_WORKSPACE/.gradle-release"
        rm -rf "$GRADLE_USER_HOME"
        # Dependencies still come from the shared cache, but Gradle only reads it.
        if [ -d "$shared_gradle_home/caches/modules-2" ]; then
            GRADLE_RO_DEP_CACHE="$shared_gradle_home/caches"
            export GRADLE_RO_DEP_CACHE
        fi
    elif [ -d "$shared_gradle_home" ]; then
        GRADLE_USER_HOME="$shared_gradle_home"
    elif [ -n "${CI_WORKSPACE:-}" ]; then
        GRADLE_USER_HOME="$CI_WORKSPACE/.gradle-ci"
    fi
fi

if [ -n "${GRADLE_USER_HOME:-}" ]; then
    export GRADLE_USER_HOME
    mkdir -p "$GRADLE_USER_HOME"
fi

"$JAVA_HOME/bin/java" -version

# The runner-mounted cache is shared by every pipeline container, but each
# container has its own PID namespace, so Gradle's lock-liveness check cannot
# tell a concurrent holder from a stale lock (identical images even produce
# identical PIDs — "Owner PID: 79, Our PID: 79"). Serialize builds with an
# advisory flock on the shared volume instead; once the exclusive lock is
# held, any Gradle lock file still on disk is provably stale (a killed run's
# leftover) and safe to sweep.
if [ "${GRADLE_USER_HOME:-}" = "$shared_gradle_home" ] && command -v flock >/dev/null 2>&1; then
    exec 9>"$GRADLE_USER_HOME/.ci-build.flock"
    if ! flock -w 2700 9; then
        echo "Timed out after 45m waiting for another pipeline's Gradle build on the shared cache." >&2
        exit 1
    fi
    find "$GRADLE_USER_HOME/caches" -name '*.lock' -type f -delete 2>/dev/null || true
elif [ -n "${GRADLE_RO_DEP_CACHE:-}" ] && command -v flock >/dev/null 2>&1; then
    # Reading the shared cache while another build writes it is not safe either. Same
    # lock, no sweep: this build does not own that cache.
    exec 9>"$shared_gradle_home/.ci-build.flock"
    if ! flock -w 2700 9; then
        echo "Timed out after 45m waiting for another pipeline's Gradle build on the shared cache." >&2
        exit 1
    fi
fi

# fd 9 stays open across exec, so the flock is held for Gradle's lifetime and
# released by the kernel when the process exits, however it exits.
#
# The runner has 32 GB. The build JVM gets 8 GB (lint runs inside it) and the
# Kotlin compiler 6 GB; test forks take 1 GB each, at most four. About 18 GB.
# Developer machines keep the smaller sizes in gradle.properties. Through
# GRADLE_OPTS, not -D: the single-use daemon splits a -D value at its spaces.
GRADLE_OPTS="${GRADLE_OPTS:-} -Dorg.gradle.jvmargs=\"-Xmx8g -XX:MaxMetaspaceSize=1024m -Dfile.encoding=UTF-8\""
export GRADLE_OPTS
exec ./gradlew --no-daemon -Pkotlin.daemon.jvmargs=-Xmx6g "$@"
