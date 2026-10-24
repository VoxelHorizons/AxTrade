#!/usr/bin/env python3
"""Safely validate the exact tracked contents of an AxTrade release commit.

Generated build outputs, untracked Python __pycache__ directories, and other
untracked CI scratch files must not block a release or enter its git commit.
The ONLY staged change is permitted to be pom.xml; no other tracked changes
may remain unstaged.
"""
from pathlib import Path
import subprocess
import sys


def git_paths(root: Path, *args: str) -> list[str]:
    result = subprocess.run(
        ("git", *args, "-z"), cwd=root, check=True,
        stdout=subprocess.PIPE,
    )
    return sorted(path.decode("utf-8") for path in result.stdout.split(b"\0") if path)


def validate(root: Path) -> None:
    staged = git_paths(root, "diff", "--cached", "--name-only")
    unstaged = git_paths(root, "diff", "--name-only")
    if staged != ["pom.xml"] or unstaged:
        raise RuntimeError(
            "Unsafe release commit: only pom.xml may be staged, and no "
            f"tracked changes may remain unstaged. staged={staged!r}, "
            f"unstaged={unstaged!r}"
        )


if __name__ == "__main__":
    try:
        validate(Path.cwd())
    except (RuntimeError, subprocess.CalledProcessError) as error:
        print(f"Release staging validation failed: {error}", file=sys.stderr)
        sys.exit(1)
    print("Release staging validated: pom.xml is the only staged file.")
