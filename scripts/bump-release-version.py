#!/usr/bin/env python3
"""Increment AxTrade's Maven version without altering other dependency versions.

Normal pushes to master bump the patch component; larger version changes are
explicit workflow_dispatch choices.  Ex: 1.28.2 -> 1.28.3, minor -> 1.29.0.
"""
import argparse
from pathlib import Path
import os
import re

POM = Path(__file__).resolve().parents[1] / "pom.xml"
# The first AxTrade project version is next to its artifactId; dependency
# versions later in the file are intentionally left unchanged.
PROJECT = re.compile(
    r"(<artifactId>AxTrade</artifactId>\s*<version>)(\d+)\.(\d+)\.(\d+)(</version>)"
)


def bump(content: str, kind: str) -> tuple[str, str]:
    match = PROJECT.search(content)
    if not match:
        raise ValueError("Could not find AxTrade's numeric project version in pom.xml")
    major, minor, patch = (int(match.group(i)) for i in (2, 3, 4))
    if kind == "patch":
        patch += 1
    elif kind == "minor":
        minor += 1
        patch = 0
    elif kind == "major":
        major += 1
        minor = patch = 0
    else:
        raise ValueError(f"Unsupported bump: {kind}")
    version = f"{major}.{minor}.{patch}"
    result = content[:match.start()] + match.group(1) + version + match.group(5) + content[match.end():]
    return result, version


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--bump", choices=("patch", "minor", "major"), default="patch")
    args = parser.parse_args()
    updated, version = bump(POM.read_text(encoding="utf-8"), args.bump)
    POM.write_text(updated, encoding="utf-8")
    print(f"AxTrade release version: {version}")
    if os.getenv("GITHUB_OUTPUT"):
        with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as output:
            output.write(f"version={version}\n")
            output.write(f"tag=v{version}\n")


if __name__ == "__main__":
    main()
