"""Verify archive notices/permissions and start its bundled GUI on the host."""
import argparse
from contextlib import contextmanager
import os
from pathlib import Path
import platform
import signal
import subprocess
import tempfile
import time
import zipfile

from release import verify_portable


@contextmanager
def package_directory():
    temporary = tempfile.TemporaryDirectory(prefix='securelan-package-smoke-')
    try:
        yield temporary.name
    finally:
        # Windows can retain mapped JAR handles briefly after the launcher tree exits.
        for attempt in range(20):
            try:
                temporary.cleanup()
                break
            except PermissionError:
                if attempt == 19:
                    raise
                time.sleep(0.5)


system = {'Windows': 'windows', 'Linux': 'linux', 'Darwin': 'macos'}[platform.system()]
architecture = 'aarch64' if platform.machine().lower() in ('aarch64', 'arm64') else 'x86_64'
target = system + '-' + architecture
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--proguard', action='store_true')
args = parser.parse_args()
suffix = 'proguard-portable' if args.proguard else 'portable'
archives = list(Path('apps/desktop-client/build/distributions').glob(f'SecureLanSuite-*-{target}-{suffix}.zip'))
if len(archives) != 1:
    raise SystemExit('Expected exactly one portable from this build in a fresh checkout')
archive = archives[0].resolve()
verify_portable(archive, target)
report = Path('build/reports/packaged-smoke') / f'{target}-{suffix}.log'
report.parent.mkdir(parents=True, exist_ok=True)
with package_directory() as directory:
    root = Path(directory)
    if os.name == 'nt':
        with zipfile.ZipFile(archive) as zipped:
            zipped.extractall(root)
        launcher = root / 'SecureLanSuite.exe'
    else:
        subprocess.run(['unzip', '-q', str(archive), '-d', directory], check=True)
        launcher = root / ('SecureLanSuite.app/Contents/MacOS/SecureLanSuite' if system == 'macos' else 'bin/SecureLanSuite')
    environment = os.environ.copy()
    environment.pop('JAVA_HOME', None)
    environment.pop('JDK_HOME', None)
    environment['PATH'] = os.pathsep.join(entry for entry in environment.get('PATH', '').split(os.pathsep)
                                         if not any(word in entry.lower() for word in ('java', 'jdk', 'temurin')))
    command = [str(launcher)]
    if system == 'linux' and not environment.get('DISPLAY'):
        command = ['xvfb-run', '-a'] + command
    with report.open('w') as log:
        process = subprocess.Popen(command, cwd=root, env=environment, stdout=log, stderr=log,
                                   start_new_session=os.name != 'nt')
        try:
            time.sleep(10)
            if process.poll() is not None:
                raise SystemExit(f'Packaged application exited during startup: {process.returncode}; see {report}')
        finally:
            if os.name == 'nt':
                subprocess.run(['taskkill', '/PID', str(process.pid), '/T', '/F'], capture_output=True)
            else:
                try:
                    os.killpg(process.pid, signal.SIGTERM)
                except ProcessLookupError:
                    pass
            try:
                process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait(timeout=10)
print(f'Portable {target}: notices, permissions and bundled GUI startup passed')
