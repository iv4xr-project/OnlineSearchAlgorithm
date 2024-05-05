package stvrExperiment;

import static agents.tactics.OnlineSearch.* ;
import agents.LabRecruitsTestAgent;
import agents.TestSettings;
import agents.tactics.GoalLibExtended;

import static agents.TestSettings.*;
import au.com.bytecode.opencsv.CSVReader;
import environments.LabRecruitsConfig;
import environments.LabRecruitsEnvironment;
import eu.iv4xr.framework.mainConcepts.ObservationEvent.ScalarTracingEvent;
import eu.iv4xr.framework.mainConcepts.ObservationEvent.TimeStampedObservationEvent;
import eu.iv4xr.framework.mainConcepts.ObservationEvent.VerdictEvent;
import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.mainConcepts.TestDataCollector;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import eu.iv4xr.framework.mainConcepts.WorldModel;
import eu.iv4xr.framework.spatial.Vec3;
import nl.uu.cs.aplib.mainConcepts.ProgressStatus;
import nl.uu.cs.aplib.utils.Pair;

import static org.junit.jupiter.api.Assertions.* ;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import game.Platform;
import gameTestingContest.DebugUtil;
import gameTestingContest.Prolog;
import leveldefUtil.LRFloorMap;
import leveldefUtil.LRconnectionLogic;
import game.LabRecruitsTestServer;


import world.BeliefStateExtended;

public class STVRExperiment {
	
	// ===== common parameters
	
	static String projectRootDir = System.getProperty("user.dir") ;
	
	static String levelsDir = Paths.get(projectRootDir, "src", "test", "resources", "levels", "STVR").toString() ;
				
	static String dataDir =  Paths.get(projectRootDir,"data").toString() ;
	
	
	static class ResultSA {
		String level ;
		String alg ;
		int numberOfConnections ;
		int runtime ;
		int explorationtime ;
		int numberOfTurns ;
		boolean goalsolved ;
		int connectionsInferred ;
		int correctConnections ;
		int wrongConnections ;
		int numberOfDoors ;
		int doorsInferred ;
		int numberOfButtons ;
		int buttonsInferred ;
		int roomsInferred ;
		float areaCoverage ;
		
		@Override
		public String toString() {
			String z = "== level:" + level ;
			z +=     "\n== alg  :" + alg ;
			z +=     "\n== goal :" + (goalsolved ? "ACHIEVED" : "X") ;
			z +=     "\n== runtime(sec):" + runtime ;
			z +=     "\n== exploraion-time(sec):" + explorationtime ;
			z +=     "\n== #turns      :" + numberOfTurns ;
			z +=     "\n== #rooms found  :" + roomsInferred  ;
			z +=     "\n== #doors found  :" + doorsInferred + "/" + numberOfDoors ;
			z +=     "\n== #buttons found:" + buttonsInferred + "/" + numberOfButtons ;
			z +=     "\n== #connections  :" + numberOfConnections ;
			z +=     "\n==    inferred:"    + connectionsInferred ;
			z +=     "\n==    correct :"    + correctConnections ;
			z +=     "\n==    wrong   :"    + wrongConnections ;	
			z +=     "\n== area-cov      :" + areaCoverage ;
			return z ;
		}
	}
	
	
	private static LabRecruitsTestServer labRecruitsTestServer;

	@BeforeAll
	static public void start() {
		// TestSettings.USE_SERVER_FOR_TEST = false ;
		// Uncomment this to make the game's graphic visible:
		// TestSettings.USE_GRAPHICS = true ;
		String projectRootDir = System.getProperty("user.dir");
		labRecruitsTestServer = TestSettings.start_LabRecruitsTestServer(projectRootDir);
	}

	@AfterAll
	static void close() {
		if (labRecruitsTestServer != null)
			labRecruitsTestServer.close();
	}



	@Test
	public void executeTestingTask() throws InterruptedException, IOException {
		//executeTestingTask("agent0","BM2021_diff1_R3_1_1_H","door1",null,5000,true) ;
		executeTestingTask("agent0","BM2021_diff3_R7_3_3","door6",null,5000,true) ;
		//executeTestingTask("agent1","sanctuary_1","doorEntrance",null,30000,true) ;
		
		// Vec3 goalPosition = new Vec3(106f,0,81f); // guide for Durk DoorKey3		
		//executeTestingTask("agent1","durk_1","doorKey4",new Vec3(67f,0,76f),30000,true) ;
		
		// subdir: "MutatedFiles\\MOSA\\selectedLevels\\1649941005014";

	}

