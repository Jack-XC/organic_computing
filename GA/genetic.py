import numpy as np
import random as random
from operator import itemgetter
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

def fitness(x, y, z):
    x = np.int64(x)
    y = np.int64(y)
    z = np.int64(z)
    return 4 + x*x + y*y + z*z


random.seed()

g_size = 10000
p_size = 50
mut = 0.1
pop = []

#init
for i in range(0, g_size):
    x = np.int16(random.randint(-32768, 32767))
    y = np.int16(random.randint(-32768, 32767))
    z = np.int16(random.randint(-32768, 32767))
    f = fitness(x, y, z)
    pop.append([x, y, z, f])

gen = 0
startTime = time.time()
lastTime = startTime
while(True):
    gen += 1

    parents = sorted(pop,key=itemgetter(3))[0:p_size]
    if (gen <= 1 or time.time() - lastTime > 10):
        print '####Gen',gen,'####'
        print 'Minimum:', parents[0][3],'with x=', parents[0][0], 'y=',parents[0][1], 'z=', parents[0][2]
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

        c1 = [x[random.randint(0, 1)], y[random.randint(0, 1)], z[random.randint(0, 1)], 0]
        
        if ( random.random() < mut ):
            k = random.randint(0, 2)
            c1[k] = mutation(c1[k])

        c1[3] = fitness(c1[0], c1[1], c1[2])

        pop.append(c1)

print parents
print pop
