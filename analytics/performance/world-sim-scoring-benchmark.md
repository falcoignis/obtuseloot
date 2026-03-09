# World Simulation Scoring Benchmark

| mode | players | seasons | total runtime ms | scoring runtime ms (est) | scoring calls | cache hit rate | cache size | evictions |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| BASELINE | 100 | 2 | 2146 | 89.80 | 44800 | 0.00% | 0 | 0 |
| PROJECTION_NO_CACHE | 100 | 2 | 954 | 57.48 | 44800 | 0.00% | 0 | 0 |
| PROJECTION_WITH_CACHE | 100 | 2 | 929 | 43.60 | 44800 | 60.50% | 17696 | 0 |
