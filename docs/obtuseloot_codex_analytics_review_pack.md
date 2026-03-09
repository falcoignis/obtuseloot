
# ObtuseLoot Codex Analytics Review Pack

This document combines:

1. Quick Review Checklist – fast sanity check after a Codex run
2. Full Review Rubric – deeper evaluation of analytics quality

Use the checklist first, then the rubric if you want a deeper evaluation.

Typical files to review:
- BUILD_INFO.md
- population-report.md
- meta-report.md
- balance-suggestions.md
- meta-analysis.json
- evolution-report.md

---
# PART 1 — Quick Review Checklist

## 1. Is the Data Large Enough?
- Sample size shown (preferably thousands of seeds)
- Percentages or counts included
- Distinction between generated vs progressed artifacts

If not, the analysis may be unreliable.

## 2. Do Path Distributions Make Sense?
The report should distinguish between:
- common paths
- rare but healthy paths
- dead or unreachable paths

Warning signs:
- “Rare = buff it”
- “Common = nerf it”

## 3. Does It Analyze Progression Timing?
Look for metrics such as:
- time to awakening
- time to fusion
- time to major evolutions

If timing is absent, pacing issues may be hidden.

## 4. Does It Understand System Interactions?
Examples:
- drift influencing evolution
- awakening amplifying reputation
- chaos increasing drift probability

Weak reports treat systems independently.

## 5. Are Outlier Seeds Identified?
Good reports mention:
- unusually strong seeds
- unusually weak seeds
- interesting rare lineages

Bad reports treat all unusual seeds as bugs.

## 6. Are Balance Suggestions Specific?
Good suggestions:
- small adjustments
- backed by evidence
- aware of side effects

Bad suggestions:
- “Buff X”
- “Nerf Y”
- no explanation

## 7. Is the Confidence Level Reasonable?
Healthy reports:
- acknowledge uncertainty
- frame conclusions as hypotheses

Red flag:
- strong certainty with weak data.

## 8. Can You Decide What To Do Next?
After reading the reports you should know:
- what to tune first
- what to leave alone
- what needs more testing

If not, the report is not useful.

Quick verdict guide:
- Analysis feels grounded in real patterns → trust cautiously
- Mixed signals → run another simulation pass
- Mostly speculation → ignore suggestions and gather more data

---
# PART 2 — Full Review Rubric

Score each category from 1 to 5.

## 1. Data Quality
Pass if:
- sample size clearly stated
- thousands of seeds used
- counts and percentages included

Red flags:
- vague claims without numbers

## 2. Path Distribution Quality
Good analysis:
- identifies dominant paths
- recognizes healthy rare paths
- identifies dead paths with evidence

Red flags:
- rare paths automatically labeled bad
- common paths automatically labeled broken

## 3. Progression Pacing
Check whether analysis evaluates:
- time to awakening
- time to fusion
- timing of major evolutions

Good reports discuss early, mid, and late progression.

## 4. Interaction Awareness
Strong analysis recognizes:
- drift influencing evolution
- awakening affecting later reputation growth
- chaos influencing mutation frequency

Weak analysis treats each system independently.

## 5. Seed / Outlier Analysis
Good analysis:
- identifies unusual seeds
- distinguishes “cool rare” vs “broken outlier”

## 6. Suggestion Quality
Good suggestions are:
- specific
- minimal
- justified with evidence

Bad suggestions:
- vague
- sweeping changes
- unsupported conclusions

## 7. False Confidence Detection
Healthy reports:
- acknowledge uncertainty
- avoid strong claims without evidence

## 8. Practical Usefulness
After reading the analysis you should know:
- what to tune first
- what to test next
- what should remain unchanged

---
# Scorecard

| Category | Score |
|---|---|
| Data quality | |
| Path distribution | |
| Progression pacing | |
| Interaction awareness | |
| Seed/outlier analysis | |
| Suggestion quality | |
| Confidence control | |
| Practical usefulness | |

Score interpretation:
- 34–40 → Excellent analysis
- 26–33 → Good but review suggestions manually
- 18–25 → Exploratory signal only
- 8–17 → Mostly noise

---
# Recommended Review Order

1. BUILD_INFO.md
2. population-report.md
3. meta-report.md
4. balance-suggestions.md
5. meta-analysis.json
6. evolution-report.md

Review evidence first, recommendations second.
