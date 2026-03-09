# Trait Scoring Benchmark

## 1. benchmark setup
- Seed pool size: 2048
- Picks per artifact: 3
- Shared deterministic seed: 20260309

## 2. scoring modes tested
- BASELINE
- PROJECTION_NO_CACHE
- PROJECTION_WITH_CACHE

## 3. workload sizes
- 10000 evaluations
- 100000 evaluations
- 1000000 evaluations

## 4. timing results

### Workload 10000
| mode | total ms | avg ns/artifact | throughput/s |
|---|---:|---:|---:|
| BASELINE | 265.88 | 26587.93 | 37611.05 |
| PROJECTION_NO_CACHE | 159.75 | 15975.34 | 62596.46 |
| PROJECTION_WITH_CACHE | 88.19 | 8819.01 | 113391.37 |

### Workload 100000
| mode | total ms | avg ns/artifact | throughput/s |
|---|---:|---:|---:|
| BASELINE | 1201.64 | 12016.41 | 83219.56 |
| PROJECTION_NO_CACHE | 390.96 | 3909.57 | 255782.55 |
| PROJECTION_WITH_CACHE | 389.20 | 3891.98 | 256938.68 |

### Workload 1000000
| mode | total ms | avg ns/artifact | throughput/s |
|---|---:|---:|---:|
| BASELINE | 6026.82 | 6026.82 | 165924.93 |
| PROJECTION_NO_CACHE | 3739.27 | 3739.27 | 267431.59 |
| PROJECTION_WITH_CACHE | 3153.11 | 3153.11 | 317147.48 |

## 5. cache results

### Workload 10000
| mode | hits | misses | hit rate | size/capacity | evictions |
|---|---:|---:|---:|---:|---:|
| BASELINE | 0 | 0 | 0.00% | 0/25000 | 0 |
| PROJECTION_NO_CACHE | 0 | 0 | 0.00% | 0/25000 | 0 |
| PROJECTION_WITH_CACHE | 7965 | 2035 | 79.65% | 2035/25000 | 0 |

### Workload 100000
| mode | hits | misses | hit rate | size/capacity | evictions |
|---|---:|---:|---:|---:|---:|
| BASELINE | 0 | 0 | 0.00% | 0/25000 | 0 |
| PROJECTION_NO_CACHE | 0 | 0 | 0.00% | 0/25000 | 0 |
| PROJECTION_WITH_CACHE | 97952 | 2048 | 97.95% | 2048/25000 | 0 |

### Workload 1000000
| mode | hits | misses | hit rate | size/capacity | evictions |
|---|---:|---:|---:|---:|---:|
| BASELINE | 0 | 0 | 0.00% | 0/25000 | 0 |
| PROJECTION_NO_CACHE | 0 | 0 | 0.00% | 0/25000 | 0 |
| PROJECTION_WITH_CACHE | 997952 | 2048 | 99.80% | 2048/25000 | 0 |

## 6. parity results
- BASELINE vs PROJECTION_NO_CACHE: top1=100.00%, top3 exact=100.00%, top3 set=100.00%, ordering consistency=100.00%
- BASELINE vs PROJECTION_WITH_CACHE: top1=100.00%, top3 exact=100.00%, top3 set=100.00%, ordering consistency=100.00%

## 7. interpretation
- Performance gain is strongest where throughput improves while parity remains high.
- Cache effectiveness depends on repeated genome state in workload and world sim loops.

## 8. recommendation
- Use **PROJECTION_WITH_CACHE** for simulation and diagnostics; keep live gameplay default unchanged until more live validation.
