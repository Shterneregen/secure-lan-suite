"""Build an offline notice bundle from checksum-verified runtime artifacts and POMs."""
import argparse
import hashlib
import io
import json
from pathlib import Path
import re
import shutil
import xml.etree.ElementTree as ET
import zipfile


NS = {'m': 'http://maven.apache.org/POM/4.0.0'}


def licenses(cache, coordinate, visited=None):
    visited = set() if visited is None else visited
    if coordinate in visited:
        return [], []
    visited.add(coordinate)
    group, name, version = coordinate.split(':')
    poms = sorted((cache / group / name / version).glob('*/*.pom'))
    if not poms:
        return [], []
    root = ET.parse(poms[0]).getroot()
    result = [{'name': x.findtext('m:name', namespaces=NS), 'url': x.findtext('m:url', namespaces=NS)}
              for x in root.findall('m:licenses/m:license', NS)]
    parents = []
    parent = root.find('m:parent', NS)
    if not result and parent is not None:
        parent_id = ':'.join(parent.findtext('m:' + key, namespaces=NS) for key in ('groupId', 'artifactId', 'version'))
        result, parents = licenses(cache, parent_id, visited)
    return result, poms[:1] + parents


def embedded_notices(archive, output, prefix=''):
    found = []
    with zipfile.ZipFile(archive) as jar:
        for entry in jar.infolist():
            name = entry.filename
            if entry.is_dir():
                continue
            if re.search(r'(?i)(license|notice|copyright|copying)', name) and not name.lower().endswith(('.class', '.so', '.dll', '.dylib')):
                target = output / (prefix + re.sub(r'[^A-Za-z0-9._-]', '_', name))
                target.write_bytes(jar.read(entry))
                found.append(target.name)
            elif name == 'classes.jar':
                found += embedded_notices(io.BytesIO(jar.read(entry)), output, 'classes_')
    return found


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('input', 'cache', 'sources', 'output'):
        parser.add_argument('--' + name, type=Path, required=True)
    args = parser.parse_args()
    # Only remove our declared, fixed build output; never consume artifact paths as deletion targets.
    repository = Path(__file__).resolve().parents[1]
    allowed = {repository / 'apps' / app / 'build/generated/third-party' for app in ('desktop-client', 'android-client')}
    if args.output.resolve() not in allowed:
        parser.error('Output must be the generated/third-party build directory')
    if args.output.exists():
        shutil.rmtree(args.output)
    bundle = args.output / 'third-party'
    shutil.copytree(args.sources, bundle)
    inventory = []
    for artifact in json.loads(args.input.read_text(encoding='utf-8')):
        coordinate = artifact['coordinate']
        target = bundle / 'components' / (coordinate.replace(':', '_') + ('_' + artifact['classifier'] if artifact.get('classifier') else ''))
        target.mkdir(parents=True, exist_ok=True)
        declared, poms = licenses(args.cache, coordinate)
        for pom in poms:
            shutil.copy2(pom, target / pom.name)
        source = Path(artifact['file'])
        notices = embedded_notices(source, target) if zipfile.is_zipfile(source) else []
        if coordinate.startswith('org.bouncycastle:'):
            declared = [{'name': 'Bouncy Castle licence (MIT)', 'url': 'https://www.bouncycastle.org/licence.html'}]
        if not declared:
            raise ValueError(f'License metadata missing for {coordinate}; review the upstream before distribution')
        with source.open('rb') as stream:
            digest = hashlib.file_digest(stream, 'sha256').hexdigest()
        inventory.append({'coordinate': coordinate, 'classifier': artifact.get('classifier'),
                          'artifact': source.name, 'sha256': digest, 'licenses': declared,
                          'embeddedNotices': notices, 'metadata': [p.name for p in poms]})
    inventory.append({'coordinate': 'vendored:material-icons', 'licenses': [{'name': 'Apache-2.0'}],
                      'source': 'apps/desktop-client/src/main/kotlin/androidx/compose/material/icons',
                      'notice': 'Copyright The Android Open Source Project; original headers retained in source.'})
    (bundle / 'inventory.json').write_text(json.dumps(sorted(inventory, key=lambda x: x['coordinate']), indent=2) + '\n', encoding='utf-8')
    print(f'Collected notices for {len(inventory)} runtime/vendored components')


if __name__ == '__main__':
    main()
