## Replication Instructions

Below we will explain to to re-run our "stvr-experiment". The experiment runs our Online Search algorithm to its performance in solving a set of testing tasks on the game [Lab Recruits](https://github.com/iv4xr-project/labrecruits).
To run the experiment **you will need the executable of LabRecruits**; the instructions to get them are in a subsection below.

Two setups of the algorithm are evaluated:
   * _Search_: this is just the Online Search algorithm with no modification.
   * _SearchMinus_. Normally the algorithm constructs a model on the fly. The model includes information such as which buttons and doors are discovered so far, which of them are in the same room, and most importantly known connections between buttons and doors (which buttons are known to toggle which doors). The algorithm exploits this information, e.g. to open a door which it unintentionally closed. In the _SearchMinus_ setup, the algorithm does not exploit the model it constructed. This setup is useful to evaluate if having such a model actually makes a difference.


The set of testing tasks are grouped in three groups: _ATEST_ (seven), _DDO_ (two), and _Large-Random_ (ten). See the paper for descriptions of these groups.

The experiment runner can be found in the class [`STVRExperiment`](./src/test/java/stvrExperiment/STVRExperiment.java).
In principle you can set various experiment parameters yourself; they are configured in this class. The _SearchMinus_ setup is configured to run with the budget of 1.2*T where T is the time it took the _Search_ setup to solve the same testing task. In the configuration in `STVRExperiment` this T is based on previous runs, and we explicitly put the numbers in the configuration, in variablels `ATEST_SAruntime`, `DDO_SAruntime`, and `LargeRandom_SAruntime`.

Each run of the test agent on a given testing task is set to be repeated 3 times. This is set in the variable `ATEST_repeatNumberPerRun` and `LargeLevels_repeatNumberPerRun`. You can change these to e.g. 10 times.

The variable `delayBetweenAgentUpateCycles` controls the delay (in ms) inserted between agent updates. The test agent controls the game through a socket interface. For efficiency reason, the control is asynchronous. The delay allows the game to progress (e.g. to allow the in-game player character to move some small distance), without the agent keeps spamming command literally at every game's frame update. In the configuration the delay is set to 100ms, which works well in Mac and Windows. For a Linux VM running in the cloud we found that 200ms delay works.

For convenience, the experiments are not scripted as a main-method, but as a set of Junit tests, so that you can run them separately e.g. using Maven test from the command-line, or from an IDE like Eclipse. The test-methods are:

  * `run_onlineFull_on_ATEST_experiment_Test()` will run the Online Algorithm with _Search_ setup on the ATEST tasks.
  Similarly, `run_onlineMinus_on_ATEST_experiment_Test()` runs the `SearchMinus` setup on ATEST.

  To run this from command-line, using Maven, you can do:

  ```
  > mvn test -Dtest=STVRExperiment#run_onlineFull_on_ATEST_experiment_Test
  >   > mvn test -Dtest=STVRExperiment#run_onlineMinus_on_ATEST_experiment_Test
  ```

  * To run the algorithm on the DDO tasks you can do:

  ```
  > mvn test -Dtest=STVRExperiment#run_onlineFull_on_DDO_experiment_Test
  > mvn test -Dtest=STVRExperiment#run_onlineMinus_on_DDO_experiment_Test
  ```

  * To run the algorith on the Large-Random tasks:

  ```
  > mvn test -Dtest=STVRExperiment#run_onlineFull_on_LargeRandom_experiment_Test
  > mvn test -Dtest=STVRExperiment#run_onlineMinus_on_LargeRandom_experiment_Test
  ```

Results can be found in the data dir in the project-root.

By default, the algorithms will run the game Lab Recruits without graphics. If you want to see the graphics, set the variable `USE_GRAPHICS` to true,  in the class [`STVRExperiment`](./src/test/java/stvrExperiment/STVRExperiment.java).

### Installing Lab Recuits

If you get this project from a replication-zip, it will already contain the Lab Recuits game. Else, you need to first install the game Lab Recuits. You can get a pre-compiled executable from the github home of [Lab Recruits](https://github.com/iv4xr-project/labrecruits). We need version 2.3.3.
If the executables are not there anymore, then you will have to build the game yourself using Unity :) See the README of Lab Recruits for the specific version of Unity that you need.

In the project root, create a directory named `gym`, if it is not already there. Then you need to put Lab Recruits' executable there:

```
(project root)
   |-- gym
        |-- Windows
        |   |-- bin
        |-- Mac
            |-- bin
        |-- Linux  
            |-- bin   
```
   * Windows: put  `LabRecruits.exe` and related files in `gym/Windows/bin`.
   * Mac: put `LabRecruits.app` in `gym/Mac/bin`.
   * Linux: put `LabRecruits` executable and related files in `gym/Linux/bin`.

After this, you are good to go.

### Saved results

Results from our own runs can be found in `./savedresults`.
