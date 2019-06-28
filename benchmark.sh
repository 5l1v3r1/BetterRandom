#!/bin/sh
sudo renice -10 $$
if [ "${ANDROID}" = "true" ]; then
  MAYBE_ANDROID_FLAG="-Pandroid"
else
  MAYBE_ANDROID_FLAG=""
fi
JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom"
cd betterrandom
mvn -B -DskipTests -Darguments=-DskipTests\
    -Dmaven.test.skip=true ${MAYBE_ANDROID_FLAG}\
    clean install &&\
cd ../benchmark &&\
mvn -DskipTests ${MAYBE_ANDROID_FLAG} package &&\
cd target &&\
java ${JAVA_OPTS} -jar benchmarks.jar $@ -jvm $(JAVA_HOME)/bin/java  -f 1 -t 1 -foe true -v EXTRA 2>&1 |\
    tee benchmark_results_one_thread.txt &&\
java ${JAVA_OPTS} -jar benchmarks.jar $@ -jvm $(JAVA_HOME)/bin/java -f 1 -t 2 -foe true -v EXTRA 2>&1 |\
    tee benchmark_results_two_threads.txt &&\
cd ../..
