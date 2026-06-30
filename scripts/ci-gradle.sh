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
exec ./gradlew --no-daemon "$@"
