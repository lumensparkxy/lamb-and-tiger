#!/usr/bin/env python3
"""Build and retain local Play preparation evidence; never sign or publish.

Run from this checkout with JAVA_HOME and ANDROID_HOME configured as in README.
Device tests are intentionally separate: their device must be exclusively available.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
from datetime import datetime, timezone
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def source_identity():
    # Include dirty and untracked build inputs; omit diagnostic harness and outputs.
    paths = subprocess.check_output([
        'git', 'ls-files', '-z', '--cached', '--others', '--exclude-standard', '--',
        'app', 'game', 'nearby', 'gradle', 'gradlew', 'gradlew.bat',
        '*.gradle.kts', '*.properties', '*lockfile', 'scripts',
    ], cwd=ROOT).decode().split('\0')
    files = {p: digest(ROOT / p) if (ROOT / p).is_file() else 'MISSING'
             for p in sorted(set(paths)) if p}
    return {'files': files, 'sha256': hashlib.sha256(
        json.dumps(files, sort_keys=True).encode()).hexdigest()}


def run(command, log):
    with log.open('w') as stream:
        result = subprocess.run(command, cwd=ROOT, stdout=stream,
                                stderr=subprocess.STDOUT, check=False)
    if result.returncode:
        raise RuntimeError(f'Command failed ({result.returncode}); inspect {log}')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--live-ads', action='store_true', help='Build the unsigned candidate with owned AdMob IDs; does not publish')
    args = parser.parse_args()
    for name in ('JAVA_HOME', 'ANDROID_HOME'):
        if not os.environ.get(name):
            raise RuntimeError(f'Set {name} as documented in README')
    stamp = datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S.%fZ')
    out = ROOT / 'artifacts/local' / f'play-preparation-{stamp}'
    out.mkdir(parents=True, exist_ok=False)
    print(f'Evidence: {out}', flush=True)
    before = source_identity()
    (out / 'source-before.json').write_text(json.dumps(before, indent=2) + '\n')
    head = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT).decode().strip()
    run(['git', 'status', '--short', '--branch'], out / 'git-status.txt')
    commands = [
        ['./scripts/doctor.sh'],
        ['./gradlew', ':game:test', ':app:assembleDebug', ':app:testDebugUnitTest',
         ':app:lintDebug', ':app:lintRelease', ':nearby:testDebugUnitTest',
         ':nearby:lintRelease', ':app:bundleRelease', 'printBuildLocations', '--console=plain'],
    ]
    commands[1].append('-Plamb.liveAds=' + str(args.live_ads).lower())
    (out / 'commands.json').write_text(json.dumps(commands, indent=2) + '\n')
    run(commands[0], out / 'doctor.log')
    run(commands[1], out / 'build.log')
    build_log = (out / 'build.log').read_text()
    locations = dict(re.findall(r'^:(app|game|nearby): (.+)$', build_log, re.M))
    if set(locations) != {'app', 'game', 'nearby'}:
        raise RuntimeError('Cannot resolve checkout-specific build locations')
    app = Path(locations['app'])
    bundle = app / 'outputs/bundle/release/app-release.aab'
    shutil.copy2(bundle, out / 'app-release.aab')
    with zipfile.ZipFile(out / 'app-release.aab') as archive:
        if archive.testzip():
            raise RuntimeError('Bundle ZIP integrity failed')
        native = [n for n in archive.namelist() if n.endswith('.so')]
        signatures = [n for n in archive.namelist()
                      if re.match(r'META-INF/[^/]+\.(RSA|DSA|EC)$', n, re.I)]
    manifest = app / 'intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml'
    shutil.copy2(manifest, out / 'merged-release-manifest.xml')
    tree = ET.parse(manifest).getroot()
    ns = '{http://schemas.android.com/apk/res/android}'
    sdk, application = tree.find('uses-sdk'), tree.find('application')
    permissions = [n.get(ns + 'name') for n in tree.findall('uses-permission')]
    blocked = {'com.google.android.gms.permission.AD_ID',
               'android.permission.ACCESS_ADSERVICES_AD_ID',
               'android.permission.ACCESS_ADSERVICES_ATTRIBUTION',
               'android.permission.ACCESS_ADSERVICES_TOPICS'}
    if blocked.intersection(permissions):
        raise RuntimeError('Child-treatment release unexpectedly includes advertising/AdServices permissions')
    build_config = app / 'generated/source/buildConfig/release/com/maswadkar/lambandtiger/BuildConfig.java'
    config_text = build_config.read_text()
    ads_enabled = 'ADS_ENABLED = true;' in config_text
    if ads_enabled != args.live_ads:
        raise RuntimeError('Ad-serving configuration does not match requested candidate')
    shutil.copy2(build_config, out / 'ReleaseBuildConfig.java')
    if tree.get('package') != 'com.maswadkar.lambandtiger':
        raise RuntimeError('Unexpected release package')
    if any(application.get(ns + flag) == 'true' for flag in ('debuggable', 'testOnly')):
        raise RuntimeError('Release manifest has development flags')
    tests, lint = {}, {}
    for module, directory in locations.items():
        directory = Path(directory)
        test_dir = directory / 'test-results' / ('test' if module == 'game' else 'testDebugUnitTest')
        reports = list(test_dir.glob('TEST-*.xml'))
        if not reports:
            raise RuntimeError(f'No JVM test reports for {module}')
        shutil.copytree(test_dir, out / f'{module}-unit-results')
        tests[module] = {k: sum(int(ET.parse(p).getroot().get(k, 0)) for p in reports)
                         for k in ('tests', 'failures', 'errors', 'skipped')}
        if not tests[module]['tests'] or tests[module]['failures'] or tests[module]['errors'] or tests[module]['skipped']:
            raise RuntimeError(f'Incomplete JVM verification: {module}')
        if module != 'game':
            report = directory / 'reports/lint-results-release.xml'
            shutil.copy2(report, out / f'{module}-lint-release.xml')
            issues = ET.parse(report).getroot().findall('issue')
            lint[module] = {level: sum(i.get('severity') == level for i in issues)
                            for level in ('Fatal', 'Error', 'Warning')}
            if lint[module]['Fatal'] or lint[module]['Error']:
                raise RuntimeError(f'Release lint failed: {module}')
    after = source_identity()
    (out / 'source-after.json').write_text(json.dumps(after, indent=2) + '\n')
    if before != after:
        raise RuntimeError('Build inputs changed during preparation; rerun on a stable candidate')
    report = {
        'prepared_at_utc': stamp, 'head': head, 'source_sha256': before['sha256'],
        'artifact_sha256': digest(out / 'app-release.aab'),
        'package': tree.get('package'), 'version_code': tree.get(ns + 'versionCode'),
        'version_name': tree.get(ns + 'versionName'),
        'min_sdk': sdk.get(ns + 'minSdkVersion'), 'target_sdk': sdk.get(ns + 'targetSdkVersion'),
        'signing': 'signature entries present; verify certificate separately' if signatures else 'unsigned',
        'native_libraries': native, 'unit_tests': tests, 'release_lint': lint,
        'live_ads_enabled': ads_enabled, 'permissions': permissions,
        'publication_ready': False,
        'not_certified': ['signing identity', 'bundletool validation', 'Play version availability',
                          'installed release/upgrade', '16 KB runtime and APK alignment',
                          'emulator and physical acceptance', 'store materials and policy'],
    }
    (out / 'report.json').write_text(json.dumps(report, indent=2) + '\n')
    print(json.dumps(report, indent=2))
    print('LOCAL PREPARATION ONLY. See docs/release/production-readiness.md for remaining gates.')


if __name__ == '__main__':
    try:
        main()
    except (RuntimeError, OSError, subprocess.SubprocessError, ET.ParseError, zipfile.BadZipFile) as error:
        print(f'Preparation stopped: {error}', file=sys.stderr)
        sys.exit(1)
