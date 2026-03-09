# Memory System Report

## 1) Scope / sample size
- Memory events tracked: 10 types across 5000 artifacts

## 2) Method summary
- Counted memory event influence occurrences in deterministic generation output.

## 3) Key findings
- Highest memory event: PLAYER_DEATH_WHILE_BOUND=529
- Lowest memory event: LONG_BATTLE=474

## 4) Dominant families / branches / mechanics
- Memory PLAYER_DEATH_WHILE_BOUND: 529
- Memory MULTIKILL_CHAIN: 520
- Memory FIRST_KILL: 518
- Memory AWAKENING: 516
- Memory CHAOS_RAMPAGE: 510

## 5) Rare but viable systems
- LONG_BATTLE: 474
- FUSION: 476
- FIRST_BOSS_KILL: 480

## 6) Dead or suspicious systems
- Dead memory types: []

## 7) Confidence / caveats
- Counts are generator-level influence markers, not direct world progression effect sizes.

## 8) Suggested next review steps
- Validate memory-driven branches in world-lab multi-run data for long-horizon viability.
