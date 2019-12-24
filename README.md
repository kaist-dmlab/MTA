# MTA: Mobile MapReduce Task Allocation

This tutorial introduces the reader to the basic concept of optimizing MapReduce task allocation in a mobile cloud and the features of a mobile MapReduce **Simulator**, which is organized as follows.

1. Overview
2. Resources: Algorithms, Faultloads, and Workloads
3. How to Run **Simulator**

## 1. Overview
Mobile cloud computing has become a widespread phenomenon owing to the rapid development and proliferation of mobile devices all over the globe. Furthermore, revolutionary mobile hardware technologies, such as 5G and IoT, have led to increased competition for mobile intelligence among tech-giants, like Google, Apple, and Facebook, further leading to developments in the field of mobile cloud computing. However, several challenges still remain; above all, resolving the task allocation problem that determines the nodes on which tasks will be executed is of paramount importance, and therefore, is the focus of many previous studies on mobile cloud. To this end, we propose a novel **Mobile MapReduce Task Allocation(MTA)** strategy that simultaneously maximizes both job speed and reliability by modelling communication delay and task reliability.

## 2. Resources: Algorithms, Faultloads, and Workloads

### 2-1. Algorithms
**MTA** is compared with state-of-the-art task allocation algorithms. We provide unofficial implementations of the algorithms in this repository because no implementation is officially provided.

### 2-2. Faultloads
We evaluated *three* faultload data sets that encompass various real situations, such as a conference, village, and race, thus exhibiting different statistics.

|  Data Set |  Category  | # Nodes | Duration | # Faultloads | Average Group Size | Average NCD |
|:---------:|:----------:|:-------:|:--------:|:------------:|:---------------:|:--------:|
|   [haggle](https://crawdad.org/cambridge/haggle/20090529)  | Conference |    78   |  3 days  |      455     |       19.2      |   0.18   |
|    [pmtr](https://crawdad.org/unimi/pmtr/20081201)   |   Village  |    44   |  19 days |      188     |       11.0      |   0.39   |
| [rollernet](https://crawdad.org/upmc/rollernet/20090202) |    Race    |    62   |  3 hours |      99      |       21.4      |   0.60   |

#### Setting a faultload in a simulator
```java
Simulator s = new Simulator();
s.addFaultloads("haggle");
```

### 2-3. Workloads
We evaluated *three* popular workloads in mobile cloud computing: Naïve Bayes Classifier, SIFT Feature Extraction, and Join, which belong to three representative application scenarios, namely Decision Making, Information Retrieval, and Sensor Preprocessing, respectively.

<table>
  <tr>
    <th rowspan="2">Workload</th>
    <th rowspan="2">Category</th>
    <th rowspan="2">Map<br>Selectivity</th>
    <th rowspan="2">Reduce<br>Selectivity</th>
    <th rowspan="2">Map<br>Throughput</th>
    <th rowspan="2">Reduce<br>Throughput</th>
    <th colspan="3">Input Data Size(MB)</th>
  </tr>
  <tr>
    <th>haggle</th>
    <th>pmtr</th>
    <th>rollernet</th>
  </tr>
  <tr>
    <td align="center" valign="center">Naïve Bayes</td>
    <td align="center" valign="center">Decision<br>Making</td>
    <td align="center" valign="center">0.04</td>
    <td align="center" valign="center">0.9</td>
    <td align="center" valign="center">0.3</td>
    <td align="center" valign="center">0.3</td>
    <td align="center" valign="center">35</td>
    <td align="center" valign="center">40</td>
    <td align="center" valign="center">55</td>
  </tr>
  <tr>
    <td align="center" valign="center">SIFT</td>
    <td align="center" valign="center">Information<br>Retrieval</td>
    <td align="center" valign="center">1.5</td>
    <td align="center" valign="center">1</td>
    <td align="center" valign="center">0.1</td>
    <td align="center" valign="center">4.2</td>
    <td align="center" valign="center">10</td>
    <td align="center" valign="center">13</td>
    <td align="center" valign="center">20</td>
  </tr>
  <tr>
    <td align="center" valign="center">Join</td>
    <td align="center" valign="center">Sensor<br>Preprocessing</td>
    <td align="center" valign="center">0.6</td>
    <td align="center" valign="center">2</td>
    <td align="center" valign="center">2</td>
    <td align="center" valign="center">1</td>
    <td align="center" valign="center">60</td>
    <td align="center" valign="center">70</td>
    <td align="center" valign="center">100</td>
  </tr>
</table>

#### Setting a workload in a simulator
```java
Simulator s = new Simulator();
s.addWorkloads(JobProfile.of("NAIVE_BAYES", 35));
```

## 3. How to Run Simulator

### 3-1. Process Overview
1. Fautloads are prepared in the path *TaskAllocationSim/faultload* and automatically configured for a simulation.
2. Run a simulation by setting a configuration in *TaskAllocationSim/src/com.mobilemr.task_allocation.Main*.
3. *Job Speed* and *Job Completion Rate* are printed in console in CSV format.

### 3-2. Simulation

#### Running a simulation
```java
JobProfile naiveBayes = JobProfile.of("NAIVE_BAYES", 35);
naiveBayes.addMap(0.04F, 0.3F);
naiveBayes.addReduce(0.9F, 0.3F);

JobProfile sift = JobProfile.of("SIFT", 10);
sift.addMap(1.5F, 0.1F);
sift.addReduce(1, 4.2F);

JobProfile join = JobProfile.of("JOIN", 60);
join.addMap(0.6F, 2);
join.addReduce(2, 1);

Simulator s = new Simulator();
s.addWorkloads(naiveBayes, sift, join);
s.addFaultloads("haggle");
s.run();
```

#### Running a simulation with heterogeneity
```java
JobProfile naiveBayes = JobProfile.of("NAIVE_BAYES", 20);
naiveBayes.addMap(0.04F, 0.3F, 0.23F, 0.20F, 0.13F, 0.07F);
naiveBayes.addReduce(0.9F, 0.3F, 0.23F, 0.20F, 0.13F, 0.07F);

JobProfile sift = JobProfile.of("SIFT", 6);
sift.addMap(1.5F, 0.1F, 0.08F, 0.07F, 0.04F, 0.02F);
sift.addReduce(1, 4.2F, 3.23F, 2.77F, 1.79F, 1.02F);

JobProfile join = JobProfile.of("JOIN", 55);
join.addMap(0.6F, 2, 1.54F, 1.32F, 0.85F, 0.49F);
join.addReduce(2, 1, 0.77F, 0.66F, 0.43F, 0.24F);

Simulator s = new Simulator();
s.addWorkloads(naiveBayes, sift, join);
s.addFaultloads("rollernet");
s.run();
```

### Setting different parameters
The parameters introduced in the paper can be set to different values in *TaskAllocationSim/src/com.mobilemr.task_allocation.Params* as follows.
```java
// Genetic Algorithm Parameters
public static Selector SURVIVORS_SELECTOR = new TournamentSelector<>(5);
public static Selector OFFSPRING_SELECTOR = new RouletteWheelSelector();
public static Alterer CROSSOVER = new SinglePointCrossover<>(0.16);
public static Alterer MUTATOR = new Mutator<>(0.115);
public static int MAX_GENERATIONS = 2000;
public static int STEADY_GENERATIONS = 5;
public static int POPULATION_SIZE = 200;

// Environment Parameters
public static float MAX_CLUSTER_UTILIZATION = 0.5F;
public static int MAX_LINK_BANDWIDTH = 5;
public static int NUM_TRIALS = 30;
```