Set-PSDebug -Trace 2
if ($env:ANDROID = 1)
{
    $MAYBE_ANDROID_FLAG = "-Pandroid"
}
else
{
    $MAYBE_ANDROID_FLAG = ""
}
if ( $env:APPVEYOR )
{
    $RANDOM_DOT_ORG_KEY = $env:random_dot_org_key
}
$MAYBE_PROGUARD="pre-integration-test"
cd betterrandom
mvn "-DskipTests" "-Darguments=-DskipTests" "-Dmaven.test.skip=true" "$MAYBE_ANDROID_FLAG" `
    "clean" "package" "$MAYBE_PROGUARD" install
cd ../benchmark
mvn "-DskipTests" "$MAYBE_ANDROID_FLAG" package
cd target
if ( $TRAVIS ) {
  java -jar benchmarks.jar -f 1 -t 1 -foe true
  java -jar benchmarks.jar -f 1 -t 2 -foe true
} else {
  java -jar benchmarks.jar -f 1 -t 1 -foe true -v EXTRA 2>&1 | `
      Tee-Object benchmark_results_one_thread.txt
  java -jar benchmarks.jar -f 1 -t 2 -foe true -v EXTRA 2>&1 | `
      Tee-Object benchmark_results_two_threads.txt
}
