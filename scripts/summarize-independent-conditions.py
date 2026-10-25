#!/usr/bin/env python3
"""Read-only postprocessing of the fixed Java experiment protocol."""
from pathlib import Path
import argparse,csv,collections,json,math
parser=argparse.ArgumentParser(description="Summarize Java-generated independent-condition CSVs; no simulation or model tuning.")
parser.add_argument('input',type=Path,help='target/independent-conditions directory')
parser.add_argument('--output',type=Path,help='Write JSON here; otherwise print it')
args=parser.parse_args()
source=args.input
def read(name):
 rows=list(csv.DictReader((source/name).open()))
 assert all(None not in row and all(value is not None for value in row.values()) for row in rows),name
 return rows
generations=read('generations.csv');activities=read('activities.csv');births=read('parentage.csv')
assert len(generations)==1536 and len(activities)==49152 and len(births)==24576
assert all(int(r['opportunities'])==int(r['unavailable'])+int(r['attempts']) for r in activities)
assert all(int(r['attempts'])==int(r['cooldown_suppressions'])+int(r['successes']) for r in activities)
assert all((r['benefit_mean']=='' and r['recovery_mean']=='')==(int(r['successes'])==0) for r in activities)
by_run=collections.defaultdict(list)
for r in births:by_run[(r['seed'],r['arm'],r['selection'],r['changing'])].append(r)
paired={}
for arm in sorted({r['arm'] for r in generations}):
 for selection in ['measured','neutral']:
  counts={'before_target_swap':0,'after_target_before_ceiling':0,'after_ceiling_swap':0}
  totals=dict.fromkeys(counts,0)
  for seed in ['722','1722','2722','3722']:
   steady=by_run[(seed,arm,selection,'false')];changed=by_run[(seed,arm,selection,'true')]
   for a,b in zip(steady,changed):
    assert (a['generation'],a['site'])==(b['generation'],b['site'])
    generation=int(a['generation']);phase='before_target_swap' if generation<4 else 'after_target_before_ceiling' if generation<6 else 'after_ceiling_swap'
    totals[phase]+=1;counts[phase]+=(a['parent_seed'],a['parent_revision'])!=(b['parent_seed'],b['parent_revision'])
  assert counts['before_target_swap']==0
  paired[arm+'_'+selection]={'changed_parent_choices':counts,'births_compared':totals}
# Check the controlled input stream, without constraining inherited genes or outcomes.
activity_runs=collections.defaultdict(list)
for row in activities:activity_runs[(row['seed'],row['arm'],row['selection'],row['changing'])].append(row)
for seed in ['722','1722','2722','3722']:
 for arm in ['BOTH_REGULAR','VARIED_REGULAR','VARIED_OPPORTUNITIES']:
  baseline=activity_runs[(seed,arm,'neutral','false')]
  for selection in ['measured','neutral']:
   for changing in ['false','true']:
    paired_rows=activity_runs[(seed,arm,selection,changing)]
    assert len(baseline)==len(paired_rows)
    for a,b in zip(baseline,paired_rows):
     assert all(a[k]==b[k] for k in ['generation','site','activity','input_seed','loadout','opportunities','unavailable','attempts'])
last_groups=collections.defaultdict(list)
for row in activities:
 if row['generation']!='7':continue
 site=int(row['site']);changing=row['changing']=='true';rich=(site%2==0)^changing;low=(site<2)^changing
 last_groups[(row['arm'],row['selection'],row['changing'],rich,low,row['activity'])].append(row)
last=[]
for key,rows in sorted(last_groups.items()):
 successes=sum(int(x['successes']) for x in rows);offers=sum(int(x['opportunities']) for x in rows);attempts=sum(int(x['attempts']) for x in rows)
 benefit=math.fsum(float(x['benefit_total']) for x in rows)
 # Recovery mean came from an integer sum / at most 11 successes; recover that sum per item.
 recovery=sum(round(float(x['recovery_mean'])*int(x['successes'])) for x in rows if x['recovery_mean'])
 last.append(dict(zip(['arm','selection','changing','rich','low','activity'],key))|{'items':len(rows),'opportunities':offers,'unavailable':sum(int(x['unavailable']) for x in rows),'attempts':attempts,'cooldown_suppressions':sum(int(x['cooldown_suppressions']) for x in rows),'successes':successes,'benefit_total':benefit,'benefit_per_item':benefit/len(rows),'benefit_per_offered_opportunity':benefit/offers if offers else None,'mean_completed_benefit':benefit/successes if successes else None,'mean_completed_recovery':recovery/successes if successes else None,'unobserved_items':sum(int(x['successes'])==0 for x in rows),'unique_reports_total':sum(int(x['unique_reports']) for x in rows)})
population=[]
for arm in sorted({r['arm'] for r in generations}):
 for selection in ['measured','neutral']:
  for changing in ['false','true']:
   rows=[r for r in generations if (r['arm'],r['selection'],r['changing'],r['generation'])==(arm,selection,changing,'7')]
   population.append({'arm':arm,'selection':selection,'changing':changing,'births':len(rows)*16,'cross_target':sum(int(x['cross_target_parents']) for x in rows),'cross_ceiling':sum(int(x['cross_ceiling_parents']) for x in rows),'fresh_founders':sum(int(x['founder_births']) for x in rows),'unobserved_current_items':sum(int(x['unobserved_current_items']) for x in rows),'mean_newborn_founder_simpson':math.fsum(float(x['newborn_founder_simpson']) for x in rows)/len(rows),'mean_precision_variance':math.fsum(float(x['precision_variance']) for x in rows)/len(rows),'mean_kinetic_variance':math.fsum(float(x['kinetic_variance']) for x in rows)/len(rows)})
summary={'rows':{'generations':len(generations),'activities':len(activities),'births':len(births)},'parent_choice_changes':paired,'last_generation_population':population,'last_generation_activity':last,'fresh_founder_births':sum(int(r['founder_births']) for r in generations),'initial_founders_per_run':64,'run_count':48,'paired_input_streams_aligned':True,'context_counterexample':read('context-counterexample.csv') if (source/'context-counterexample.csv').exists() else None}
encoded=json.dumps(summary,indent=2)
if args.output:
 args.output.write_text(encoded+'\n')
 print(json.dumps({'output':str(args.output),'rows':summary['rows'],'paired_input_streams_aligned':True}))
else:print(encoded)
