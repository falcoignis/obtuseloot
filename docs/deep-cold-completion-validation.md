# Deep cold completion validation

## Objective
This pass completes the cold-start system by adding a bounded first-exposure fallback on top of the existing cold-template boost. The goal is to make never-selected templates eventually surface under ordinary category selection without changing novelty tuning, similarity tuning, category balancing, randomness policy, or template definitions.

## Implementation summary
The generator now keeps using lifetime template usage as the run-local "seen vs unseen" signal and adds a small fallback inside category-local weighted sampling:

1. Weighted selection still runs normally inside the chosen category.
2. If the provisional winner has already been seen, the generator checks for unseen templates in that category.
3. With a bounded `12%` probability, selection is rerouted to an unseen template **only** when that unseen candidate still clears the same safety gates used elsewhere in generator scoring:
   - novelty floor
   - same-niche similarity ceiling
   - niche compatibility / adjacency
   - existing registry / gating validity inherited from the selected category pool
4. The reroute remains weighted by the category sampling scores rather than switching to uniform random choice.
5. Once every template in a category has been seen at least once, the override naturally disables for that category because no unseen candidates remain.

## Validation work performed
### Completed checks
- `mvn -q -DskipTests compile`
- `mvn -q -DskipTests test-compile`
- Reduced custom probe run against compiled classes to confirm the new fallback executes without breaking novelty ordering metrics.

### Reduced probe snapshot
A reduced runtime probe was executed because the full cold-completion probe did not finish within the bounded local execution window. That reduced probe produced the following live results:

- **Stealth:** `5` hits, `5 / 9` reachable, top-share `0.20`
- **Defense:** `3` hits, `3 / 8` reachable, top-share `0.3333`
- **Ritual:** `0` hits in the reduced sample
- **Novelty:** average novelty `0.4804`, average similarity `0.5196`, intra-niche novelty `0.6585`, global novelty `0.4804`

Interpretation:
- The reduced run is consistent with the protected scoring properties: novelty remains comfortably above floor expectations, similarity remains below collapse thresholds, and intra-niche novelty still exceeds global novelty.
- The reduced sample is **too small** to prove bounded first exposure for every template, so it cannot certify the full reachability target.

## Requested validation criteria status
1. **Reachability: all templates reachable** — not fully proven locally.
2. **Stealth: 9 / 9 reachable without supplemental probing** — not proven locally.
3. **Defense / ritual previously missing templates now appear** — not proven locally.
4. **Distribution: no template above 50% and no uniform randomness** — design preserved; reduced sample stayed below `50%`, but full proof is still pending.
5. **Stability: novelty ordering preserved and niche divergence unchanged/improved** — partially supported by the reduced probe metrics, but not fully re-measured with the full battery.

## Conclusion
The code path for bounded unseen-template exposure is now implemented and compiled successfully, and the reduced live probe shows the protected novelty/similarity behavior remains intact. However, the full ordinary-pressure validation battery required to certify complete cold completion did not finish inside the available execution window, so this pass must be recorded as partial pending a longer probe run.

COLD_COMPLETION_RESULT: PARTIAL
