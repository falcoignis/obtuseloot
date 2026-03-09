# Trait Projection Performance Report

1. **Scoring method used:** Vectorized genome/ability dot-product projection with bounded projection caching.
2. **Cache hit rate:** Pending runtime simulation execution (reported by world simulation harness output).
3. **Number of scored genomes:** Pending runtime simulation execution.
4. **Estimated speed improvement:** Estimated from cache hit ratio in runtime stats (`1.0 + hitRate * 2.5`).
5. **Bottlenecks still remaining:** ability branch resolution and mutation phase remain per-artifact hotspots.

## Notes
- This report is overwritten automatically by `WorldSimulationHarness` after simulation runs.
- Use `/obtuseloot debug projection stats` and `/obtuseloot debug projection cache` for live diagnostics.
