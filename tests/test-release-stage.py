#!/usr/bin/env python3
"""Release stage smoke test, including the previously failing __pycache__ case."""
import importlib.util
from pathlib import Path
import subprocess
import tempfile

root = Path(__file__).resolve().parents[1]
path = root / "scripts" / "validate-release-stage.py"
spec = importlib.util.spec_from_file_location("validate_release_stage", path)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

def git(worktree: Path, *args: str) -> None:
    subprocess.run(("git", *args), cwd=worktree, check=True,
                   stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)

with tempfile.TemporaryDirectory() as scratch:
    worktree = Path(scratch)
    git(worktree, "init", "-q")
    (worktree / "pom.xml").write_text("<version>1.28.2</version>\n")
    (worktree / "README.md").write_text("tracked file\n")
    git(worktree, "add", "pom.xml", "README.md")
    git(worktree, "-c", "user.name=CI", "-c", "user.email=ci@example.invalid",
        "commit", "-qm", "initial")
    (worktree / "pom.xml").write_text("<version>1.28.3</version>\n")
    # The old git status --porcelain count incorrectly rejected this directory.
    bytecode = worktree / "scripts" / "__pycache__"
    bytecode.mkdir(parents=True)
    (bytecode / "bump-release-version.cpython-313.pyc").write_bytes(b"scratch")
    git(worktree, "add", "pom.xml")
    module.validate(worktree)  # untracked scratch does not block a release

    (worktree / "README.md").write_text("unexpected tracked mutation\n")
    try:
        module.validate(worktree)
        raise AssertionError("Unexpected modification of README.md was accepted")
    except RuntimeError:
        pass
    git(worktree, "restore", "README.md")

    git(worktree, "add", "README.md")
    (worktree / "README.md").write_text("unexpected staged change\n")
    git(worktree, "add", "README.md")
    try:
        module.validate(worktree)
        raise AssertionError("Unexpected staged README.md was accepted")
    except RuntimeError:
        pass

print("Release staging tests passed, including untracked __pycache__.")
