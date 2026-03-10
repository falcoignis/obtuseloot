# Trait Projection Performance Report

1. **Scoring method used:** Vectorized genome/ability dot-product projection with bounded LRU cache.
2. **Cache hit rate:** 42.60%
3. **Number of scored genomes:** 960
4. **Estimated speed improvement:** 2.07x
5. **Bottlenecks still remaining:** branch resolution and mutation phase still run per-artifact and dominate at high lineage counts.

## Projection Metrics
- Optimized scoring enabled: true
- Ability vectors loaded: 24
- Trait vector dimensionality: 9
- Cache size/capacity: 551/25000
- Cache hits: 409
- Cache misses: 551
- Average scoring time: 37.848 us
