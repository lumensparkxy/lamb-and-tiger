#!/usr/bin/env bash
# Keep the original test result while exposing reports that AGP omits from stdout.
set -uo pipefail
# A cold CI emulator can restart its system process while Gradle is compiling.
# Finish compilation first, then verify boot and unlock before sending UI input.
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest --console=plain || exit $?
boot_ready=false
for attempt in $(seq 1 60); do
  if [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = '1' ]; then
    boot_ready=true
    break
  fi
  sleep 1
done
if [ "$boot_ready" != true ]; then
  echo '::error::Emulator did not finish booting before instrumentation'
  exit 1
fi
adb shell input keyevent KEYCODE_WAKEUP || exit $?
adb shell wm dismiss-keyguard || exit $?
./gradlew :app:connectedDebugAndroidTest --console=plain --info
test_status=$?
python3 - <<'PY'
import os
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(os.environ.get('GRADLE_USER_HOME', str(Path.home() / '.gradle')))
for report in sorted(root.glob('lamb-and-tiger-builds/*/app/outputs/androidTest-results/connected/debug/TEST-*.xml')):
    print(f'::group::Instrumentation report: {report.name}', flush=True)
    suite = ET.parse(report).getroot()
    print(suite.attrib, flush=True)
    for case in suite.iter('testcase'):
        for kind in ('failure', 'error'):
            for failure in case.findall(kind):
                print(case.attrib, failure.attrib, failure.text or '', flush=True)
    for kind in ('failure', 'error', 'system-err'):
        for failure in suite.findall(kind):
            print(failure.attrib, failure.text or '', flush=True)
    print('::endgroup::', flush=True)
PY
if [ "$test_status" -ne 0 ]; then
  echo '::group::Device window diagnostics'
  adb shell dumpsys window policy || true
  adb shell dumpsys window windows || true
  echo '::endgroup::'
  echo '::group::Device crash diagnostics'
  adb logcat -b crash -d -t 300 || true
  echo '::endgroup::'
fi
exit "$test_status"
