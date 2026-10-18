import io
from pathlib import Path
import tempfile
import unittest
import zipfile

from third_party import embedded_notices


class NoticeBundleTest(unittest.TestCase):
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
