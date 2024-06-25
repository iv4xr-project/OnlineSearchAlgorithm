# Online Search Algorithm

For [**experiments replication**, see here](./experiment_replication.md).

This project contains the implementation of the algorithm presented in [_An online agent-based search approach in automated computer game testing with model construction_](https://dl.acm.org/doi/abs/10.1145/3548659.3561309) (also available on [Zenodo](https://zenodo.org/records/7230140)).
The implementation can be found in the class [`OnlineSearch`](./src/main/java/agents/tactics/OnlineSearch.java). It is built on top of the agent programming framework [iv4xr/aplib](https://github.com/iv4xr-project/aplib). In our studies, we use the game [Lab Recruits](https://github.com/iv4xr-project/labrecruits) as our experiment subject. The game is highly configurable, allowing users to define their own game levels.  The algorithm leverage a set of tactics already available for automating navigation (path-finding) and exploration on Lab Recruits. This is provided by the project [iv4xrDemo](https://github.com/iv4xr-project/iv4xrDemo). This means that the algorithm can control the game at the high level, e.g. to instruct the test agent to navigate to a certain game object without having to actually steer the agent to do the navigation (the steering is done by the underlying tactics from iv4xrDemo).

An example setup to run the algorithm can be found in the method `executeTestingTask` in the class [`STVRExperiment`](./src/test/java/stvrExperiment/STVRExperiment.java). In this method you can specify which target game-level in the game Lab Recruits that you want to try the algorithm. The testing task to accomplish is to verify that a given door in this level can be opened (and hence verifying that the area behind the door is reachable from the player starting position). The algorithm uses a test agent, that controls the in-game player's avatar. The algorithm and the testing task it has to do are encoded as a so-called 'goal structure' for the agent:

```java
// Instantiate the Online-search algorithm here:
GoalStructure testingTask = onlineSearchAlgorithm1(testAgent,
   targetDoor,
   approximateTargetLocation, // if any
   (BeliefStateExtended B) -> ! GoalLibExtended.isDoorClosedPredicate(B,targetDoor)) ;
```

This goal structure is given to agent to accomplish:

```java
testAgent.setGoal(testingTask);
```

The agent is then ran in a while-loop (inside the said method `executeTestingTask`), where at every iteration the agent performs an `agent.update()`, that will select and do an action (e.g. move some small distance to a certain direction) steered by the aforementioned goal structure:

```java
while(testingTask.getStatus().inProgress()) {
  ...
  testAgent.update()
  ...
  //break the execution if the budget runs out, or if no-progress is detected
}
```

The task is successfully completed if the loop ends with the `testingTask` has a `Success` status. This means that the test passes. Else the test fails.

### Installing the game Lab Recruits

The algorithm is in this project configured to target the game Lab Recruits as example. You can get a pre-compiled executable from the github home of [Lab Recruits](https://github.com/iv4xr-project/labrecruits). We need version 2.3.3.
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
