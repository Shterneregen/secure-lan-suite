"""Promote only immutable artifacts from a successful, trusted candidate run."""
import json
import os
from pathlib import Path
import re
import subprocess

from release import run, sha256, verify_qualification, verify_apk
from types import SimpleNamespace


repo = os.environ['GITHUB_REPOSITORY']
commit = os.environ['GITHUB_SHA']
run_id = os.environ['CANDIDATE_RUN_ID']
if not run_id.isdecimal():
    raise SystemExit('Candidate run ID must be numeric')
metadata = json.loads(run('gh', 'api', f'repos/{repo}/actions/runs/{run_id}'))
if (metadata.get('conclusion') != 'success' or metadata.get('event') != 'workflow_dispatch'
        or metadata.get('head_sha') != commit or metadata.get('head_branch') != 'master'
        or metadata.get('path') != '.github/workflows/release-candidate.yml'
        or metadata.get('head_repository', {}).get('full_name') != repo):
    raise SystemExit('Candidate must come from the successful release workflow on this exact master commit')
run('gh', 'run', 'download', run_id, '--repo', repo, '--name', 'release-candidate', '--dir', 'candidate')
directory = Path('candidate')
manifest = json.loads((directory / 'manifest.json').read_text(encoding='utf-8-sig'))
artifacts = {path.name: sha256(path) for path in directory.iterdir() if path.suffix in ('.zip', '.exe', '.apk')}
if manifest['commit'] != commit or manifest['artifacts'] != artifacts:
    raise SystemExit('Candidate manifest does not match the downloaded bytes/commit')
qualification = json.loads(os.environ['QUALIFICATION_JSON'])
verify_qualification(qualification, artifacts, commit)
policy = json.loads((directory / 'windows-signing.json').read_text(encoding='utf-8-sig'))
if qualification['windowsSigning'] != policy['policy']:
    raise SystemExit('Qualification changed the Windows signing policy')
if policy['policy'] == 'authenticode':
    installer = next(directory.glob('*.exe'))
    # The file path is supplied through the environment, not interpolated as shell code.
    os.environ['SECURELAN_INSTALLER'] = str(installer.resolve())
    status = run('powershell', '-NoProfile', '-Command',
                 '$s = Get-AuthenticodeSignature -LiteralPath $env:SECURELAN_INSTALLER; if ($s.Status -ne "Valid" -or !$s.TimeStamperCertificate) { exit 1 }; Write-Output Valid')
apk = next(directory.glob('*.apk'))
parts = manifest['version'].split('.')
version_code = qualification.get('androidVersionCode', int(parts[0]) * 1000000 + int(parts[1]) * 1000 + int(parts[2]))
verify_apk(SimpleNamespace(apk=str(apk), sdk=os.environ['ANDROID_HOME'], version=manifest['version'], version_code=version_code, output=None))
(directory / 'qualification.json').write_text(json.dumps(qualification, indent=2) + '\n', encoding='utf-8')
notes = 'Qualified release from commit ' + commit + '.\n'
if policy['policy'] == 'unsigned-pilot':
    notes += '\nWindows installer is an unsigned pilot: ' + policy['reason'] + '\n'
(directory / 'release-notes.md').write_text(notes, encoding='utf-8')
# A maintainer must create the version tag on this commit before promotion.
tag = 'v' + manifest['version']
tag_commit = run('gh', 'api', f'repos/{repo}/commits/{tag}', '--jq', '.sha').strip()
if tag_commit != commit:
    raise SystemExit('Release tag does not point to the qualified commit')
subprocess.run(['gh', 'release', 'create', tag, '--repo', repo, '--verify-tag', '--target', commit,
                '--notes-file', str(directory / 'release-notes.md')] +
               [str(path) for path in sorted(directory.iterdir()) if path.name != 'release-notes.md'], check=True)
