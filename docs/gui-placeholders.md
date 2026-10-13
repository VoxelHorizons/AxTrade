# GUI placeholders (VoxelHorizons fork)

All `guis.yml` text used to build the trade interface now shares one placeholder pipeline:

1. AxTrade internal tokens (e.g. `%player%`, `%own-name%`, `%partner-name%`, `%amount%`, `%own-status%`, `%partner-status%`)
2. PlaceholderAPI with **the player viewing that GUI** as context
3. VoxelCore UI aliases and offsets when VoxelCore is installed (via its public `TextPlaceholderService`)

Both integrations are optional. Disabling PlaceholderAPI or VoxelCore leaves the underlying GUI and trading functional, with unsupported placeholders unchanged.

## Config examples

```yaml
# guis.yml - title (AxTrade's %player% is the trade PARTNER)
title: ':offset_-8::trade_menu:&fTrading with %player% &7(%player_name%)'

decoration-example:
  slot: [4, 13, 22, 31, 40, 49]
  material: GLASS_PANE
  item-model: 'voxel:ui/blank'
  name: '&f:branding_icon_small::offset_3:%player_name%'
  lore:
    - '&f:shop_coin:Balance: &6%vault_eco_balance_commas%'
    - '&fYour trade partner is %player%'
```

The same processing is applied to **all string-valued AxAPI item builder fields**, including `material`/`type`, `texture`, `item-model`, `tooltip-style`, `amount`, `color`, `name`, `lore`, and nested config strings. YAML numbers and booleans are kept as their original types. Slot lists can contain numeric PlaceholderAPI expressions too, though trade inventory zones should generally use fixed slots.

`%player_name%` is resolved for the **viewer**, while AxTrade's built-in `%player%` is resolved for the **partner** in both title and items. Existing `%own-name%`, `%partner-name%`, and dynamic currency values retain their meaning. This order matters: trade tokens are replaced **before** PlaceholderAPI is asked to parse them.

While the AxAPI item builder supports existing hex and `&` color formatting, avoid relying on arbitrary PlaceholderAPI output as a valid material name, model key, or numeric amount unless that expansion produces the appropriate type.

### Preview and live updates

Run `/axtrade preview` to inspect your GUI with your own player as both trading parties. Live trade titles are re-resolved when trade status changes; accepting/rejecting can change `%own-status%` and `%partner-status%`. Trade GUI items are built for the viewer on initial display and on subsequent updates.

### Optional dependencies

- PlaceholderAPI is already an optional dependency and must be installed for `%player_name%` and expansion placeholders.
- VoxelCore is listed as a `softdepend` to encourage stable load order; it is not bundled or required.
- No dependency changes to AxTrade's trading economy, cancellation, or inventory-transaction code.

### Build note

The upstream `pom.xml` references several locally distributed plugins with `<scope>system</scope>` and paths in `libs/*.jar`. Those JARs were not included in the GitHub fork (the root `.gitignore` ignores JARs), so a full Maven build cannot be assumed to work in a clean GitHub runner without supplying these APIs or replacing the system dependencies with resolvable repositories. A separate dependency-free regression test validates the text replacement ordering.
