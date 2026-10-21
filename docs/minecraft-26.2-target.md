# Minecraft 26.2 build target

The owner selected Minecraft **26.2** on 2026-10-21. This replaces the old
1.21.1 API and Java 21 baseline. The plugin now requires **Java 25** and declares
`api-version: "26.2"`, so older servers are outside this target.

## Aligned build surfaces

- `pom.xml`: Java release 25 and the fixed stable API
  `org.purpurmc.purpur:purpur-api:26.2.build.2633-stable`, provided by the server.
- All seven workflows that set up Java use Temurin 25, including build, release,
  dashboard and simulation workflows. The disabled nightly workflow stays disabled.
- Local builds use the official `maven:3.9-eclipse-temurin-25` image.
- Runtime plugin metadata and README requirements declare 26.2.
- The canonical skill and decision ledger carry the same target.

The upstream API has moved from `-R0.1-SNAPSHOT` coordinates to exact build versions.
The pinned build makes dependency resolution repeatable without following a newer
Minecraft line. Updating this dependency is a reviewed repository change.

## API migration

`Attribute.GENERIC_MOVEMENT_SPEED` was removed; the effect now uses
`Attribute.MOVEMENT_SPEED`. Attribute constants are backed by the running server's
registry. The speed handler accepts an attribute lookup for controlled test fixtures;
its production constructor uses the real player attribute. This preserves tests of
actual modifier objects without pretending to initialize a game server registry.
Both lease removal and startup cleanup continue to affect only owned modifiers.

The official API JAR SHA-256 was compared with the publisher's digest:
`8de76f407d6b70c33ed8fca73d48ad98ffba3d035eda5eecaa4c62eb80533746`.
The reviewed sources JAR independently matched its published SHA-256:
`a4ace22b3b6428c2a75f276316b9f8d7544e6530a6839d564b51e809edc4c823`.

## Compatibility boundaries

The core retains optional PlaceholderAPI, LuckPerms and AuraSkills support.
Retargeting does not add XP awards, cancel native skill XP or change another plugin's
permissions, movement state, protection rules or configuration. Optional API absence
and failure checks remain part of verification. See [plugin compatibility](plugin-compatibility.md).

26.2 provides native jump input through `PlayerInputEvent` and `Input.isJump()`;
this lets the forthcoming double-jump slice use native input without requiring
PacketEvents or granting flight as a key-detection mechanism. Double jump itself
remains a separate movement/protection-tested change.

Compilation and API-double tests are not a real-server compatibility certificate.
The server testing/deployment destination remains undecided; this build-target choice
does not authorize deployment or upgrades to an existing server.

## Upstream references

- [Paper Java requirements](https://docs.papermc.io/paper/getting-started/)
- [Paper 26.2 API changes](https://papermc.io/news/26-2/)
- [Official Purpur Maven metadata](https://repo.purpurmc.org/snapshots/org/purpurmc/purpur/purpur-api/maven-metadata.xml)

## Verification

Final focused run: **47 tests across five classes**, zero failures/errors/skips,
Maven exit 0. This covers equipment, passive modifier ownership/additive stacking,
real event-based effects, immutable placeholders and standalone optional integrations.

Full `clean package` on Java 25: **493 tests across 96 classes**, zero
failures/errors/skips, Maven exit 0, **19:36**. All **505** source/resource/build-file
hashes matched the frozen test manifest afterward. The packaged plugin contains Java 25
class files (major version 69), declares API 26.2, retains only soft plugin dependencies,
and does not bundle optional plugin API classes. Generated tracked reports were restored.

No game server was started or deployed. Actual startup and cross-plugin movement,
protection and XP behavior still need testing on an explicitly chosen server destination.
