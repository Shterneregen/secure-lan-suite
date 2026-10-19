"""Record the actual packaging toolchain without recording environment secrets."""
import argparse
import json
import os
from pathlib import Path
import platform
import re
import shutil
import subprocess


def jdk(path):
    properties = dict(re.findall(r'^([A-Z_]+)="([^"]*)"$', (path / 'release').read_text(), re.M))
    return {key: properties.get(key) for key in ('IMPLEMENTOR', 'JAVA_VERSION', 'JAVA_RUNTIME_VERSION', 'OS_ARCH')}


def wix_metadata(required=False):
    executable = shutil.which('wix')
    if executable is None:
        if required:
            raise SystemExit('Release WiX was not found in PATH')
        return {'wix': None, 'wixExtensions': []}
    wix = subprocess.run([executable, '--version'], capture_output=True, text=True)
    extensions = subprocess.run([executable, 'extension', 'list', '--global'], capture_output=True, text=True)
    if wix.returncode or extensions.returncode:
        raise SystemExit('WiX version/extensions could not be recorded')
    return {'wix': wix.stdout.strip(), 'wixExtensions': extensions.stdout.strip().splitlines()}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('jdk17', 'jdk25', 'output'):
        parser.add_argument('--' + name, type=Path, required=True)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    settings = (root / 'settings.gradle').read_text()
    result = {'java17': jdk(args.jdk17), 'java25': jdk(args.jdk25),
              'os': platform.platform(), 'architecture': platform.machine(), 'python': platform.python_version(),
              'gradle': '9.4.1', 'kotlin': re.search(r"id 'org.jetbrains.kotlin.jvm' version '([^']+)'", settings)[1],
              'agp': re.search(r"id 'com.android.application' version '([^']+)'", settings)[1],
              'compose': re.search(r"id 'org.jetbrains.compose' version '([^']+)'", settings)[1],
              'compileSdk': 35, 'buildTools': '36.0.0'}
    verify_release = os.environ.get('SECURELAN_VERIFY_RELEASE_TOOLCHAIN') == 'true'
    if os.name == 'nt':
        result.update(wix_metadata(required=verify_release))
    if verify_release:
        for name, expected in (('java17', '17.0.20.1+1'), ('java25', '25.0.4.1+1')):
            if result[name]['JAVA_RUNTIME_VERSION'] not in (expected, expected + '-LTS') or 'Eclipse Adoptium' not in (result[name]['IMPLEMENTOR'] or ''):
                raise SystemExit(f'Release {name} differs from the pinned Temurin baseline')
        if os.name == 'nt' and (not result['wix'].startswith('5.0.2+') or any(
                not any(extension in line and '5.0.2' in line for line in result['wixExtensions'])
                for extension in ('WixToolset.UI.wixext', 'WixToolset.Util.wixext'))):
            raise SystemExit('Release WiX/extensions differ from the pinned 5.0.2 baseline')
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')


if __name__ == '__main__':
    main()
