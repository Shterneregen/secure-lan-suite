import unittest

from release import validate_certificate, verify_qualification


class ReleaseGateTest(unittest.TestCase):
    def test_unexpected_missing_multiple_and_debug_certificates_are_rejected(self):
        trusted = 'a' * 64
        for output in ('', 'Signer #1 certificate SHA-256 digest: ' + 'b' * 64,
                       'Signer #1 certificate SHA-256 digest: ' + trusted + '\nSigner #2 certificate SHA-256 digest: ' + trusted,
                       'Signer #1 certificate SHA-256 digest: ' + trusted + '\nSigner #1 certificate DN: CN=Android Debug'):
            with self.subTest(output=output), self.assertRaises(ValueError):
                validate_certificate(output, trusted)
        self.assertEqual(trusted, validate_certificate('Signer #1 certificate SHA-256 digest: ' + trusted, trusted))

    def test_qualification_is_bound_to_exact_commit_and_artifact_bytes(self):
        artifacts = {'candidate.apk': 'a' * 64}
        record = {'commit': 'commit', 'artifacts': artifacts, 'devices': ['Windows VM', 'Android device'],
                  'tester': 'maintainer', 'testedAt': '2026-10-17', 'windowsSigning': 'unsigned-pilot',
                  'unsignedPilotReason': 'Pilot distribution with checksums',
                  'checks': {name: 'pass' for name in (
                      'desktopLaunchWithoutJava', 'hostJoinChat', 'filesBothDirections', 'disconnectReconnect',
                      'voiceStartStop', 'androidUpgrade', 'windowsInstallUpgradeUninstall', 'thirdPartyNoticesReviewed')}}
        verify_qualification(record, artifacts, 'commit')
        for commit, hashes in (('other', artifacts), ('commit', {'candidate.apk': 'b' * 64})):
            with self.assertRaises(ValueError):
                verify_qualification(record, hashes, commit)
        record['checks']['voiceStartStop'] = 'not-tested'
        with self.assertRaises(ValueError):
            verify_qualification(record, artifacts, 'commit')


if __name__ == '__main__':
    unittest.main()
