#!/bin/bash
set -e
# Backup and replace local.properties on the host before Docker runs
cp /home/ghost/Projects/CtoK/local.properties /tmp/local.properties.bak
printf 'sdk.dir=/opt/android-sdk\n' > /home/ghost/Projects/CtoK/local.properties

docker run --rm \
  -v /home/ghost/Projects/CtoK:/workspace:z \
  -v /home/ghost/.gradle-docker:/root/.gradle:z \
  -e JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 \
  -w /workspace \
  registry.gitlab.com/fdroid/fdroidserver:buildserver-trixie \
  bash -c "apt-get install -y -q openjdk-21-jdk-headless 2>/dev/null && ./gradlew --no-daemon -Dorg.gradle.java.home=/usr/lib/jvm/java-21-openjdk-amd64 :app:assembleFossRelease"

# Always restore local.properties
cp /tmp/local.properties.bak /home/ghost/Projects/CtoK/local.properties
ls -la /home/ghost/Projects/CtoK/app/build/outputs/apk/foss/release/
