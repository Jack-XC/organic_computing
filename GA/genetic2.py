import numpy as np
import random as random
from operator import itemgetter
import math
import time 

def mutation(x):
    if (random.random() <= 1.0/16.0):
        return np.int16(~x)
    return np.int16(x ^ pow(2, random.randint(0, 15)))

def crossover(x,y):
    s = np.int16(pow(2, random.randint(0, 16)) - 1)
    a = np.int16((x & s) + (y & ~s))
    b = np.int16((x & ~s) + (y & s))
    return [a, b]

def fitness(x16, y16, z16, kBool, iBool):
    x = int(x16)
    y = int(y16)
    z = int(z16)
    
    if kBool:
        k = 1
    else:
        k = -1

    if iBool:
        i = 1
    else:
        i = -1

    try:
        exp1 = math.exp(x*y)
    except OverflowError:
        return float('inf')
    try:  
        exp2 = math.exp(z*x)
    except OverflowError:
        return float('inf')
    if z == 0 and x < 0:
        return float('inf')
    return 42 - k * x * y * int(exp1) + i * x * y**2 + k * z**x * i * int(exp2)

random.seed()

g_size = 10000
p_size = 500
mut = 0.5
pop = []

#init
for i in range(0, g_size):
    x = np.int16(random.randint(0, 10))
    y = np.int16(random.randint(0, 10))
    z = np.int16(random.randint(0, 10))
    k = bool(random.getrandbits(1))
    i = bool(random.getrandbits(1))
    f = fitness(x, y, z, k, i)
    pop.append([x, y, z, k, i, f])

gen = 0
startTime = time.time()
lastTime = startTime
while(True):
    gen += 1

    parents = sorted(pop,key=itemgetter(3))[0:p_size]
    if (gen <= 1 or time.time() - lastTime > 10):
        print '####Gen',gen,'####'
        print 'Minimum:', parents[0][5],'with x =', parents[0][0], ' ,y =',parents[0][1], ', z =', parents[0][2], ', k =', parents[0][3],  ', i =', parents[0][4]
        lastTime = time.time()
        print 'Elapsed Time:', int(lastTime - startTime),'s'

    pop = []
    while (len(pop) < g_size):
        
        p1 = random.randint(0, p_size - 1)
        p2 = random.randint(0, p_size - 1)
        while ( p2 == p1 ):
            p2 = random.randint(0, p_size - 1)
        
        p1 = parents[p1]
        p2 = parents[p2]

        x = crossover(p1[0], p2[0])
        y = crossover(p1[1], p2[1])
        z = crossover(p1[2], p2[2])
        k =  [p1[3], p2[3]]
        i =  [p1[4], p2[4]]

        c1 = [x[random.randint(0, 1)], y[random.randint(0, 1)], z[random.randint(0, 1)], k[random.randint(0, 1)], i[random.randint(0, 1)], 0]
        
        if ( random.random() < mut ):
            k = random.randint(0, 4)
            if k < 3:
                c1[k] = mutation(c1[k])
            else:
                c1[k] = not c1[k]

        c1[5] = fitness(c1[0], c1[1], c1[2], c1[3], c1[4])

        pop.append(c1)


