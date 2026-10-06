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

"$JAVA_HOME/bin/java" -version

# GitHub-hosted runners are ephemeral, and gradle/actions/setup-gradle restores
# and saves GRADLE_USER_HOME, so there is no shared cache volume to lock or to
# keep signing builds away from. Release jobs restore it read-only.
#
# A standard Linux runner has 16 GB. The build JVM gets 6 GB (lint runs inside
# it) and the Kotlin compiler 4 GB; test forks take 1 GB each, two on four
# cores. About 12 GB. Developer machines keep the smaller sizes in
# gradle.properties. Through GRADLE_OPTS, not -D: the single-use daemon splits a
# -D value at its spaces.
GRADLE_OPTS="${GRADLE_OPTS:-} -Dorg.gradle.jvmargs=\"-Xmx6g -XX:MaxMetaspaceSize=1024m -Dfile.encoding=UTF-8\""
export GRADLE_OPTS
exec ./gradlew --no-daemon -Pkotlin.daemon.jvmargs=-Xmx4g "$@"
