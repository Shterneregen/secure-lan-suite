"""Fail-closed validation of built artifacts and release qualification records."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import zipfile


def sha256(path):
    with Path(path).open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def run(*command):
    result = subprocess.run([str(x) for x in command], capture_output=True, text=True)
    if result.returncode:
        # Commands contain no credentials. Do not emit arbitrary tool payloads.
        raise ValueError(f'{Path(command[0]).name} failed (exit {result.returncode})')
    return result.stdout


def validate_certificate(output, expected):
    expected = expected.replace(':', '').lower()
    if not re.fullmatch('[a-f0-9]{64}', expected):
        raise ValueError('A trusted ANDROID_RELEASE_CERT_SHA256 is required')
    fingerprints = re.findall(r'Signer #\d+ certificate SHA-256 digest: ([a-fA-F0-9]+)', output)
    if len(fingerprints) != 1 or fingerprints[0].lower() != expected:
        raise ValueError('APK certificate does not match the trusted release certificate')
    if re.search(r'certificate DN:.*(?:Android Debug|androiddebugkey)', output, re.I):
        raise ValueError('Android Debug certificate is forbidden for production')
    return expected


def verify_apk(args):
    tools = Path(args.sdk) / 'build-tools' / '36.0.0'
    signer = tools / ('apksigner.bat' if os.name == 'nt' else 'apksigner')
    cert = validate_certificate(run(signer, 'verify', '--verbose', '--print-certs', args.apk),
                                os.environ.get('ANDROID_RELEASE_CERT_SHA256', ''))
    run(tools / ('zipalign.exe' if os.name == 'nt' else 'zipalign'), '-c', '-P', '16', '4', args.apk)
    badging = run(tools / ('aapt.exe' if os.name == 'nt' else 'aapt'), 'dump', 'badging', args.apk)
    package = re.search(r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'", badging)
    if not package or package.groups() != ('com.shterneregen.securelan.androidclient', str(args.version_code), args.version):
        raise ValueError('APK application ID or version does not match this build')
    if 'application-debuggable' in badging:
        raise ValueError('Production APK is debuggable')
    with zipfile.ZipFile(args.apk) as archive:
        required = ('assets/third-party/inventory.json', 'assets/third-party/webrtc-android-NOTICES.txt')
        if any(name not in archive.namelist() for name in required):
            raise ValueError('APK is missing its runtime inventory/native notices')
    previous = os.environ.get('ANDROID_PREVIOUS_VERSION_CODE')
    if previous and args.version_code <= int(previous):
        raise ValueError('versionCode must exceed the previously published versionCode')
    verified = {'artifact': Path(args.apk).name, 'sha256': sha256(args.apk),
                'certificateSha256': cert, 'version': args.version, 'versionCode': args.version_code}
    if args.output:
        output = Path(args.output)
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(json.dumps(verified, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(verified))


def verify_portable(path, target):
    executable_count = 0
    with zipfile.ZipFile(path) as archive:
        names = archive.namelist()
        if not any('third-party/' in name for name in names):
            raise ValueError('Portable is missing third-party notices')
        if not any('/legal/' in name for name in names):
            raise ValueError('Portable is missing bundled JDK legal files')
        for entry in archive.infolist():
            if entry.is_dir():
                continue
            unix_executable = (entry.external_attr >> 16) & 0o111
            if unix_executable:
                executable_count += 1
            # Check launchers and the runtime Java executable, rather than extensions.
            if target.startswith(('linux-', 'macos-')) and (
                    entry.filename.endswith('/bin/java') or
                    entry.filename.endswith('/MacOS/SecureLanSuite') or
                    entry.filename == 'bin/SecureLanSuite') and not unix_executable:
                raise ValueError(f'Executable permission lost: {entry.filename}')
        if not target.startswith('windows-') and executable_count < 2:
            raise ValueError('Unix portable has no executable launcher/runtime')


def verify_qualification(record, artifacts, commit):
    if record.get('commit') != commit:
        raise ValueError('Qualification belongs to another commit')
    if record.get('artifacts') != artifacts or not artifacts:
        raise ValueError('Qualification artifact hashes do not match the candidate')
    required = ('desktopLaunchWithoutJava', 'hostJoinChat', 'filesBothDirections',
                'disconnectReconnect', 'voiceStartStop', 'androidUpgrade', 'windowsInstallUpgradeUninstall')
    required += ('thirdPartyNoticesReviewed',)
    checks = record.get('checks', {})
    if any(checks.get(name) != 'pass' for name in required):
        raise ValueError('Required device/install qualification is missing or failed')
    if not record.get('devices') or not record.get('tester') or not record.get('testedAt'):
        raise ValueError('Qualification must name devices, tester and date')
    if record.get('windowsSigning') not in ('authenticode', 'unsigned-pilot'):
        raise ValueError('Explicit Windows signing policy is required')
    if record['windowsSigning'] == 'unsigned-pilot' and not record.get('unsignedPilotReason'):
        raise ValueError('Unsigned pilot requires a published explanation')


def manifest(args):
    artifacts = {path.name: sha256(path) for path in sorted(Path(args.directory).iterdir())
                 if path.is_file() and path.suffix in ('.zip', '.exe', '.apk')}
    if not artifacts:
        raise ValueError('No candidate artifacts found')
    if args.qualification:
        record = json.loads(Path(args.qualification).read_text(encoding='utf-8'))
        verify_qualification(record, artifacts, args.commit)
    toolchain = json.loads(Path(args.toolchain).read_text(encoding='utf-8')) if args.toolchain else {}
    result = {'commit': args.commit, 'version': args.version, 'target': args.target,
              'artifacts': artifacts, 'toolchain': toolchain,
              'qualified': bool(args.qualification)}
    Path(args.directory, 'manifest.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    Path(args.directory, 'SHA256SUMS').write_text(''.join(f'{digest}  {name}\n' for name, digest in artifacts.items()), encoding='utf-8')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest='command', required=True)
    apk = commands.add_parser('verify-apk')
    apk.add_argument('--apk', required=True)
    apk.add_argument('--sdk', required=True)
    apk.add_argument('--version', required=True)
    apk.add_argument('--version-code', type=int, required=True)
    apk.add_argument('--output')
    portable = commands.add_parser('verify-portable')
    portable.add_argument('--zip', required=True)
    portable.add_argument('--target', required=True)
    inventory = commands.add_parser('manifest')
    for name in ('directory', 'commit', 'version', 'target'):
        inventory.add_argument('--' + name, required=True)
    inventory.add_argument('--toolchain')
    inventory.add_argument('--qualification')
    args = parser.parse_args()
    try:
        if args.command == 'verify-apk':
            verify_apk(args)
        elif args.command == 'verify-portable':
            verify_portable(args.zip, args.target)
        else:
            manifest(args)
    except (ValueError, OSError, zipfile.BadZipFile) as error:
        parser.exit(1, f'Release validation failed: {error}\n')


if __name__ == '__main__':
    main()
