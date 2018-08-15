#!/bin/sh
if [ "$ANDROID" = 1 ]; then
  MAYBE_ANDROID_FLAG="-Pandroid"
else
  MAYBE_ANDROID_FLAG=""
fi
cd betterrandom
NO_GIT_PATH="${PATH}"
if [ "${APPVEYOR}" != "" ]; then
  JAVA_OPTS=""
  export RANDOM_DOT_ORG_KEY=$(powershell 'Write-Host ($env:random_dot_org_key) -NoNewLine')
  if [ "${OSTYPE}" = "cygwin" ]; then
    # Workaround for a faulty PATH in Appveyor Cygwin (https://github.com/appveyor/ci/issues/1956)
    NO_GIT_PATH=$(echo "${PATH}" | /usr/bin/awk -v RS=':' -v ORS=':' '/git/ {next} {print}')
  fi
else
  JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom"
fi
cd betterrandom
PATH="${NO_GIT_PATH}" mvn -DskipTests -Darguments=-DskipTests\
    -Dmaven.test.skip=true ${MAYBE_ANDROID_FLAG}\
    clean install &&\
cd ../benchmark &&\
PATH="${NO_GIT_PATH}" mvn -DskipTests ${MAYBE_ANDROID_FLAG} package &&\
cd target &&\
if [ "$TRAVIS" = "true" ]; then
  java ${JAVA_OPTS} -jar benchmarks.jar -f 1 -t 1 -foe true &&\
  java ${JAVA_OPTS} -jar benchmarks.jar -f 1 -t 2 -foe true
else
  java ${JAVA_OPTS} -jar benchmarks.jar -f 1 -t 1 -foe true -v EXTRA 2>&1 |\
      /usr/bin/tee benchmark_results_one_thread.txt &&\
  java ${JAVA_OPTS} -jar benchmarks.jar -f 1 -t 2 -foe true -v EXTRA 2>&1 |\
      /usr/bin/tee benchmark_results_two_threads.txt
fi && cd ../..
