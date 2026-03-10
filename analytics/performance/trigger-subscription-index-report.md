# Trigger Subscription Index Report

Generated: development-time baseline (no live server dispatch sample)

## 1) What was indexed
- Per-player trigger subscriptions are indexed into `AbilityTrigger -> ArtifactTriggerBinding` lists.
- Bindings contain artifact identity metadata and ability references used by runtime dispatch.

## 2) Event types benefited most
- `ON_MOVEMENT`
- `ON_HIT`
- `ON_KILL`
- `ON_BOSS_KILL`
- `ON_LOW_HEALTH`
- `ON_REPOSITION`
- `ON_MEMORY_EVENT`

These event categories no longer require full ability-profile scans when trigger indexing is enabled.

## 3) Average subscriber counts
- Live values are emitted from runtime instrumentation via `TriggerSubscriptionIndexReporter`.
- Use `/obtuseloot debug subscriptions stats` for in-server summary.

## 4) Rebuild behavior
- Rebuilds occur on join load, debug refresh flows, and kill-cycle state changes (evolution/drift/awakening/fusion updates).
- Rebuild timings are captured in microseconds and exported by the runtime report writer.

## 5) Estimated runtime savings
- Dispatch now performs trigger-targeted lookup instead of full scans for indexed players.
- Savings increase with larger ability pools and sparse per-trigger subscriptions.

## 6) Remaining hot-path concerns
- Ensure debug/manual mutation flows call refresh hooks to avoid stale subscriptions.
- Keep `runtime.triggerSubscriptionIndexing` enabled in production unless fallback scan diagnostics are needed.