	/**
	 * Execute a tetsing task to verify that the target blocker can be opened.
	 * 
	 * @param budget Budget expressed in the number of update-cycles to use.
	 * @param exploitModel  If true, the algorithm will track and exploit a model
	 * 		of the game under test. 
	 */
	@SuppressWarnings({ "unchecked", "rawtypes"})
	public void executeTestingTask(String agentName,
			String levelName,
			String targetDoor,
			Vec3 approximateTargetLocation,
			int budget,  // in #cycles,
			boolean exploitModel
			) throws InterruptedException, IOException {

		System.out.println(">>> start testing...");
		
		var LRconfig = new LabRecruitsConfig(levelName, levelsDir);
		LRconfig.agent_speed = 0.1f;
		LRconfig.view_distance = 4f;
		var environment = new LabRecruitsEnvironment(LRconfig);

		try {
			if (TestSettings.USE_GRAPHICS) {
				System.out.println(
						"You can drag then game window elsewhere for beter viewing. Then hit RETURN to continue.");
				new Scanner(System.in).nextLine();
			}
			var beliefState = new BeliefStateExtended();

			var prolog = new Prolog(beliefState);
			var testAgent = new LabRecruitsTestAgent(agentName) 
					.attachState(beliefState)
					.attachEnvironment(environment);

			beliefState.highLevelGragh.goalPosition = approximateTargetLocation;

			var testingTask = onlineSearchAlgorithm1(testAgent, 
					targetDoor, 
					approximateTargetLocation,
					(BeliefStateExtended B) -> ! GoalLibExtended.isDoorClosedPredicate(B,targetDoor)
					);

			var dataCollector = new TestDataCollector();
			testAgent
				.setTestDataCollector(dataCollector)
				.setGoal(testingTask);

			var Result = new ResultSA() ;
			Result.alg = "online-search" ;
			if (! exploitModel) Result.alg = "online-search WITHOUT model" ;
			Result.level = levelName ;
			
			long startTime = System.currentTimeMillis();
			testAgent.registerEvent(new TimeStampedObservationEvent("startTest"));
			
			int cycleNumber = 0;
			while (testingTask.getStatus().inProgress()) {
				if (testAgent.getState().worldmodel.position != null) {
					dataCollector.registerEvent(testAgent.getId(),
						new ScalarTracingEvent(
							new Pair<String,Number>("posx", testAgent.getState().worldmodel.position.x),
							new Pair<String,Number>("posy", testAgent.getState().worldmodel.position.y),
							new Pair<String,Number>("posz", testAgent.getState().worldmodel.position.z),
							new Pair<String,Number>("turn", cycleNumber), 
							new Pair<String,Number>("tick", 1)));
				}
				if (cycleNumber == 1)
					Thread.sleep(1500);
				Thread.sleep(100);
				System.out.println("*** " + cycleNumber + ", " + testAgent.getState().id + " @"
						+ testAgent.getState().worldmodel.position);

				cycleNumber++;
				testAgent.update();

				if (beliefState.worldmodel().health <= 0) {
					DebugUtil.log(">>>> the agent died. Aaaw.");
				}
				if (cycleNumber > budget) {
					break;
				}
			}

			testAgent.registerEvent(new TimeStampedObservationEvent("endTest"));
			long totalTime = System.currentTimeMillis() - startTime;
			
			// Collecting and calculating results:
			
			Result.goalsolved = testingTask.getStatus().success() ;
			Result.numberOfTurns = cycleNumber ;
			Result.runtime = (int) (totalTime/1000) ;
			String levelFile = Paths.get(levelsDir, levelName + ".csv").toString() ;
			Result.numberOfButtons = LRFloorMap.getButtonsInFirstFloor(levelFile).size() ;
			Result.numberOfDoors   = LRFloorMap.getDoorsInFirstFloor(levelFile).size() ;
						
			var Z = LRconnectionLogic.compareConnection( LRconnectionLogic.parseConnections(levelFile), prolog.getConnections()) ;
			Result.numberOfConnections = Z.get("#connections") ;
			Result.connectionsInferred = Z.get("#inferred") ;
			Result.correctConnections = Z.get("#correct") ;
			Result.wrongConnections = Z.get("#wrong") ;
			Result.buttonsInferred = prolog.buttons().size() ;
			Result.doorsInferred = prolog.doors().size() ;
			Result.roomsInferred = prolog.rooms().size() ;
			

			// saving location-visits to a csv file:
			String visitInfoFileName = levelName + "_" + "visits.csv" ;
			dataCollector.saveTestAgentScalarsTraceAsCSV(
					testAgent.getId(), 
					Paths.get(dataDir,visitInfoFileName).toString() 
					);
			
			// calculating time spent on doing exploration:
			var traceExplore = testAgent.getTestDataCollector().getTestAgentTrace(testAgent.getId()).stream().filter(
					e -> e.getFamilyName() == "startExploreRecorder" || e.getFamilyName() == "endExploreRecorder")
					.collect(Collectors.toList());
		
			// [start,end,start,end....]
			long explorationTime = 0;
			for (int i = 0; i < traceExplore.size(); i++) {
				if (traceExplore.get(i) != null && i % 2 == 0 && i < traceExplore.size() - 1) {
					// System.out.println("trace " + i + traceExplore.get(i)) ;
					var diff = Duration
							.between(traceExplore.get(i).getTimestamp(), traceExplore.get(i + 1).getTimestamp())
							.toMillis();
					explorationTime = explorationTime + diff;
					// System.out.println("diff milliis " + diff) ;
				}
			}
			Result.explorationtime = (int) (explorationTime/1000) ;

			// trace the name of the tried doors
			List<String> traceTriedDoors = testAgent.getTestDataCollector().getTestAgentTrace(testAgent.getId())
					.stream().map(e -> e.getFamilyName()).collect(Collectors.toList());
			;

			traceTriedDoors.forEach(e -> {
				if (e != null && e.contains("door"))
					System.out.println("traceTriedDoors  " + e);
			});


			// Saving event-trace; disabled for now:
			//try {
				//String eventTraceFile = Paths.get(dataDir, levelName + "_eventTrace.csv").toString() ;
				//		+ File.separator + "data" + File.separator + levelName + "_positionTraceViewDis.csv");
				// testAgent.getTestDataCollector()
				// .saveTestAgentEventsTraceAsCSV(testAgent.getId(),eventTraceFile);
			//} catch (IOException e1) {
				// TODO Auto-generated catch block
			//	e1.printStackTrace();
			//	}

			// print the prolog data
			System.out.println("== Inferred MODEL:") ;
			prolog.report();
			System.out.println("== Ground-truth connection logic: " + LRconnectionLogic.parseConnections(levelFile)) ;

			System.out.println("== MODEL's high-level nav-graph edges: ");
			if (!beliefState.highLevelGragh.edges.isEmpty())
				beliefState.highLevelGragh.getEntityConnections().forEach(e -> System.out.print(e.toString()));

			String newFileLocation = projectRootDir + File.separator + "data" + File.separator + levelName
					+ "_all-info.csv";
			BufferedWriter br = new BufferedWriter(new FileWriter(newFileLocation));
			StringBuilder sb = new StringBuilder();

			// add all connections
			for (ArrayList<String> a : beliefState.highLevelGragh.getEntityConnections()) {
				for (String b : a) {
					sb.append(b);
					sb.append(",");
				}
				sb.append("\n");
			}

			// add tried doors

			for (String a : traceTriedDoors) {
				if (a != null && a.contains("door")) {
					sb.append("Tried Door");
					sb.append(a);
					sb.append(",");
					sb.append("\n");
				}
			}

			// add exploration
			sb.append("exploration time");
			sb.append(",");
			sb.append(Result.explorationtime);
			sb.append(",");
			sb.append("\n");

			// add cycle Number
			sb.append("cycleNumber");
			sb.append(",");
			sb.append(cycleNumber);
			sb.append(",");
			sb.append("\n");

			// add cycle Number
			sb.append("Run time");
			sb.append(",");
			sb.append(totalTime / 1000);
			sb.append(",");
			sb.append("\n");

			// add test status
			sb.append("test status");
			sb.append(",");
			sb.append(testAgent.success());
			sb.append(",");
			sb.append("\n");

			br.write(sb.toString());
			br.close();
			
			System.out.println("\n== SUMMARY:\n" + Result);


		} finally {
			environment.close();
		}

	}
}