M=(1<<48)-1
def doubles(seed,n=2):
    s=(seed ^ 0x5DEECE66D) & M
    out=[]
    def nxt(bits):
        nonlocal s
        s=(s*0x5DEECE66D + 0xB) & M
        return s >> (48-bits)
    for _ in range(n):
        out.append(((nxt(26) << 27) + nxt(27)) * (2.0**-53))
    return out
cats=[("TRAVERSAL",6,9),("SENSING",8,10),("SURVIVAL",8,14),("COMBAT",6,8),("DEFENSE",8,5),("RESOURCE",7,8),("CRAFTING",7,5),("SOCIAL",7,8),("RITUAL",8,12),("STEALTH",7,9)]
for o,(name,p,n) in enumerate(cats):
    rolls=[]; seconds=[]
    for i in range(500):
        seed=(20_000_000 + o*100_000 + i) ^ p ^ o
        d=doubles(seed)
        rolls.append(d[0]); seconds.append(d[1])
    rs=sorted(rolls)
    q=lambda f: rs[int(f*(len(rs)-1))]
    # bucket by uniform-weight index
    from collections import Counter
    idx=Counter(min(n-1,int(r*n)) for r in rolls)
    lt12=sum(1 for x in seconds if x<0.12)
    print(f"{name:9s} p={p} n={n} p5={q(.05):.3f} p50={q(.5):.3f} p95={q(.95):.3f} uniformIdx={dict(sorted(idx.items()))} second<0.12={lt12}")
