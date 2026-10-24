#!/usr/bin/env python3
"""Standalone release-version regression checks."""
import importlib.util
from pathlib import Path

module_path = Path(__file__).resolve().parents[2] / "scripts" / "bump-release-version.py"
spec = importlib.util.spec_from_file_location("version_bump", module_path)
mod = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)

for mode, expected in [("patch", "1.28.3"), ("minor", "1.29.0"), ("major", "2.0.0")]:
    sample = '<artifactId>AxTrade</artifactId>\n    <version>1.28.2</version>\n<dependencies><version>123</version>'
    modified, value = mod.bump(sample, mode)
    assert value == expected, (mode, value)
    assert '<version>123</version>' in modified, "dependency version was modified"
    assert f"<version>{expected}</version>" in modified, modified

try:
    mod.bump("<version>nope</version>", "patch")
    raise AssertionError("Malformed project version accepted")
except ValueError:
    pass
print("Release version bump regression tests passed")
