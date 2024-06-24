#
# For performing t-test, wilcoxon-test, and  other stats on collected data.
# The data are copied explicitly into the tables in this script.
#  
import numpy as np  
from scipy import stats
#
successRate = {
    "Search"    : [1,1,1,1, 
                   1,1,1, 
                   0.9,1, 
                   1,1,1,0.9,1,  
                   0.8,0.9,0.9,0,0.8 ],
    "SearchMin" : [0,.8,0,0,
                   1,0.8,1,
                   0.9,0.5,
                   1,1,1,0.9,1,
                   0.9,1,0.9,0,0.7],
    "Random"    : [0.7,0.8,1,0.7,
                   1,0.6,1,
                   0.3,0.3,
                   1,1,1,0.3,0.2,
                   0.2,0.4,0,0,0],
    "Evo"       : [1,0.1,0.1,1,
                   0.4,0.5,0.8,
                   0.3,0.3,
                   1,0,1,0.2,0.8,
                   0.3,0.7,0,0,0],
    "MCTS"      : [1,0,0.5,1,
                   0.6,0.3,1,
                   0.1,0,
                   1,0.1,1,0.7,0.9,
                   0.9,0.7,0.1,0,0.1],
    "Q"         : [1,0,0.6,0.8,
                   0.9,0,1,
                   0.1,0,
                   1,0.2,1,0.4,0.3,
                   0.1,0.4,0,0,0.1]
}

physicalCoverage = {
    "Search"    : [ 0.68, 0.64, 0.76, 0.58,
                    0.69, 0.6,  0.62,
                    0.59, 0.78,
                    0.018, 0.041, 0.147, 0.093, 0.081,
                    0.099, 0.067, 0.147, 0.20 ,  0.122
                      ],
    "Random"    : [ 0.52, 0.67, 0.78, 0.7,
                    0.65, 0.59, 0.65, 
                    0.6,  0.74,
                    0.015, 0.170, 0.05, 0.197, 0.2,
                    0.204, 0.184, 0.21, 0.21,  0.21  
                  ],
    "Evo"       : [ 0.74, 0.56, 0.76, 0.79,
                    0.76, 0.67, 0.7,
                    0.55, 0.45,
                    0.125, 0.240, 0.124, 0.234, 0.219,
                    0.225, 0.220, 0.236, 0.235, 0.230
                      ],
    "MCTS"      : [ 0.66, 0.6,  0.81, 0.77,
                    0.76, 0.67, 0.71,
                    0.6, 0.57,
                    0.124, 0.268, 0.126, 0.249, 0.228,
                    0.253, 0.265, 0.283, 0.309, 0.296
                      ],
    "Q"         : [ 0.77, 0.63, 0.82, 0.76,
                    0.78, 0.67, 0.71,
                    0.6, 0.58,
                    0.124, 0.236, 0.124, 0.232, 0.235,
                    0.235, 0.217, 0.224, 0.212, 0.209
                      ]
}

modelRecall = {
   "Search"    : [  0.5, 0.83, 0.98, 0.83,
                    0.45, 0.58, 0.29,
                    0.64, 0.79,
                    0.05
                 ],
   "Random"    : [ 0.52, 0.84, 0.91, 0.5,
                   0.7, 0.59, 0.55,
                   0.6, 0.65,
                   0.06
                 ],
   "Evo"       : [ 0.57, 0.76, 0.78, 0.56,
                  0.67, 0.64, 0.46,
                  0.33, 0.31,
                  0.06
                 ],
   "MCTS"      : [ 0.42, 0.83, 0.71, 0.36,
                   0.69, 0.54, 0.43,
                   0.36, 0.43,
                   0.07  
                 ],
   "Q"         : [ 0.65, 0.81, 0.84, 0.51, 
                   0.74, 0.59, 0.51,
                   0.35, 0.43,
                   0.05  ]  
}

modelPrecision = {
   "Search"    : [ 1, 0.98, 0.98, 1,
                   1, 1, 1,
                   0.84, 0.9,
                   0.97
                  ],
   "Random"    : [ 1, 0.84, 0.91, 0.71,
                  0.81, 0.79, 0.84,
                  0.8, 0.72,
                  0.87       
                  ],
   "Evo"       : [ 1, 0.76, 1, 1,
                   1, 0.97, 1,
                   0.97, 0.90,
                   0.98
                 ],
   "MCTS"      : [ 1, 0.83, 1, 1,
                   1, 0.9, 1,
                   0.89, 0.77,
                   0.88       
                 ],
   "Q"         : [ 1, 0.87, 1, 0.93,
                   1, 0.59, 1,
                   0.95, 0.75,
                   0.91   
                 ]  
    }

experimentTime = [
    # OnlineSearch: ATEST/DDO/Large-Rnd:
    3.77, 11.55, 12.45,
    # SearchMin:
    3.27, 10.07, 13.95,
    # ATEST-Gym:
    154996.0/3600.0, 
    # DDO-Gym:
    312373.0/3600.0,
    # Large-Random: Rnd/Evo/MCTS/Q:
    67.86, 106.53, 79.86, 112.01, 
    # Mutation test:
    62
    ]

def check(data, expectedCount):
    for x in data:
       if (len(data[x]) != expectedCount) : return False
    return True 

def compare(title, data, numberOfProps):
    print("======================")
    print(f"*** {title}") ;
    ok = check(data,numberOfProps) 
    if not ok : 
        print("=== the rows are not all of the same length! Aborting.")
        return
    allTotal = 0.0
    N = 0.0
    for alg in data:
        print(f"    {alg} mean: {np.mean(data[alg])}, med: {np.median(data[alg])}, stdev: {np.std(data[alg])}")
        for alg2 in data:
            if alg2==alg : continue
            ttest = stats.ttest_ind(data[alg],data[alg2])
            wcx   = stats.wilcoxon(data[alg],data[alg2])
            print(f"      ** t-test   {alg} vs {alg2}, p={ttest.pvalue}")
            print(f"      ** wilcoxon {alg} vs {alg2}, p={wcx.pvalue}")
        allTotal +=  np.sum(data[alg])
        N += numberOfProps
    print(f"=== overall avrg: {allTotal/N}")
    print("***") ;

def avrgAreaCov_LargeRandom() :
    print("======================")
    for alg in physicalCoverage:
        largeRandom = physicalCoverage[alg][9:]
        print(f"== {alg} Large-Random area-cov, n={len(largeRandom)}, mean: {np.mean(largeRandom)}")

compare("Success-rate",successRate,19)
compare("Physical coverage",physicalCoverage,19)
compare("Model recall",modelRecall,10)
compare("Model precision",modelPrecision,10)

avrgAreaCov_LargeRandom()
print(f"== Total experiment time: {sum(experimentTime)} hrs ({sum(experimentTime)/24.0} days)")

