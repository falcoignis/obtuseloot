# Trait Projection Performance Report

1. **Scoring method used:** Vectorized genome/ability dot-product projection with bounded LRU cache.
2. **Cache hit rate:** 64.41%
3. **Number of scored genomes:** 11520
4. **Estimated speed improvement:** 2.61x
5. **Bottlenecks still remaining:** branch resolution and mutation phase still run per-artifact and dominate at high lineage counts.

## Projection Metrics
- Optimized scoring enabled: true
- Ability vectors loaded: 24
- Trait vector dimensionality: 9
- Cache size/capacity: 4100/25000
- Cache hits: 7420
- Cache misses: 4100
- Average scoring time: 9.798 us
