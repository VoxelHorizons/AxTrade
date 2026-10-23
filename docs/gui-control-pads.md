# VoxelHorizons trade GUI controls with separate pads

AxTrade can now render optional pad controls that display **different ItemStacks**
from the primary confirmation / currency controls, while sharing their handlers,
placeholders and live trade status.

## Pad naming and behavior

| Main section | Optional matching pad | Behavior |
|---|---|---|
| `own.confirm-item` | `own.confirm-item-pad` | Both click to confirm, or to cancel when already confirmed |
| `partner.confirm-item` | `partner.confirm-item-pad` | Both mirror partner's state, with read-only clicks |
| `own.currency1` | `own.currency1-pad` | Both open the SAME currency amount selector |
| `partner.currency1` | `partner.currency1-pad` | Both show partner's amount; clicks are read-only |
| `own.currency2` | `own.currency2-pad` | Likewise, for a second currency |

Other numbered currency sections work the same. Currency pads inherit the
currency **provider name** from their base section and have the same
`%amount%`, `%tax-amount%`, `%tax-percent%`, `%tax-fee%` placeholders.
Do **not** repeat `currency:` in a currency pad.

A `confirm-item-pad` may use independent `accept:` / `cancel:` states,
or a flat item section (`material:`/`voxelcore:`) to use the same visual
while the active click action changes. Both variants use the correct handler.
Main confirm buttons preserve the existing countdown stack-size behavior;
pads default to amount 1 to prevent countdown-dependent UI artifacts.
Add `use-confirm-countdown: true` under a pad to mirror the countdown if needed.

## Registering VoxelCore items

Use `voxelcore: voxel:ui-blank` (or the shorthand `material: voxel:ui-blank`) to create a **registered VoxelCore item**:
the integration calls the public VoxelCore `ItemManager.createItem(ContentID)`
API, preserving its PDC and custom model components. Abstract display-only
items are supported through `ItemManager.createRenderItem(ContentID)`.

```yaml
voxelcore: 'voxel:ui-blank'
name: '&fClick to confirm'
lore:
  - '&7Click any part of this button'
```

`material: GLASS_PANE` plus `item-model: voxel:ui/blank` is also supported;
that is a **vanilla item with a model override**, not the registered VoxelCore item.
To provide a fallback on servers without VoxelCore, put both keys in an item:
`voxelcore: voxel:ui-blank` and `material: GLASS_PANE`.
The VoxelCore variant takes precedence; missing custom items log a warning
and use the vanilla fallback when provided (or a visible BARRIER otherwise).

## Example: 54-slot, 20 items per side, separate pad and main icons

```yaml
own-slots: ['14-17', '23-26', '32-35', '41-44', '50-53']
partner-slots: ['9-12', '18-21', '27-30', '36-39', '45-48']

own:
  confirm-item:
    slot: 8
    accept:
      material: PAPER
      item-model: voxel:ui/trade/confirm-off
      name: '&a&lConfirm trade'
    cancel:
      material: PAPER
      item-model: voxel:ui/trade/confirm-on
      name: '&c&lCancel confirmation'
  confirm-item-pad:
    slot: 7
    # Flat pad: the same blank custom item in both confirmation states
    voxelcore: 'voxel:ui-blank'
    material: GLASS_PANE
    name: '&fClick to confirm or cancel'

  currency1:
    slot: 6
    currency: Vault
    material: GOLD_NUGGET
    item-model: voxel:ui/trade/coins
    name: '&6Coins: %amount%'
  currency1-pad:
    slot: 5
    voxelcore: 'voxel:ui-blank'
    material: GLASS_PANE
    name: '&6Coins: %amount%'

partner:
  confirm-item:
    slot: 0
    accept:
      material: PAPER
      item-model: voxel:ui/trade/partner-waiting
      name: '&7Waiting for %partner-name%'
    cancel:
      material: PAPER
      item-model: voxel:ui/trade/partner-ready
      name: '&a%partner-name% is ready'
  confirm-item-pad:
    slot: 1
    accept:
      voxelcore: 'voxel:ui-blank'
      name: '&7Waiting for %partner-name%'
    cancel:
      voxelcore: 'voxel:ui-blank'
      name: '&a%partner-name% is ready'

  currency1:
    slot: 2
    currency: Vault
    material: GOLD_NUGGET
    name: '&6Their coins: %amount%'
  currency1-pad:
    slot: 3
    voxelcore: 'voxel:ui-blank'
    name: '&6Their coins: %amount%'
```

The model names `voxel:ui/trade/*` in the example are **illustrative**.
Use the model IDs that actually exist in your resource pack.

Controls and pads must have **non-overlapping slots** and must never be
placed in `own-slots` or `partner-slots`; those slots store actual traded items.

## Testing

Install the latest `AxTrade-GUI-Test-JAR` artifact from PR #1. Restart your
Paper test server, then edit `plugins/AxTrade/guis.yml`, run `/axtrade reload`
and `/axtrade preview`. For full action verification, use two players:
accepting and canceling through either own main icon or its pad, setting
currency amount through either own currency icon or its pad, and ensuring
partner-side controls are read-only.

The CI GUI-test distribution intentionally excludes some optional third-party
currency and AxShulkers hooks because upstream doesn't publish their API JARs
to the fork. Production deployment still requires a complete build.
