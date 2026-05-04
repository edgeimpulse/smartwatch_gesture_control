FROM ubuntu:22.04

ENV DEBIAN_FRONTEND=noninteractive
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${PATH}

# ── System deps ───────────────────────────────────────────────────────────────
RUN apt-get update -qq && apt-get install -y --no-install-recommends \
    openjdk-17-jdk-headless \
    wget \
    unzip \
    git \
    && rm -rf /var/lib/apt/lists/*

# ── Android cmdline-tools (pinned build 11076708 = tools v12.0) ───────────────
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip \
         -O /tmp/cmdtools.zip && \
    unzip -q /tmp/cmdtools.zip -d /tmp/cmdtools && \
    mv /tmp/cmdtools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest && \
    rm -rf /tmp/cmdtools /tmp/cmdtools.zip

# ── SDK components (exact versions matching local build) ──────────────────────
# NDK 28.2.13676358 = r28b, confirmed from configure_command.bat
# CMake 3.22.1 = from app/CMakeLists.txt cmake_minimum_required
# build-tools 35.0.0 = highest stable when targeting API 36
RUN yes | sdkmanager --licenses > /dev/null 2>&1 && \
    sdkmanager \
        "platforms;android-36" \
        "build-tools;35.0.0" \
        "ndk;28.2.13676358" \
        "cmake;3.22.1"

# ── App source (ei-deployment injected at runtime; local.properties excluded) ─
WORKDIR /app
COPY . .
RUN rm -rf app/src/main/cpp/ei-deployment && chmod +x gradlew

# ── Pre-warm Gradle 9.3.1 + resolve library dependencies ─────────────────────
# Native compilation needs ei-deployment, so we skip :app:externalNativeBuild.
# This still downloads Gradle itself, AGP 9.1.0, Kotlin 2.2.10, Compose BOM.
RUN ./gradlew :app:dependencies --no-daemon --quiet 2>&1 | tail -3 || true

ENTRYPOINT ["deployment-block/build.sh"]
