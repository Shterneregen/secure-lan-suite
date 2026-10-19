import subprocess
import unittest
from unittest.mock import patch

from toolchain import wix_metadata


class ToolchainTest(unittest.TestCase):
    def test_portable_manifest_does_not_require_wix(self):
        with patch('toolchain.shutil.which', return_value=None), patch('toolchain.subprocess.run') as run:
            self.assertEqual({'wix': None, 'wixExtensions': []}, wix_metadata())
            run.assert_not_called()

    def test_release_still_requires_wix(self):
        with patch('toolchain.shutil.which', return_value=None):
            with self.assertRaisesRegex(SystemExit, 'Release WiX was not found'):
                wix_metadata(required=True)

    def test_installed_wix_and_extensions_are_recorded(self):
        responses = [subprocess.CompletedProcess([], 0, '5.0.2+test\n'),
                     subprocess.CompletedProcess([], 0, 'WixToolset.UI.wixext 5.0.2\nWixToolset.Util.wixext 5.0.2\n')]
        with patch('toolchain.shutil.which', return_value='wix.exe'), patch('toolchain.subprocess.run', side_effect=responses):
            self.assertEqual({'wix': '5.0.2+test', 'wixExtensions': ['WixToolset.UI.wixext 5.0.2', 'WixToolset.Util.wixext 5.0.2']},
                             wix_metadata(required=True))

    def test_broken_wix_probe_is_not_reported_as_absent(self):
        for codes in ((1, 0), (0, 1)):
            with self.subTest(codes=codes), patch('toolchain.shutil.which', return_value='wix.exe'), \
                    patch('toolchain.subprocess.run', side_effect=[subprocess.CompletedProcess([], code, '') for code in codes]):
                with self.assertRaisesRegex(SystemExit, 'could not be recorded'):
                    wix_metadata()
