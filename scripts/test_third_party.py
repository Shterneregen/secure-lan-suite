import io
from pathlib import Path
import tempfile
import unittest
import zipfile

from third_party import embedded_notices, licenses


class NoticeBundleTest(unittest.TestCase):
    def test_licenses_use_explicit_poms_without_a_gradle_cache_layout(self):
        with tempfile.TemporaryDirectory(prefix='securelan-poms-test-') as directory:
            pom = Path(directory) / 'material3.pom'
            pom.write_text('<project xmlns="http://maven.apache.org/POM/4.0.0"><licenses><license>'
                           '<name>Apache-2.0</name><url>https://www.apache.org/licenses/LICENSE-2.0.txt</url>'
                           '</license></licenses></project>')
            declared, files = licenses({'androidx.compose.material3:material3-android:1.3.1': str(pom)},
                                       'androidx.compose.material3:material3-android:1.3.1')
            self.assertEqual('Apache-2.0', declared[0]['name'])
            self.assertEqual([pom], files)
            self.assertEqual(([], []), licenses({}, 'androidx.compose.material3:material3-android:1.3.1'))

    def test_parent_license_is_inherited_from_the_explicit_metadata_index(self):
        with tempfile.TemporaryDirectory(prefix='securelan-poms-test-') as directory:
            child, parent = Path(directory) / 'child.pom', Path(directory) / 'parent.pom'
            child.write_text('<project xmlns="http://maven.apache.org/POM/4.0.0"><parent>'
                             '<groupId>example</groupId><artifactId>parent</artifactId><version>1</version></parent></project>')
            parent.write_text('<project xmlns="http://maven.apache.org/POM/4.0.0"><licenses><license>'
                              '<name>Example license</name></license></licenses></project>')
            declared, files = licenses({'example:child:1': str(child), 'example:parent:1': str(parent)}, 'example:child:1')
            self.assertEqual([{'name': 'Example license', 'url': None}], declared)
            self.assertEqual([child, parent], files)

    def test_notice_named_classes_are_not_repackaged_as_misnamed_bytecode(self):
        data = io.BytesIO()
        with zipfile.ZipFile(data, 'w') as archive:
            archive.writestr('META-INF/LICENSE.txt', 'license text')
            archive.writestr('org/example/NoticeReference.class', b'bytecode')
            archive.writestr('org/example/LICENSE.class', b'bytecode')
        data.seek(0)
        with tempfile.TemporaryDirectory(prefix='securelan-notices-test-') as directory:
            paths = embedded_notices(data, Path(directory))
            self.assertEqual(['META-INF_LICENSE.txt'], paths)
            self.assertEqual('license text', (Path(directory) / paths[0]).read_text())


if __name__ == '__main__':
    unittest.main()
