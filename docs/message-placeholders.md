# PlaceholderAPI in AxTrade messages

This PR builds on the GUI placeholder integration in PR #1, reusing the
same ordered resolver for AxTrade-controlled player-facing text: `config.yml`
`prefix`, `lang.yml` messages and help arrays, trade summaries, trade
request button labels and hover text, and update notifications. GUI titles,
item fields, and the currency-editor sign are covered by prerequisite PR #1.

## Configuration example

```yaml
# config.yml
prefix: ':branding_icon_small: &8[%player_name%] &7» '

# lang.yml
trade:
  started: '&aTrading with %player% &7(Balance: %vault_eco_balance_commas%)'
request:
  sent-receiver:
    accept:
      hover: '&aAccept trade &7(Balance: %vault_eco_balance_commas%)'
```

AxTrade variables (for example `%player%`, `%amount%`, `%currency%`,
`%current%` and `%latest%`) resolve **first**; PlaceholderAPI runs with
the **message recipient** as viewer; optional VoxelCore glyph aliases such
as `:offset_-8:` resolve last. AxTrade `%player%` continues to mean
the *other trader*; PAPI's `%player_name%` means the *recipient*.
Parsing occurs at send time, not at YAML load time.

These are optional integrations. If PlaceholderAPI or VoxelCore is absent,
external tokens remain literal and trading continues. Console output can
only resolve server/global placeholders: it has no player context.
Command-framework validation errors have the same limitation because its
`LocaleReader.get(String)` API does not expose a sender.

Sounds, permissions, command aliases, blacklist patterns, numeric values and
other machine-readable settings are deliberately **not** parsed. The actual
click commands remain fixed trusted AxTrade commands. This change does not
modify messages emitted by other plugins.

`/axtrade reload` now also refreshes update notification templates.

## Verification checklist

1. Include `:branding_icon_small:` and `%player_name%` in `prefix`.
   Have two different players trade and verify each sees their own name.
2. Place `%vault_eco_balance_commas%` in request info, accept/deny labels,
   accept/deny hover text, and started/completed/aborted messages.
3. Verify `%player%` still means the trade partner and `%player_name%`
   means the viewer.
4. Verify help arrays, summary lines, failure messages, reload, GUI title/item
   lore, currency sign, and update notifications.
5. Uninstall PAPI and VoxelCore independently and confirm graceful fallback.
6. Reload `config.yml` and `lang.yml` and confirm live changes are used.

The dependency-free Java regression workflow covers ordering and sample
prefix/hover substitutions. A production Maven build still requires the
upstream proprietary optional API JARs documented by PR #1.
