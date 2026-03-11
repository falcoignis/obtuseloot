# Trait Projection Performance Report

1. **Scoring method used:** Vectorized genome/ability dot-product projection with bounded LRU cache.
2. **Cache hit rate:** 59.11%
3. **Number of scored genomes:** 560
4. **Estimated speed improvement:** 2.48x
5. **Bottlenecks still remaining:** branch resolution and mutation phase still run per-artifact and dominate at high lineage counts.

## Projection Metrics
- Optimized scoring enabled: true
- Ability vectors loaded: 24
- Trait vector dimensionality: 9
- Cache size/capacity: 687/25000
- Cache hits: 993
- Cache misses: 687
- Average scoring time: 21.676 us
