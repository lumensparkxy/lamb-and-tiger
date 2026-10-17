#!/usr/bin/env bash
# Keep the original test result while exposing reports that AGP omits from stdout.
set -uo pipefail
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
  echo '::group::Device crash diagnostics'
  adb logcat -b crash -d -t 300 || true
  echo '::endgroup::'
fi
exit "$test_status"
