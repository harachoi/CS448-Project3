import numpy as np
import matplotlib.pyplot as plt
import pandas as pd

x = []
y_min = []
y_avg = []
y_max = []
ys = []

with open('part1test1-out/time.txt') as f:
    while (True):
        line = f.readline()
        if not line:
            break
        x.append(line)
        for i in range(4):
            ys += f.readline().split(' ')

x = np.array(x, dtype=float)
ys = np.array(ys, dtype=float)

print(x, ys)

fig, ax = plt.subplots(1)
ax.plot()