# Trait Projection Performance Report

1. **Scoring method used:** Vectorized genome/ability dot-product projection with bounded LRU cache.
2. **Cache hit rate:** 74.05%
3. **Number of scored genomes:** 11520
4. **Estimated speed improvement:** 2.85x
5. **Bottlenecks still remaining:** branch resolution and mutation phase still run per-artifact and dominate at high lineage counts.

## Projection Metrics
- Optimized scoring enabled: true
- Ability vectors loaded: 24
- Trait vector dimensionality: 9
- Cache size/capacity: 8970/25000
- Cache hits: 25590
- Cache misses: 8970
- Average scoring time: 12.895 us
