# Trait Projection Performance Report

1. **Scoring method used:** Vectorized genome/ability dot-product projection with bounded LRU cache.
2. **Cache hit rate:** 54.68%
3. **Number of scored genomes:** 9600
4. **Estimated speed improvement:** 2.37x
5. **Bottlenecks still remaining:** branch resolution and mutation phase still run per-artifact and dominate at high lineage counts.

## Projection Metrics
- Optimized scoring enabled: true
- Ability vectors loaded: 24
- Trait vector dimensionality: 9
- Cache size/capacity: 4351/25000
- Cache hits: 5249
- Cache misses: 4351
- Average scoring time: 31.352 us
