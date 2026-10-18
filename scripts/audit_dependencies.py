"""Query OSV for resolved Maven coordinates, preserving runtime/build/test scope."""
from datetime import date
import json
from pathlib import Path
import urllib.request


scopes = {}
for path in Path('.').glob('**/build/reports/dependencies/*.json'):
    if path.stem not in ('windows-x86_64', 'linux-x86_64', 'linux-aarch64', 'macos-x86_64', 'macos-aarch64'):
        continue
    for row in json.loads(path.read_text(encoding='utf-8')):
        coordinate = row['coordinate']
        scope = ('build' if row['scope'] in ('kotlinLint', 'buildscript') else
                 'test' if 'test' in row['scope'].lower() else
                 'runtime' if 'runtime' in row['scope'].lower() else 'compile')
        scopes.setdefault(coordinate, set()).add(scope)
if not scopes:
    raise SystemExit('Resolve the dependency inventory before the advisory check')
coordinates = sorted(scopes)
findings = []
for offset in range(0, len(coordinates), 500):
    batch = coordinates[offset:offset + 500]
    queries = []
    for coordinate in batch:
        group, name, version = coordinate.split(':')
        queries.append({'package': {'ecosystem': 'Maven', 'name': group + ':' + name}, 'version': version})
    request = urllib.request.Request('https://api.osv.dev/v1/querybatch', data=json.dumps({'queries': queries}).encode(),
                                     headers={'Content-Type': 'application/json'})
    with urllib.request.urlopen(request, timeout=60) as response:
        results = json.load(response)['results']
    if len(results) != len(batch):
        raise SystemExit('OSV returned an incomplete result')
    for coordinate, result in zip(batch, results):
        if not isinstance(result, dict) or 'error' in result:
            raise SystemExit('OSV could not evaluate a coordinate; refusing an incomplete scan')
        for vulnerability in result.get('vulns', []):
            findings.append({'coordinate': coordinate, 'id': vulnerability['id'], 'scopes': sorted(scopes[coordinate])})
policy = json.loads(Path('gradle/advisory-exceptions.json').read_text())
unresolved = []
for finding in findings:
    exception = next((entry for entry in policy if entry['coordinate'] == finding['coordinate'] and entry['id'] == finding['id']), None)
    if (not exception or date.fromisoformat(exception['reviewBy']) < date.today()
            or not all(exception.get(key) for key in ('owner', 'reachability', 'reason'))):
        unresolved.append(finding)
report = Path('build/reports/dependencies/advisories.json')
report.parent.mkdir(parents=True, exist_ok=True)
report.write_text(json.dumps({'findings': findings, 'unresolved': unresolved}, indent=2) + '\n', encoding='utf-8')
print(f'OSV: {len(coordinates)} resolved coordinates; {len(findings)} findings; {len(unresolved)} unresolved')
if unresolved:
    raise SystemExit('Untriaged or expired dependency advisories block release; see advisories.json')
