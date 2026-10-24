# Native currency-input dialogs (Paper 1.21.7+)

AxTrade can now present the trade currency editor as a **native Minecraft
Dialog**, not the temporary sign editor, on newer Paper servers with the
Dialog API. The same trading, permissions, validation, quantity parsing,
accept/cancel state and inventory return logic is shared across both paths.

## Requirements and fallback

Minecraft added dialogs in 1.21.6; the **Paper Java Dialog API** began
in Paper 1.21.7. AxTrade uses a runtime API capability check instead of
parsing the Minecraft version, so versions such as 26.2 and 26.3 can
use the native API where it exists.

- Newer **Paper** with the API: native text-input Dialog.
- Older Paper, Spigot, unavailable API, or dialog creation exception:
  the original **four-line sign editor** is used.
- No new plugin hard-dependency; a legacy server does not load or link
  Paper-only Dialog API types just to enable AxTrade.
- `config.yml` supports `currency-input.mode: auto | dialog | sign`.
  Both `auto` and `dialog` fall back safely to signs if Dialog APIs
  are unavailable; `sign` forces the old behavior.
- If native dialog creation fails, the failure is logged with a warning
  instead of aborting the trade.

## Example configuration

```yaml
# config.yml
currency-input:
  mode: auto
  max-length: 32
  width: 240
```

```yaml
# lang.yml
currency-editor-dialog:
  title: "&#00ffddEnter %currency% offer"
  input-label: "&fAmount"
  confirm: "&#00ffddApply"
  cancel: "&#ff7777Cancel"
```

Display strings support AxTrade's existing placeholder order:
AxTrade internal tokens, PlaceholderAPI, then VoxelCore UI font aliases.
`%currency%` expands to the chosen currency hook name.

## Runtime behavior and safety

- Clicking own currency item still cancels confirmation and closes the
  trade GUI with its existing `inSign` temporary-editor guard.
- The native form includes a text field and separate Apply/Cancel buttons.
  Escape closing is disabled, ensuring an explicit reply rather than
  silently suppressing the trade's inventory close/abort lifecycle.
- Apply passes the text unchanged to `TradePlayer.setCurrency`;
  number validation, balance enforcement and messages are unchanged.
- Cancel reopens the trade GUI without changing the currency offer.
- The callback is single-use; it checks the responding player's UUID
  and runs currency mutations on that player's server scheduler.
- Opening the GUI again after either choice is deferred one tick to
  let the Dialog close, preserving trade offers, contents and state.
- Trade expiration/abort during a dialog prevents the editor from
  reopening a finished trade.

## Manual testing checklist

1. On Paper 26.3, click Vault and Experience entries, type an amount,
   apply, and check both traders see the updated offer.
2. Repeat with too-high balance, negative, invalid and empty input;
   ensure the old currency validation paths and messages apply.
3. Test Cancel and subsequent trade reopen without changing offers.
4. In `mode: sign`, ensure the previous sign UI is still used on 26.3.
5. On an older Paper or Spigot server, `auto` must open the sign editor,
   and the plugin must load without modern Paper Dialog classes.
6. Test when the other trader aborts while the Dialog is open, and when
   the viewer disconnects mid-dialog. No closed trade may reopen.
7. Verify optional PlaceholderAPI/skin/VoxelCore placeholders in dialog
   title and labels.
8. Test specifically on the server's bundled Paper/Dialog API version;
   the PR's CI compiles core code, but cannot simulate real UI packets.

Reference: https://docs.papermc.io/paper/dev/dialogs/
and https://minecraft.wiki/w/Dialog
