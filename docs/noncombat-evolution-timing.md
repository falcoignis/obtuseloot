# Noncombat evolution timing and ability catalog

Normal play can now advance ordinary evolution stages and refresh active abilities from
measured effects without a kill. The existing stage thresholds (10/25/45/70 by default),
archetype rules, awakening requirements, convergence rules and designed drift chance are
unchanged. This does not add wild mutation or noncombat awakening/convergence rolls.

## When updates happen

`EngineScheduler` collects live measured outcomes through `ItemAbilityManager` and evaluates
each affected player once per game-tick window. The default is:

```yaml
evolution:
  noncombat-update-interval-ticks: 100
```

At 20 TPS this is five seconds; under lag it follows game time. This positive setting is
read on every tick and can be changed with the existing runtime reload. An invalid interval
is rejected before replacing the settings snapshot. Empty windows do not select abilities
or reroll identities. This timer does not change the separately scheduled ecology engine.

The queue accepts a finite, nonnegative observation matching the current execution's holder,
storage key, ability, mechanic and trigger. Its success/no-op status must agree with the
observed amount. An effect can report measured runtime outcomes without a Bukkit event;
each handler is responsible for verifying its actual world or runtime action.

The existing classifier's net utility delta earns reputation in that ability's existing
family: one whole net utility unit earns one point. Fractional credit and negative utility
debt persist in YAML and SQL, so windows and restarts do not round away credit or erase debt.
Real measured no-ops contribute their existing penalties. Classifications, unimplemented
effects, budget suppression and failures without observations do not earn this credit or
schedule refreshes. Crediting occurs once at observation time; retrying a window cannot
credit it again. No kill count, boss count or survival streak is fabricated.

At a window boundary, the normal evolution evaluator runs and subscriptions are rebuilt
from the latest utility history. Stale queued identities are discarded. Quit/shutdown flush
ordinary stage evaluation before existing saves, without another ability selection. Join
reevaluates saved reputation before binding abilities. Save failures retain retryable state,
including when the optional artifact cache is disabled. As with existing autosaves, an
abrupt process crash can lose changes since the last completed save.

## Adding or removing an ability

`AbilityRegistry` is the catalog shared by live selection and execution. Each
`AbilityRegistration` owns an immutable `AbilityTemplate` and an optional `LiveAbilityEffect`.
Handlers are separate from template metadata, preserving stable template equality/hashes.

To add an effect for an existing event:

1. Implement `LiveAbilityEffect` in `abilities/effects/`. Check the real event/runtime action
   and applicable protection decisions before reporting success.
2. Add its metadata entry in `AbilityRegistry`, ending with
   `.withLiveEffect(new YourEffect())`.
3. Return `AbilityExecutionResult.observed(...)` with the amount actually committed or
   delivered. Supply `.withObservationKey(...)` when distinct real targets/states should be
   distinguished. A checked, unavailable effect has amount zero; an unimplemented effect
   has no observation. Do not create random keys merely to bypass redundancy penalties.
4. Add a focused effect test for success, denial, repeated outcomes and measured feedback.

No new branch is needed in `AbilityExecutor`, `EngineScheduler` or the evolution queue.
A genuinely new event or mechanic still needs its event wiring or enum definition.

Remove the registration to remove it from selection and execution together. Removing just
its live handler leaves an offline description but excludes it from the live pool. Catalogs
are immutable; these code changes take effect on plugin restart, not via a dynamic plugin
loader or `/ol reload`. Duplicate/blank IDs fail construction. An empty catalog is safe,
and stale definitions cannot execute after their registration is removed.

Live bootstrap uses `implementedOnly()`: currently Gentle Harvest and Material Insight.
The complete metadata catalog remains available to offline tooling, whose classification
outputs do not prove gameplay success. A mutated trigger/mechanic/family without an
implementation retains its original executable definition, with the fallback recorded in
mutation history. Designed drift itself remains unchanged.

## Observation identity and limits

Material Insight identifies the actual world, block coordinates and delivered report
(material/light/growth). An unchanged report at the same target remains redundant; another
target or changed state is distinct. Gentle Harvest identifies the actual world, crop
position and game tick of a paid, committed replant. These keys feed the existing novelty
and spam scoring; no scoring weights or distribution targets change. Telemetry includes
the observation key when present.

Report delivery is a proxy for usefulness. Alternating previously seen targets can reset
the existing consecutive-repeat penalty, and repeat history is not fully durable across
reloads. This is not a comprehensive anti-farming mechanism or evidence of emergence.
The live pool currently contains only two effects, so broad live niche diversity cannot
be inferred from the offline catalog or its health test.

The SQL reputation schema gains `utility_experience`. Fresh databases are required for
this schema version; no migration is provided, following the owner's no-existing-data
decision. SQLite and YAML round trips are tested; no live MySQL server is used in tests.

## Validation

Focused verification: 60 tests passed, including a real dispatcher-to-tracker-to-window
progression path with no kills, tick cadence/reload, idle windows, stale identity rejection,
quit/shutdown, retry credit, fractional/debt persistence, cache-disabled save failure,
catalog add/remove, stable metadata hashes, supported live generation, and real inspected
targets versus repeated unchanged reports.

Full local `clean package`: **445 tests across 86 classes, zero failures/errors/skips**,
Maven exit 0, 21:45. The existing seeded ecosystem health test passed. See the canonical
skill status for publication state.
These tests use real Bukkit events and stateful API doubles. No game server was started
or deployed. Server and protection-plugin validation remains pending the owner's chosen
test destination; see [measured ability effects](measured-ability-effects.md).
