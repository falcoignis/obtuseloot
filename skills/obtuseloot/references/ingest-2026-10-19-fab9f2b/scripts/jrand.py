M=(1<<48)-1
def first_double(seed):
    s=(seed ^ 0x5DEECE66D) & M
    def nxt(bits):
        nonlocal s
        s=(s*0x5DEECE66D + 0xB) & M
        return s >> (48-bits)
    return ((nxt(26) << 27) + nxt(27)) * (2.0**-53)
for ordv in (0,5,9):
  for pressure in (7,8):
    vals=[first_double((20_000_000 + ordv*100_000 + i) ^ pressure ^ ordv) for i in range(500)]
    print(ordv, pressure, "min=%.4f max=%.4f" % (min(vals), max(vals)))
print("---all ordinals, pressure 8")
for ordv in range(10):
    vals=[first_double((20_000_000 + ordv*100_000 + i) ^ 8 ^ ordv) for i in range(500)]
    print(ordv, "min=%.4f max=%.4f" % (min(vals), max(vals)))
