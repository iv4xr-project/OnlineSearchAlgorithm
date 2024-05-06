package stvrExperiment;

import static agents.tactics.OnlineSearch.* ;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;
import java.nio.file.Files;

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
import reasoningSupport.Prolog;

import static org.junit.jupiter.api.Assertions.* ;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
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
import leveldefUtil.LRFloorMap;
import leveldefUtil.LRconnectionLogic;
import myUtils.DebugUtil;
import game.LabRecruitsTestServer;


import world.BeliefStateExtended;

public class STVRExperiment {
	
	// ===== common parameters
	
	static String projectRootDir = System.getProperty("user.dir") ;
	
	static String levelsDir = Paths.get(projectRootDir, "src", "test", "resources", "levels", "STVR").toString() ;
				
	static String dataDirRoot =  Paths.get(projectRootDir,"data").toString() ;
	
	// in ms
	// static int delayBetweenAgentUpateCycles = 100 ; 
	static int delayBetweenAgentUpateCycles = 50 ; 
	
	/**
	 * For the purpose of calculating area coverage ({@see #coveredTiles2D}), we pretend the
	 * agent to be a rectangle of size 2 x assumedExtentOfAgent.
	 */
	static float assumedExtentOfAgent = 1 ;
		
	// ====
	
	static int ATEST_repeatNumberPerRun = 3 ;
	static int LargeLevels_repeatNumberPerRun = 2 ;
	
	// ================ ATEST levels =================
	
	static String[] ATEST_levels = { 
		"BM2021_diff1_R3_1_1_H"   // minimum solution: 2
		,"BM2021_diff1_R4_1_1"    // minimum solution: 4
		,"BM2021_diff1_R4_1_1_M"  // minimum solution: 3
		,"BM2021_diff2_R5_2_2_M"  // minimum solution: 2
		,"BM2021_diff2_R7_2_2"    // minimum solution: 4
		,"BM2021_diff3_R4_2_2"    // minimum solution: 0
		,"BM2021_diff3_R4_2_2_M"  // minimum solution: 4
		,"BM2021_diff3_R7_3_3" // minimum solution: 2
	} ;
		
	// runtime of Samira's alg, in seconds:
	static int[] ATEST_SAruntime = { 
		68, 84, 139, 140, 
		146, 60, 144, 254 } ;
		
		
	static String[] ATEST_targetDoors = {
		"door1", "door6", "door5", "door4", 
		"door6", "door6", "door3", "door6"
	} ;
		
		
	// ================ DDO levels =================

	static String[] DDO_levels = { "sanctuary_1"
				// , "durk_1"
		} ;
	static int[] DDO_SAruntime = { 1492, 2680 } ;
	static String[] DDO_targetDoors = { "doorEntrance", "doorKey4",  } ;
		

	// ================ Large-Random level =================

	static String[] LargeRandom_levels = { 
		  "FBK_largerandom_R9_cleaned"   // F1
		  // "FBK_largerandom_R9_cleaned",   // F2
		  //"FBK_largerandom_R9_cleaned",   // F3
		  //"FBK_largerandom_R9_cleaned",  // F4
		  //"FBK_largerandom_R9_cleaned",  // F5
		  //"FBK_largerandom_R9_cleaned",  // F6
		  //"FBK_largerandom_R9_cleaned",  // F7
		  //"FBK_largerandom_R9_cleaned",  // F8  unsolvable by SA
		  //"FBK_largerandom_R9_cleaned",  // F9  unsolvable by SA
		  //"FBK_largerandom_R9_cleaned",  // F10
		  //"FBK_largerandom_R9_cleaned"   // F11
		} ;
		
	static int[] LargeRandom_SAruntime = { 
		   //14,   // F1
		   //113,  // F2
		   //954,  // F3
		   //1045, // F4
		   1076, // F5
		   1827, // F6 
		   1532, 
		   3000, // one hrs (3000 x 1.2)
		   3000, // one hrs
		   1420, // time unknown!
		   1420 			
		} ;
		
