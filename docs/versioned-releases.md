# Automatic AxTrade GitHub releases

This repository now uses **.github/workflows/versioned-release.yml** to
create downloadable releases after every new push to `master`, including
pull request merges. Merged code is built **from the current master**
rather than using a JAR from an older pull request.

## Versioning

The `pom.xml` version is authoritative; `plugin.yml` inherits it through
Maven resource filtering.

| Event | Maven version | Tag | Uploaded asset |
|---|---|---|---|
| Existing | 1.28.2 | — | — |
| Next merge to master | **1.28.3** | `v1.28.3` | `AxTrade-1.28.3.jar` |
| Following merge | **1.28.4** | `v1.28.4` | `AxTrade-1.28.4.jar` |
| Explicit minor upgrade via Actions | **1.29.0** | `v1.29.0` | `AxTrade-1.29.0.jar` |

Each subsequent push uses the version from the previously bumped pom.xml;
other dependency versions are never modified. An **annotated Git tag** and
the version commit are pushed atomically. The version commit is marked
`[skip ci] [skip release]` to avoid an infinite release loop.

To select a **minor** or **major** upgrade, navigate to **Actions → Versioned
AxTrade Release → Run workflow → select bump**. Normal PR merges always
bump **patch**.

## Downloading the JAR

After the release workflow has passed:

1. Open **Releases** on the repository home page (or
   `https://github.com/VoxelHorizons/AxTrade/releases`).
2. Open the newest `AxTrade vX.Y.Z (Voxel Core)` release.
3. Download the versioned `AxTrade-X.Y.Z.jar` under **Assets**.

For a short-lived copy from the same run, download
`AxTrade-X.Y.Z` from **Actions → Versioned AxTrade Release →
Artifacts** (90-day retention). The GitHub Release asset remains available
until explicitly deleted.

## Critical distribution limitation

The upstream POM references optional *private system-scoped libraries*
under `libs/*.jar`; those JARs are absent from this GitHub repository.
Attempting to produce the full upstream plugin with `mvn package` fails.
Do **not** publish the truncated GUI-test distribution under a normal
version and imply that every currency hook works.

Instead, the workflow invokes
`python3 scripts/prepare-gui-test.py --core-release` on the CI workspace,
which builds a separately labelled internal `X.Y.Z-voxel-core` plugin (published as `AxTrade-X.Y.Z.jar`). It includes:

- Core player-to-player trading and the configured GUI controls.
- Vault and experience currency integrations.
- PlaceholderAPI placeholder currency provider.
- VoxelCore optional placeholders, glyphs, item models and `vitem` support.

This distribution **does not contain** hooks for upstream private optional
APIs such as some MobCoins, CoinsEngine, ExcellentEconomy, Kingdoms, etc.
The exact available features are bounded by the source-pruning script.
Do not advertise it as a fully equivalent upstream build. If access to all
optional API JARs is obtained, implement a separate full-distribution job
and verify the redistribution licenses before attaching the full JAR.

The existing pull request **GUI Test JAR** workflow remains available
independently; release builds use a different internal `-voxel-core` version marker; the public JAR name has no suffix.

## Failure / access checks

- The release requires **Actions write access to repository contents**:
  Settings → Actions → General → Workflow permissions → Read and write.
- Protected branch rules must allow an automated version commit to master.
  If direct bot pushes are prohibited, the release will fail **without
  silently publishing a mismatched version or tag**.
- If GitHub Actions fails to compile the selected version, it neither commits
  the version nor tags the release.
- PR checks include a release-build smoke test and standalone version
  regression tests. Workflow runs are serialised to avoid duplicate tags.
- The updater for `-voxel-` builds does not query upstream AxTrade versions.
