"""Reject failures, unexplained skips and absent JVM/Compose test suites."""
from pathlib import Path
import xml.etree.ElementTree as ET

reports = list(Path('.').glob('**/build/test-results/**/TEST-*.xml'))
total = 0
compose = 0
for path in reports:
    suite = ET.parse(path).getroot()
    if any(int(suite.get(key, 0)) for key in ('failures', 'errors', 'skipped')):
        raise SystemExit(f'Test failures/errors/skips: {path}')
    count = int(suite.get('tests', 0))
    total += count
    if suite.get('name', '').endswith('ConnectionInteractionTest'):
        compose += count
if total < 486 or compose < 2:
    raise SystemExit(f'Missing baseline/Compose suites: total={total}, Compose={compose}')
print(f'JVM tests: {total}; Compose interactions: {compose}; no failures/errors/skips')
