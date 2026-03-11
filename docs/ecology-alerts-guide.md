# Ecology Alerts Guide

## What PNNC measures
Persistent Novel Niche Count (PNNC) counts niche signatures that are both **novel** and **durable**.
A niche is novel only when its signature is sufficiently separated from prior known niches.
A niche is durable only when it persists long enough with enough occupancy/species support.

## PNNC vs NSER
- **NSER**: rate of newly emergent strategies in the current window.
- **PNNC**: how many novel niches actually survived significance thresholds.

NSER can spike while PNNC stays zero (temporary novelty). PNNC confirms long-horizon ecological expansion.

## Novelty and persistence defaults
PNNC defaults are conservative:
- minimum persistence seasons: `3`
- minimum occupancy share: `0.05`
- minimum species support: `2`
- novelty distance threshold: `0.35`

Signature dimensions used in novelty distance:
- trigger profile distribution
- mechanic distribution
- regulatory gate profile
- environmental affinity
- survival/persistence style
- support vs combat role
- memory/environment role
- branch tendency
- occupancy/species support context

## PNNC interpretation guidance
- `PNNC = 0` → no durable novel niches.
- `PNNC = 1-2` → weak bounded novelty.
- `PNNC = 3-5` → real ecological expansion.
- `PNNC > 5` → strong persistent novelty / possible open-ended dynamics.

## PNNC-aware alert conditions
Alerts/regression gates evaluate END + TNT + NSER + PNNC:
- `FALSE_DIVERGENCE` (warning): active turnover/diversity but PNNC stays zero.
- `BOUNDED_RESHUFFLING` (warning): ecological activity without durable niche persistence.
- `EMERGENT_ECOLOGY` (info): END/TNT/NSER healthy and PNNC above threshold.
- `NOVELTY_REGRESSION` (warning/error): baseline had PNNC>0 and current drops below threshold.

## CI warning vs fail behavior
Configure via JVM properties (used by world-lab and CI runs):
- `analytics.ecologyAlerts.enabled=true`
- `analytics.ecologyAlerts.failOnError=true`
- `analytics.ecologyAlerts.minEND=2.5`
- `analytics.ecologyAlerts.maxTNT=0.60`
- `analytics.ecologyAlerts.minNSER=0.05`
- `analytics.ecologyAlerts.minPNNC=1`
- `analytics.ecologyAlerts.warnIfFalseDivergence=true`
- `analytics.ecologyAlerts.failIfNoveltyRegresses=false`

Warnings do not fail CI. Error-level alerts fail CI only if `failOnError=true`.

## Safe threshold tuning
- Change one threshold at a time.
- Use multi-run world-lab comparisons before tightening fail gates.
- Avoid setting `minPNNC` too high without empirical baseline data.
- Keep novelty-regression fail disabled until stable baseline variance is known.
