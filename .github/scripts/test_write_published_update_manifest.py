import json
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).with_name("write-published-update-manifest.py")


class WritePublishedUpdateManifestTest(unittest.TestCase):
    def test_publishes_matching_changelog(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            source_dir = root / "update"
            changelog_dir = source_dir / "changelogs"
            output_dir = root / "published" / "update"
            changelog_dir.mkdir(parents=True)
            manifest = {
                "channel": "stable",
                "versionName": "1.2.3",
                "versionCode": 42,
                "isReady": False,
                "assets": [],
            }
            (source_dir / "manifest.json").write_text(
                json.dumps(manifest), encoding="utf-8"
            )
            expected_changelog = "修正更新提示。\n"
            (changelog_dir / "42.changelog").write_text(
                expected_changelog, encoding="utf-8"
            )

            env = os.environ.copy()
            env.update(
                {
                    "SOURCE_MANIFEST": str(source_dir / "manifest.json"),
                    "OUTPUT_DIR": str(output_dir),
                    "RELEASE_URL": "https://example.com/release/42",
                    "ASSET_URL": "https://example.com/release/42/app.apk",
                    "APK_SHA256": "a" * 64,
                    "APK_SIZE": "1234",
                }
            )
            subprocess.run([sys.executable, str(SCRIPT)], env=env, check=True)

            self.assertEqual(
                expected_changelog,
                (output_dir / "changelogs" / "42.changelog").read_text(encoding="utf-8"),
            )
            published_manifest = json.loads(
                (output_dir / "manifest.json").read_text(encoding="utf-8")
            )
            self.assertEqual(expected_changelog, published_manifest["releaseNotes"])

    def test_rejects_missing_changelog(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            source_dir = root / "update"
            source_dir.mkdir()
            (source_dir / "manifest.json").write_text(
                json.dumps({"versionCode": 42}), encoding="utf-8"
            )
            env = os.environ.copy()
            env.update(
                {
                    "SOURCE_MANIFEST": str(source_dir / "manifest.json"),
                    "OUTPUT_DIR": str(root / "published"),
                    "RELEASE_URL": "https://example.com/release/42",
                    "ASSET_URL": "https://example.com/release/42/app.apk",
                    "APK_SHA256": "a" * 64,
                    "APK_SIZE": "1234",
                }
            )

            result = subprocess.run(
                [sys.executable, str(SCRIPT)],
                env=env,
                text=True,
                capture_output=True,
            )

            self.assertNotEqual(0, result.returncode)
            self.assertIn("Missing or empty changelog", result.stderr)


if __name__ == "__main__":
    unittest.main()
