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
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import game.Platform;
import gameTestingContest.DebugUtil;
import gameTestingContest.Prolog;
import game.LabRecruitsTestServer;


import world.BeliefStateExtended;

public class Experiment0 {
	
	// ===== common parameters
	
	static String projectRootDir = System.getProperty("user.dir") ;
	
	static String levelsDir = Paths.get(projectRootDir, "src", "test", "resources", "levels", "STVR").toString() ;
				
	static String dataDir =  Paths.get(projectRootDir,"data").toString() ;
	
	

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
		executeTestingTask("agent0","BM2021_diff1_R3_1_1_H","door1",null,5000) ;
		//executeTestingTask("agent0","BM2021_diff3_R7_3_3","door6",null,5000) ;
		//executeTestingTask("agent1","sanctuary_1","doorEntrance",null,30000) ;
		
		// Vec3 goalPosition = new Vec3(106f,0,81f); // guide for Durk DoorKey3		
		//executeTestingTask("agent1","durk_1","doorKey4",new Vec3(67f,0,76f),30000) ;
		
		// subdir: "MutatedFiles\\MOSA\\selectedLevels\\1649941005014";

	}
	
	@SuppressWarnings({ "unchecked", "rawtypes"})
	public void executeTestingTask(String agentName,
			String levelName,
			String targetDoor,
			Vec3 approximateTargetLocation,
			int budget // in #cycles
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

			// saving location-visits to a csv file:
			String visitInfoFileName = levelName + "_" + "visits.csv" ;
			dataCollector.saveTestAgentScalarsTraceAsCSV(
					testAgent.getId(), 
					Paths.get(dataDir,visitInfoFileName).toString() 
					);
			
			System.out.println("****** testing task status: " + testingTask.getStatus());
			System.out.println("******run time******");
			System.out.println(totalTime / 1000);
			System.out.println("******cycle number******");
			System.out.println(cycleNumber);

			/// *************************** data collector
			long sumMiliSec = 0;
			long sumMinutes = 0;

			// trace the information about exploration time
			var traceExplore = testAgent.getTestDataCollector().getTestAgentTrace(testAgent.getId()).stream().filter(
					e -> e.getFamilyName() == "startExploreRecorder" || e.getFamilyName() == "endExploreRecorder")
					.collect(Collectors.toList());
			;
			// System.out.println("trace2222 " + traceExplore + traceExplore.size()) ;

			// [start,end,start,end....]
			for (int i = 0; i < traceExplore.size(); i++) {

				if (traceExplore.get(i) != null && i % 2 == 0 && i < traceExplore.size() - 1) {
					// System.out.println("trace " + i + traceExplore.get(i)) ;
					var diff = Duration
							.between(traceExplore.get(i).getTimestamp(), traceExplore.get(i + 1).getTimestamp())
							.toMillis();
					sumMiliSec = sumMiliSec + diff;
					// System.out.println("diff milliis " + diff) ;
					var diff2 = Duration
							.between(traceExplore.get(i).getTimestamp(), traceExplore.get(i + 1).getTimestamp())
							.toMinutes();
					// System.out.println("diff min " + diff2) ;
					sumMinutes = sumMinutes + diff2;
				}
			}
			System.out.println("sum (ms) :" + sumMiliSec + ", in sec:" + sumMiliSec / 1000);
			System.out.println("sum (min):" + sumMinutes);

			// trace the name of the tried doors
			List<String> traceTriedDoors = testAgent.getTestDataCollector().getTestAgentTrace(testAgent.getId())
					.stream().map(e -> e.getFamilyName()).collect(Collectors.toList());
			;

			traceTriedDoors.forEach(e -> {
				if (e != null && e.contains("door"))
					System.out.println("traceTriedDoors  " + e);
			});

			// trace the position of the agent
			List<Map<String, Number>> tracePosition = testAgent.getTestDataCollector()
					.getTestAgentScalarsTrace(testAgent.getId()).stream().map(event -> event.values)
					.collect(Collectors.toList());

			// System.out.println("tracePosition " + tracePosition) ;

			// save the recorded data
			// save the position
			String projectRootDir = System.getProperty("user.dir");
			try {
				testAgent.getTestDataCollector().saveTestAgentScalarsTraceAsCSV(testAgent.getId(), projectRootDir
						+ File.separator + "data" + File.separator + levelName + "_positionTraceViewDis.csv");
				// testAgent.getTestDataCollector()
				// .saveTestAgentEventsTraceAsCSV(testAgent.getId(),projectRootDir
				// +File.separator+"data"+File.separator+"FBK"+File.separator+levelName+
				// "EventTraceViewDis.csv");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			var agentneTimeStamss = testAgent.getState().knownEntities();
			// print the prolog data
			prolog.report();
			System.out.println("get all connection : ");
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
			sb.append(sumMiliSec / 1000);
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
			
			System.out.println("****** testing task status: " + testingTask.getStatus());


		} finally {
			environment.close();
		}

	}
}