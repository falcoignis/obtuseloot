# Trait Projection Performance Report

1. **Scoring method used:** Vectorized genome/ability dot-product projection with bounded LRU cache.
2. **Cache hit rate:** 66.41%
3. **Number of scored genomes:** 128
4. **Estimated speed improvement:** 2.66x
5. **Bottlenecks still remaining:** branch resolution and mutation phase still run per-artifact and dominate at high lineage counts.

## Projection Metrics
- Optimized scoring enabled: true
- Ability vectors loaded: 24
- Trait vector dimensionality: 9
- Cache size/capacity: 129/25000
- Cache hits: 255
- Cache misses: 129
- Average scoring time: 132.225 us
