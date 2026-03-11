# Trait Projection Performance Report

1. **Scoring method used:** Vectorized genome/ability dot-product projection with bounded LRU cache.
2. **Cache hit rate:** 51.93%
3. **Number of scored genomes:** 0
4. **Estimated speed improvement:** 2.30x
5. **Bottlenecks still remaining:** branch resolution and mutation phase still run per-artifact and dominate at high lineage counts.

## Projection Metrics
- Optimized scoring enabled: true
- Ability vectors loaded: 24
- Trait vector dimensionality: 9
- Cache size/capacity: 12922/25000
- Cache hits: 13958
- Cache misses: 12922
- Average scoring time: 0.000 us
