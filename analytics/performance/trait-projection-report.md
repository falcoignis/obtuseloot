# Trait Projection Performance Report

1. **Scoring method used:** Vectorized genome/ability dot-product projection with bounded LRU cache.
2. **Cache hit rate:** 76.18%
3. **Number of scored genomes:** 17920
4. **Estimated speed improvement:** 2.90x
5. **Bottlenecks still remaining:** branch resolution and mutation phase still run per-artifact and dominate at high lineage counts.

## Projection Metrics
- Optimized scoring enabled: true
- Ability vectors loaded: 24
- Trait vector dimensionality: 9
- Cache size/capacity: 12806/25000
- Cache hits: 40954
- Cache misses: 12806
- Average scoring time: 6.693 us
