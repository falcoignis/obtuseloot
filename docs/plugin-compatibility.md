# Plugin compatibility contract and AuraSkills review

Owner requirements, 2026-10-21: ObtuseLoot must work standalone and retain plugin support
as a standing design concern. Support PlaceholderAPI and LuckPerms when installed; detect
AuraSkills at boot and preserve its normal XP gain. PacketEvents is conditional on a
feature needing it. Whether to add **extra** AuraSkills XP from artifact outcomes is still
an open design choice; preserving normal XP does not imply awarding additional XP.

## Rules for future features

Use a shared catalog and explicit integration boundaries, not plugin-specific branches
in the evolution or reproduction loop. Every new ability must identify its equipment,
input and effect ownership, and consider these compatibility surfaces:

| Surface | Required behavior |
| --- | --- |
| Optional dependencies | Core works when absent; present/broken optional APIs fail visibly without regenerating data or disabling unrelated core features. |
| Permissions | Honor effective permissions/context changes; no OP bypass or automatic group/grant writes. |
| Protection and vanilla events | Respect final denials. Never uncancel an event to force an ability or suppress another plugin's legitimate gameplay/XP handling. |
| Movement, attributes, anticheat | Own and release only our modifiers; preserve other movement/flight owners. Validate actual server behavior before enabling more movement features. |
| Inventory, equipment, synchronization | Preserve metadata and per-item identity; reconcile restored/swapped gear. Test external equipment restrictions and restoration paths. |
| XP, mana, skills and economy | Do not replace native accounting or duplicate rewards. Add cross-plugin awards only under an explicit, reviewed design. |
| Display and placeholders | Read-only snapshots; no storage, activation, permission changes or evolution from formatting requests. |

A specific unsupported case must be recorded rather than hidden behind a generic claim
that a plugin is supported. Availability, API readiness and verified compatibility are
separate facts. Keep existing adapters and absent-plugin tests working as new adapters
are added. Reuse established server/protection APIs where sufficient; PacketEvents is an
input adapter of last resort, not a replacement for server validation or protection checks.

## AuraSkills findings

This review used the local gameplay/QoL skill and upstream documentation/source on
2026-10-21, source boundary `0ef2b6d10fc949e77a4e5867615a1aae6bebd3f7`. The local skill includes historical server-specific information, which is not
a current inventory or a compatibility test. Upstream's latest release at review time was
2.4.0; API documentation still shows a 2.2.4 dependency example. Verify the actual API
version before introducing a compiled AuraSkills adapter.

- **Loading:** skill content may finish loading on the first server tick. A future API
  adapter must wait for `SkillsLoadEvent` and check the user's loaded state, rather than
  querying skill content in `onEnable`. [Official API guide](https://wiki.aurelium.dev/auraskills/api)
- **Input:** mana abilities use secondary clicks and have `require_sneak`, `check_offhand`
  and `sneak_offhand_bypass` options. Preserve those decisions and normal input propagation.
  [Mana abilities](https://wiki.aurelium.dev/auraskills/mana-abilities)
- **Movement:** the inspected `MovementSpeedTrait` writes player walk speed and refreshes
  it on load/world changes. ObtuseLoot must not overwrite that state. Its separate owned
  attribute modifier still needs actual-server tests across AuraSkills trait refreshes.
  [Upstream speed implementation](https://github.com/Archy-X/AuraSkills/blob/0ef2b6d10fc949e77a4e5867615a1aae6bebd3f7/bukkit/src/main/java/dev/aurelium/auraskills/bukkit/trait/MovementSpeedTrait.java)
- **Requirements:** AuraSkills distinguishes held-item and armor requirements, including
  global requirements. Cancelled vanilla interactions/equipment changes must remain denied.
  Restored inventories and directly equipped restricted gear need explicit server tests;
  this slice does not duplicate AuraSkills' internal global-requirement engine.
  [Item requirements](https://wiki.aurelium.dev/auraskills/skills/item-requirements)
- **Farming XP:** upstream's block XP handler checks placed-block status only for a source
  whose `check_replace` is true. Its placement tracker uses the same option. All five crops
  Gentle Harvest replants ship with `check_replace: false`, so the protection placement
  probe does not, under those defaults, mark these crops as placed sources that suppress
  normal farming XP. This is source-backed expected behavior, not a live server result.
  Preserve custom server policies rather than deleting tracking to force XP.
  [Farming defaults](https://github.com/Archy-X/AuraSkills/blob/0ef2b6d10fc949e77a4e5867615a1aae6bebd3f7/common/src/main/resources/sources/farming.yml),
  [placement tracking](https://github.com/Archy-X/AuraSkills/blob/0ef2b6d10fc949e77a4e5867615a1aae6bebd3f7/bukkit/src/main/java/dev/aurelium/auraskills/bukkit/region/BukkitRegionManager.java),
  [native block XP](https://github.com/Archy-X/AuraSkills/blob/0ef2b6d10fc949e77a4e5867615a1aae6bebd3f7/bukkit/src/main/java/dev/aurelium/auraskills/bukkit/source/BlockLeveler.java)
- **Replant overlap:** AuraSkills' Replenish schedules its own crop replacement. Test both
  abilities together for redundant payment, delayed replacement and outcome attribution.
  Do not call the same crop two paid successes or change AuraSkills XP to compensate.
  [Upstream Replenish](https://github.com/Archy-X/AuraSkills/blob/0ef2b6d10fc949e77a4e5867615a1aae6bebd3f7/bukkit/src/main/java/dev/aurelium/auraskills/bukkit/skills/farming/Replenish.java)

The current coexistence baseline leaves native XP listeners, mana, stats, item requirements
and placed-block tracking owned by AuraSkills. It neither injects nor intercepts XP events.
Actual server compatibility remains unverified; no live server configuration was changed.

## Native movement follow-up

[Double Jump](double-jump.md) uses native input with per-player charges, effective
permissions and per-item credit. Its custom pre-impulse veto and ordinary move/velocity
denials remain active. It reserves elytra input and foreign flight ownership, and writes
no XP, mana, potion, fall-distance or permission state. The default is disabled pending
testing of the intended claim/anticheat/skill combination. A disposable Purpur 26.2 protocol
test is narrower evidence than certification with those production plugins.
