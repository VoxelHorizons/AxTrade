**Bug Reports and Feature Requests:** https://github.com/Artillex-Studios/Issues

**Support:** https://dc.artillex-studios.com/

![axtrade-banner](https://github.com/Artillex-Studios/AxTrade/assets/52270269/5383e676-8355-4ee6-a4b6-624b5c59aaa9)

### VoxelHorizons GUI placeholder support

The VoxelHorizons fork adds optional PlaceholderAPI + VoxelCore processing for AxTrade titles, item builder fields, trade currency signs and auxiliary GUI text. See [GUI placeholder documentation](docs/gui-placeholders.md) for supported tokens and examples.

### Versioned Voxel Core releases

On each merge to `master`, the GitHub Actions release workflow increments the
patch version, compiles the current merged code, and publishes a versioned JAR
under [GitHub Releases](https://github.com/VoxelHorizons/AxTrade/releases).
Minor/major bumps are manual. See [versioning and optional dependency
limitations](docs/versioned-releases.md) before deploying: the public release
JAR includes the core/Vault/experience/PlaceholderAPI feature set but excludes
unavailable private third-party currency hooks.