	static String[] LargeRandom_targetDoors = {
		  //"door26",  // F1
		  //"door5",   // F2
		  //"door39",  // F3
		  //"door33",  // F4
		  "door16",  // F5
		  "door37",  // F6
		  "door34", "door3", "door21", "door22", "door38"}  ;
		
	
	
	
	// ====
	
	
	enum AlgorithmVariant { OnlineSearch, OnlineMinus } 
	
	
	static class ResultOneRun {
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
		int numOfDoorAttemps ;
		float areaCoverage ;
		
		@Override
		public String toString() {
			String z = "== level:" + level ;
			z +=     "\n== alg  :" + alg ;
			z +=     "\n== goal :" + (goalsolved ? "ACHIEVED" : "X") ;
			z +=     "\n== runtime(sec)  :" + runtime ;
			z +=     "\n== exploraion-time(sec):" + explorationtime ;
			z +=     "\n== #door-attempts:" + numOfDoorAttemps ;	
			z +=     "\n== #turns        :" + numberOfTurns ;
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
	
	/**
	 * Averaged results over multiple runs.
	 */
	static class ResultMultiRuns {
		String level ;
		String alg ;
		int numberOfRuns ;
		int numberOfConnections ;
		float runtime ;
		float explorationtime ;
		float numberOfTurns ;
		int goalsolved ;
		float connectionsInferred ;
		float correctConnections ;
		float wrongConnections ;
		float connectionsPrecision = 0 ;
		float connectionsRecall = 0 ;
		int numberOfDoors ;
		float doorsInferred ;
		int numberOfButtons ;
		float buttonsInferred ;
		float roomsInferred ;
		float numOfDoorAttemps ;
		float areaCoverage ;
		
		ResultMultiRuns(ResultOneRun[] runs) {
			var run0 = runs[0] ;
			List<ResultOneRun> runs_ = new LinkedList<>() ;
			for (int k=0; k<runs.length; k++) runs_.add(runs[k]) ;
			level = run0.level ;
			alg = run0.alg ;
			this.numberOfRuns = runs.length ;
			this.numberOfConnections = run0.numberOfConnections ;
			this.numberOfButtons = run0.numberOfButtons ;
			this.numberOfDoors = run0.numberOfDoors ;
			this.goalsolved = (int) runs_.stream().filter(info -> info.goalsolved).count() ;
			this.runtime = (float) (double) 
					runs_.stream().map(info -> info.runtime) .collect(Collectors.averagingDouble(i -> (double) i)) ;
			this.explorationtime = (float) (double) 
					runs_.stream().map(info -> info.explorationtime) .collect(Collectors.averagingDouble(i -> (double) i)) ;	
			this.numberOfTurns = (float) (double) 
					runs_.stream().map(info -> info.numberOfTurns) .collect(Collectors.averagingDouble(i -> (double) i)) ;
			this.connectionsInferred = (float) (double) 
					runs_.stream().map(info -> info.connectionsInferred) .collect(Collectors.averagingDouble(i -> (double) i)) ;
			this.correctConnections = (float) (double) 
					runs_.stream().map(info -> info.correctConnections) .collect(Collectors.averagingDouble(i -> (double) i)) ;
			this.wrongConnections = (float) (double) 
					runs_.stream().map(info -> info.wrongConnections) .collect(Collectors.averagingDouble(i -> (double) i)) ;
			this.doorsInferred = (float) (double) 
					runs_.stream().map(info -> info.doorsInferred) .collect(Collectors.averagingDouble(i -> (double) i)) ;
			this.buttonsInferred = (float) (double) 
					runs_.stream().map(info -> info.buttonsInferred) .collect(Collectors.averagingDouble(i -> (double) i)) ;
			this.roomsInferred = (float) (double) 
					runs_.stream().map(info -> info.roomsInferred) .collect(Collectors.averagingDouble(i -> (double) i)) ;
			this.numOfDoorAttemps = (float) (double) 
					runs_.stream().map(info -> info.numOfDoorAttemps) .collect(Collectors.averagingDouble(i -> (double) i)) ;
			this.areaCoverage = (float) (double) 
					runs_.stream().map(info -> info.areaCoverage) .collect(Collectors.averagingDouble(i -> (double) i)) ;
			
			if (this.correctConnections > 0) {
				this.connectionsPrecision = this.correctConnections / this.connectionsInferred ;
				this.connectionsRecall = this.correctConnections / (float) this.numberOfConnections ;
			}
		}
		
		@Override
		public String toString() {
			String z = "== level:" + level ;
			z +=     "\n== alg  :" + alg ;
			z +=     "\n== #success on goal:" + goalsolved + "/" + numberOfRuns ;
			z +=     "\n== avrg runtime(sec)  :" + runtime ;
			z +=     "\n== avrg exploraion-time(sec):" + explorationtime ;
			z +=     "\n== avrg #door-attempts:" + numOfDoorAttemps ;	
			z +=     "\n== avrg #turns        :" + numberOfTurns ;
			z +=     "\n== avrg #rooms found  :" + roomsInferred  ;
			z +=     "\n== avrg #doors found  :" + doorsInferred + "/" + numberOfDoors ;
			z +=     "\n== avrg #buttons found:" + buttonsInferred + "/" + numberOfButtons ;
			z +=     "\n== #connections    :" + numberOfConnections ;
			z +=     "\n==    avrg inferred:"    + connectionsInferred ;
			z +=     "\n==    avrg correct :"    + correctConnections ;
			z +=     "\n==    avrg wrong   :"    + wrongConnections ;	
			z +=     "\n==    precision    :"    + connectionsPrecision ;	
			z +=     "\n==    recall       :"    + connectionsRecall ;	
			z +=     "\n== avrg area-cov      :" + areaCoverage ;
			return z ;
		}
	}
	
	Pair<Integer,Integer> get2DTileLocation(Vec3 p) {
		int x = (int) Math.floor(p.x + 0.5) ;
		int y = (int) Math.floor(p.z + 0.5) ;
		return new Pair<>(x,y) ;
	}
	
	Set<Pair<Integer,Integer>> get2DTilesAroundAgent(Vec3 p) {
		Set<Pair<Integer,Integer>> S = new HashSet<>() ;
		S.add(get2DTileLocation(p)) ;
		float xMin = p.x - assumedExtentOfAgent ;
		float xMax = p.x + assumedExtentOfAgent ;
		float yMin = p.z - assumedExtentOfAgent ;
		float yMax = p.z + assumedExtentOfAgent ;
		float x = xMin ;
		while (x < xMax) {
			float y = yMin ;
			while (y < yMax) {
				S.add(new Pair<Integer,Integer>((int) x, (int) y)) ;
				y += 1f ;
			}
			x += 1f ;
		}
		return S ;
	}
	
	Set<Pair<Integer,Integer>> getCoveredTiles2D(List<Vec3> visitedLocations) {		
		Set<Pair<Integer,Integer>> covered = new HashSet<>() ;
		for (var pos : visitedLocations) {
			if (pos != null) {
				if (assumedExtentOfAgent <= 0.5)
					covered.add(get2DTileLocation(pos)) ;
				else {
					covered.addAll(get2DTilesAroundAgent(pos)) ;
				}
			}
		}
		return covered ;	
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
	public void test0() throws InterruptedException, IOException {
		
		executeTestingTask(1,"ATEST","agent0","BM2021_diff3_R4_2_2_M","door3",null,
				5000,null,
				AlgorithmVariant.OnlineMinus) ;
		/*
		executeTestingTask(1,"ATEST","agent0","BM2021_diff3_R4_2_2","door6",null,
				5000,null,
				AlgorithmVariant.OnlineMinus) ;
		
		*/
		/*
		executeTestingTask(1,"ATEST","agent0","BM2021_diff3_R7_3_3","door6",null,
				5000,null,
				AlgorithmVariant.OnlineMinus) ;
		*/
		
		/*
		executeTestingTask(1,"ATEST","agent1","sanctuary_1","doorEntrance",null,
				30000,
				null,
				AlgorithmVariant.OnlineSearch) ;
		*/
		
		// Vec3 goalPosition = new Vec3(106f,0,81f); // guide for Durk DoorKey3		
		//executeTestingTask(1,"agent1","durk_1","doorKey4",new Vec3(67f,0,76f),30000,true) ;
		
		// subdir: "MutatedFiles\\MOSA\\selectedLevels\\1649941005014";

	}
	
	
	public void run_ATEST_experiment_Test() throws IOException {
		String benchMarkName = "ATEST" ;
		Path dataDir = Paths.get(dataDirRoot,benchMarkName) ;
		if (Files.notExists(dataDir)) {
			Files.createDirectories(dataDir) ;
		}
		String reportFileName = benchMarkName + "_results.txt" ;
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");  
		LocalDateTime now = LocalDateTime.now();  
		appendWritelnToFile(dataDir.toString(),reportFileName,">>>> START experiment "
				+ " " + dtf.format(now)
				,true) ;	
		
		runOneLevel(1,"ATEST","agent0","BM2021_diff3_R4_2_2_M","door3",null,
				5000,null,
				AlgorithmVariant.OnlineMinus) ;
		
	
	}
	
	
	public List<Set<Pair<Integer,Integer>>> runOneLevel(
			String benchMarkName,
			String agentName,
			String levelName,
			String targetDoor,
			Vec3 approximateTargetLocation,
			Integer budgetInCycles,  // ignored if null
			Integer budgetInMs,
			int numberOfRuns,
			AlgorithmVariant algVariant
			) throws InterruptedException, IOException {
		
		ResultOneRun[] results = new ResultOneRun[numberOfRuns] ;
		for (int k=0; k<numberOfRuns; k++ ) {
			results[k] = executeTestingTask(k,benchMarkName,agentName,levelName,targetDoor,approximateTargetLocation,
					budgetInCycles,
					budgetInMs,
					algVariant) ;
		}
		ResultMultiRuns combinedResult = new ResultMultiRuns(results) ;
		String dataDir = Paths.get(dataDirRoot,benchMarkName).toString() ;
		String reportFileName = benchMarkName + "_results.txt" ;
		appendWritelnToFile(dataDir,reportFileName,"*********************",true) ;
		appendWritelnToFile(dataDir,reportFileName,"====== " 
				+ levelName + " " + targetDoor + " with "
				+ combinedResult.alg , true) ;
		appendWritelnToFile(dataDir,reportFileName,combinedResult.toString(),true);
		
		return null ;
	}

	/**
	 * A single run of executing a testing task to verify that the target blocker 
	 * can be opened.
	 * 
	 * @param budget Budget expressed in the number of update-cycles to use.
	 * @param exploitModel  If true, the algorithm will track and exploit a model
	 * 		of the game under test. 
	 */
	@SuppressWarnings({ "unchecked", "rawtypes"})
	public ResultOneRun executeTestingTask(
			int runNumber,
			String benchMarkName,
			String agentName,
			String levelName,
			String targetDoor,
			Vec3 approximateTargetLocation,
			Integer budgetInCycles,  // in #cycles, if not null
			Integer budgetInMs, // in milli-seccond, if not null
			AlgorithmVariant algVariant
			) throws InterruptedException, IOException {

		System.out.println(">>> Start a test run ...");
		
		// Launching LR and loading the level:
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
			
			// preparing and creating the test agent; including prepping its state, data collector, etc:
			
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

			var Result = new ResultOneRun() ;
			Result.alg = "" + algVariant ;
			if (algVariant == AlgorithmVariant.OnlineMinus) {
				beliefState.disabledModelTracking = true ;
			}
			Result.level = levelName ;
			
			long startTime = System.currentTimeMillis();
			testAgent.registerEvent(new TimeStampedObservationEvent("startTest"));
			
			// the loop to run the agent:
			int cycleNumber = 0;
			long runTime = 0 ;
			while (testingTask.getStatus().inProgress()) {
				long t0 = System.currentTimeMillis() ;
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
				Thread.sleep(delayBetweenAgentUpateCycles);
				System.out.println("*** " + cycleNumber + ", " + testAgent.getState().id + " @"
						+ testAgent.getState().worldmodel.position);

				cycleNumber++;
				testAgent.update();

				if (beliefState.worldmodel().health <= 0) {
					DebugUtil.log(">>>> the agent died. Aaaw.");
				}
				if (budgetInCycles != null && cycleNumber > budgetInCycles) {
					break;
				}
				runTime = runTime + (System.currentTimeMillis() - t0) ;
				if (budgetInMs != null && runTime > budgetInMs) {
					break ;
				}
			}
			// the test is done now (either achieving the test-goal, or it exhausts the budget)
			
			testAgent.registerEvent(new TimeStampedObservationEvent("endTest"));
			long totalTime = System.currentTimeMillis() - startTime;
			
			// Collecting and analyzing results, and reporting:
			
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
			
			// saving location-visits to a file:
			String dataDir = Paths.get(dataDirRoot , benchMarkName).toString() ;
			String visitInfoFileName = levelName + "_" + Result.alg + "_visits_run" + runNumber + ".csv" ;
			dataCollector.saveTestAgentScalarsTraceAsCSV(
					testAgent.getId(), 
					Paths.get(dataDir,visitInfoFileName).toString() 
					);
			
			// calculating time spent on doing exploration:
			var traceExplore = testAgent.getTestDataCollector().getTestAgentTrace(testAgent.getId()).stream().filter(
					e -> e.getFamilyName() == "startExploreRecorder" || e.getFamilyName() == "endExploreRecorder")
					.collect(Collectors.toList());
			// pattern: [start,end,start,end....]
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
			
			// calculating area-coverage
			var walkableTiles = LRFloorMap.firstFloorWalkableTiles(levelFile) ; 
			var visitedLocations = dataCollector.getTestAgentScalarsTrace(testAgent.getId()).stream()
				.map(ev -> new Vec3((float) ev.values.get("posx"), 0 , (float) ev.values.get("posz")))
				.collect(Collectors.toList()) ;
			var visitedTiles = getCoveredTiles2D(visitedLocations) ;		
			int covered = (int) visitedTiles.stream().filter(tile -> walkableTiles.contains(tile)).count() ;
			Result.areaCoverage = (float) covered / (float) walkableTiles.size() ;

			// get information on attempts on doors:
			List<String> traceTriedDoors = testAgent.getTestDataCollector().getTestAgentTrace(testAgent.getId())
					.stream()
					.map(e -> e.getFamilyName())
					.filter(e -> e.toLowerCase().contains("door")) //fragile! assuming door-names start with doors!
					.collect(Collectors.toList());
			;
			Result.numOfDoorAttemps = traceTriedDoors.size() ;
			
			// construct a summary report:
			StringBuilder sb = new StringBuilder();			
			sb.append("\n===============") ;
			sb.append("\n== Inferred MODEL:") ;
			sb.append("\n" + prolog.toString());
			sb.append("\n== Ground-truth connection logic: " + LRconnectionLogic.parseConnections(levelFile)) ;
			sb.append("\n== MODEL's high-level nav-graph edges: ");
			if (! beliefState.highLevelGragh.edges.isEmpty())
				beliefState.highLevelGragh.getEntityConnections().forEach(e -> sb.append(e.toString()));
			sb.append("\n==	attempts on doors:" + traceTriedDoors) ;
			sb.append("\n===============") ;
			sb.append("\n" + Result) ;
			String reportFileName = levelName + "_" + Result.alg + "_all-info_run" + runNumber + ".txt" ;
			writelnToFile(dataDir,reportFileName, sb.toString(),true) ; // echo is true, to also print to screen
	        
	        return Result ;
			
		} finally {
			environment.close();
		}

	}
	
	/**
	 * Append the given string to a file. Create the file if it does not 
	 * exists.
	 */
	void appendWritelnToFile(String dir, String fname, String s, boolean echo) throws IOException {
	    Files.writeString(
	        Path.of(dir, fname),
	        s + "\n",
	        CREATE, APPEND
	    );
	    if (echo) {
	    	System.out.println(s) ;
	    }
	}
	
	/**
	 * Write the given string to a file. Create the file if it does not 
	 * exists.
	 */
	void writelnToFile(String dir, String fname, String s, boolean echo) throws IOException {
	    Files.writeString(
	        Path.of(dir, fname),
	        s + "\n",
	        CREATE, WRITE
	    );
	    if (echo) {
	    	System.out.println(s) ;
	    }
	}

}