#!/bin/sh
if [ "$ANDROID" = 1 ]; then
  MAYBE_ANDROID_FLAG="-Pandroid"
else
  MAYBE_ANDROID_FLAG=""
fi
if ([ "{$TRAVIS_JDK_VERSION}" = "oraclejdk9" ] || [ "${TRAVIS_JDK_VERSION}" = "openjdk9" ]); then
  JAVA9="true"
  mv pom9.xml pom.xml
fi
NO_GIT_PATH="${PATH}"
if [ "${APPVEYOR}" != "" ]; then
  RANDOM_DOT_ORG_KEY=$(powershell 'Write-Host ($env:random_dot_org_key) -NoNewLine')
  if [ "${OSTYPE}" = "cygwin" ]; then
    # Workaround for a faulty PATH in Appveyor Cygwin (https://github.com/appveyor/ci/issues/1956)
    NO_GIT_PATH=$(echo "${PATH}" | /usr/bin/awk -v RS=':' -v ORS=':' '/git/ {next} {print}')
  fi
else
  sudo apt-get install tor
fi
cd betterrandom
# Coverage test
PATH="${NO_GIT_PATH}" mvn ${MAYBE_ANDROID_FLAG} clean jacoco:prepare-agent test jacoco:report -e
STATUS=$?
if [ "$STATUS" = 0 ]; then
  if [ "$TRAVIS" = "true" ]; then
    if [ "$JAVA9" != "true" ]; then
      # Coveralls doesn't seem to work in non-.NET Appveyor yet
      # so we have to hope Appveyor pushes its Jacoco reports before Travis does! :(
      mvn coveralls:report

      # Send coverage to Codacy
      wget 'https://github.com/codacy/codacy-coverage-reporter/releases/download/2.0.0/codacy-coverage-reporter-2.0.0-assembly.jar'
      java -jar codacy-coverage-reporter-2.0.0-assembly.jar -l Java -r target/site/jacoco/jacoco.xml

      # Send coverage to Codecov
      curl -s https://codecov.io/bash | bash
    fi
    COMMIT="$TRAVIS_COMMIT"
    JOB_ID="travis_$TRAVIS_JOB_NUMBER"
    git config --global user.email "travis@travis-ci.org"
  elif [ "$APPVEYOR" != "" ]; then
    GH_TOKEN=$(powershell 'Write-Host ($env:access_token) -NoNewLine')
    COMMIT="$APPVEYOR_REPO_COMMIT"
    JOB_ID="appveyor_$APPVEYOR_BUILD_ID"
    git config --global user.email "appveyor@appveyor.com"
  else
    PUSH_JACOCO="false"
  fi
  git clone https://github.com/Pr0methean/betterrandom-coverage.git
  cd betterrandom-coverage
  /bin/mkdir -p "$COMMIT"
  /bin/mv ../target/jacoco.exec "$COMMIT/$JOB_ID.exec"
  cd "$COMMIT"
  git add .
  git commit -m "Coverage report from job $JOB_ID"
  git remote add originauth "https://${GH_TOKEN}@github.com/Pr0methean/betterrandom-coverage.git"
  git push --set-upstream originauth master
  while [ ! $? ]; do
    git pull --rebase  # Merge
    git push
  done
  cd ../..
  if [ "$JAVA9" = "true" ]; then
    PATH="${NO_GIT_PATH}" mvn -DskipTests -Dmaven.test.skip=true ${MAYBE_ANDROID_FLAG} jacoco:report-aggregate package && (
      # Post-Proguard test (verifies Proguard settings)
      PATH="${NO_GIT_PATH}" mvn ${MAYBE_ANDROID_FLAG} test -e
    )
    STATUS=$?
  fi
fi
cd ..
exit "$STATUS"
