package stvrExperiment;

import static agents.tactics.OnlineSearch.* ;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;
import java.nio.file.Files;

import agents.LabRecruitsTestAgent;
import agents.TestSettings;
import agents.tactics.GoalLibExtended;
import agents.tactics.TacticLib;

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
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.*;
import game.Platform;
import leveldefUtil.LRFloorMap;
import leveldefUtil.LRconnectionLogic;
import myUtils.DebugUtil;
import game.LabRecruitsTestServer;
import world.BeliefState;
import world.BeliefStateExtended;
import world.LRNavGraphRepair;

public class STVRExperiment {
	
	// ===== common parameters
	
	static boolean USE_GRAPHICS = false ;
	
	static String projectRootDir = System.getProperty("user.dir") ;
	
	static String levelsDir = Paths.get(projectRootDir, "src", "test", "resources", "levels", "STVR").toString() ;
				
	static String dataDirRoot =  Paths.get(projectRootDir,"data").toString() ;
	
	// in ms
	static int delayBetweenAgentUpateCycles = 100 ; 
	//static int delayBetweenAgentUpateCycles = 50 ; 
	
	
	/**
	 * For the purpose of calculating area coverage ({@see #coveredTiles2D}), we pretend the
	 * agent to be a rectangle of size 2 x assumedExtentOfAgent.
	 */
	static float assumedExtentOfAgent = 1 ;
		
	// ====
	
	//static int ATEST_repeatNumberPerRun = 10 ;
	static int ATEST_repeatNumberPerRun = 3 ;
	//static int LargeLevels_repeatNumberPerRun = 5 ;
	static int LargeLevels_repeatNumberPerRun = 3 ;
	
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
		
	static String[] ATEST_targetDoors = {
		"door3", //"door1", 
		"door6", "door5", "door4", 
		"door6", "door6", "door3", "door6"
	} ;
	
	static Vec3[] ATEST_guidingLocations = {
		null, null, null, null,
		null, null, null, null
	} ;
	
	static int[] ATEST_fullOnline_budget = {
		6000,6000,6000,6000,
		6000,6000,6000,6000
	} ;

	// runtime of Samira's alg, in seconds:
	/* from orignal paper:
	static int[] ATEST_SAruntime = { 
		68, 84, 139, 140, 
		146, 60, 144, 254 } ;
	*/
	static int[] ATEST_SAruntime = { 
		75, 107, 135, 160, 
		135, 379, 127, 215 } ;
		
	// ================ DDO levels =================

	static String[] DDO_levels = { 
			"sanctuary_1"
		  , "durk_1"
		} ;
	static String[] DDO_targetDoors = { "doorEntrance", "doorKey4",  } ;
	//static Vec3[] DDO_guidingLocaations = { null, new Vec3(67f,0,76f) } ;	
	static Vec3[] DDO_guidingLocations = { null, null } ;	
	static int[] DDO_fullOnline_budget = { 30000,30000 } ;
	//static int[] DDO_SAruntime = { 1492, 2680 } ; original
	static int[] DDO_SAruntime = { 1538, 2483 } ;

	// ================ Large-Random level =================

	static String[] LargeRandom_levels = { 
		  "FBK_largerandom_R9_cleaned",   
		  "FBK_largerandom_R9_cleaned",   
		  "FBK_largerandom_R9_cleaned",   
		  "FBK_largerandom_R9_cleaned",  
		  "FBK_largerandom_R9_cleaned",  
		  
		  "FBK_largerandom_R9_cleaned",  
		  "FBK_largerandom_R9_cleaned",  
		  "FBK_largerandom_R9_cleaned",
		  "FBK_largerandom_R9_cleaned",  
		  "FBK_largerandom_R9_cleaned"
		} ;
		
	/* Original targets
	static String[] LargeRandom_targetDoors = {
		  "door26",  // F1
		  "door5",   // F2
		  "door39",  // F3
		  "door33",  // F4
		  "door16",  // F5
		  "door37",  // F6
		  "door34",  // F7
		  "door3",   // F8 unsolvable by SA
		  "door21",  // F9 unsolvable by SA
		  "door22",  // F10
		  "door38"   // F11
		  }  ;
	*/
	static String[] LargeRandom_targetDoors = { // new targets, 10x
			  "door17",  
			  "door12",   
			  "door5",  
			  "door39",  
			  "door2",
			  // ----
			  "door33",  
			  "door16",  
			  "door30",   
			  "door15",  
			  "door9"  
	} ;
		
	static Vec3[] LargeRandom_guidingLocations = { 
			null, 
			null, 
			null, 
			null, 
			new Vec3(107,0,104), //d2
			// ----
			new Vec3(12,0,124),  //d33
			new Vec3(84,0,81),   //d16 
			new Vec3(104,0,156), //d30
			new Vec3(144,0,61),  // d15
			new Vec3(144,0,118)  //d9
			} ;	
	
	static int[] LargeRandom_fullOnline_budget = {
			30000,30000,30000,30000,30000,
			30000,30000,30000,30000,30000
		} ;	
	
	/*
	// from the orig. experiment:
	static int[] LargeRandom_SAruntime = {  
		   14,   // F1
		   113,  // F2
		   954,  // F3
		   1045, // F4
		   1076, // F5
		   1827, // F6 
		   1532, 
		   3000, // one hrs (3000 x 1.2)
		   3000, // one hrs
		   1420, // time unknown!
		   1420 			
		} ;
	*/
	
	static int[] LargeRandom_SAruntime = {  
			   47,   // d17 solved
			   82,   // d12 solved
			   162,  // d5  solved
			   333,  // d39 solved
			   460,  // d2  ... mostly Online could not solve
			   // ==
			   453,  // d33 Online could not solve 
			   217,  // d16 solved
			   460,  // d30 not solved
			   470,  // d15 not solved
			   445,  // d9  not solved			
			} ;
		
	
	// ====
	
	
	enum AlgorithmVariant { OnlineSearch, OnlineMinus } 
	
	
	/**
	 * Result of a single run on a level.
	 */
	static class ResultOneRun {
		String level ;
		String goalName ;
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
		/**
		 * Well... for the LargeRandom experiment we need to bubble up area coverage
		 * information :(  Piggy-backing it here.
		 */
		Set<Pair<Integer,Integer>> visitedTiles ;
		
		@Override
		public String toString() {
			String z = "== level:" + level ;
			z +=     "\n== alg  :" + alg ;
			z +=     "\n== goal " + goalName + ":" + (goalsolved ? "ACHIEVED" : "X") ;
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
		float stdDevRuntime ;
		float explorationtime ;
		float numberOfTurns ;
		int goalsolved ;
		float connectionsInferred ;
		float stdDevConnectionsInferred ;
		float correctConnections ;
		float wrongConnections ;
		float connectionsPrecision = 0 ;
		float connectionsRecall = 0 ;
		int numberOfDoors ;
		float doorsInferred ;
		float stdDevDoorsInferred ;
		int numberOfButtons ;
		float buttonsInferred ;
		float stdDevButtonsInferred ;
		float roomsInferred ;
		float stdDevRoomsInferred ;
		float numOfDoorAttemps ;
		float areaCoverage ;
		float stdDevAreaCoverage ;
		/**
		 * Well... for the LargeRandom experiment we need to bubble up area coverage
		 * information :(  Piggy-backing it here.
		 */
		List<Set<Pair<Integer,Integer>>> visitedTiles ;
		
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
			this.runtime = avrg(runs_.stream().map(info -> (float) info.runtime) .collect(Collectors.toList())) ;
			this.stdDevRuntime = stdDev(runs_.stream().map(info -> (float) info.runtime) .collect(Collectors.toList())) ;
			this.explorationtime = avrg(runs_.stream().map(info -> (float) info.explorationtime) .collect(Collectors.toList())) ; 
			this.numberOfTurns = avrg(runs_.stream().map(info -> (float) info.numberOfTurns) .collect(Collectors.toList())) ;
			this.connectionsInferred = avrg(runs_.stream().map(info -> (float) info.connectionsInferred) .collect(Collectors.toList())) ;			
			this.stdDevConnectionsInferred = stdDev(runs_.stream().map(info -> (float) info.connectionsInferred) .collect(Collectors.toList())) ;			
			this.correctConnections = avrg(runs_.stream().map(info -> (float) info.correctConnections) .collect(Collectors.toList())) ;			
			this.wrongConnections = avrg(runs_.stream().map(info -> (float) info.wrongConnections) .collect(Collectors.toList())) ;			
			this.doorsInferred = avrg(runs_.stream().map(info -> (float) info.doorsInferred) .collect(Collectors.toList())) ;			
			this.stdDevDoorsInferred = stdDev(runs_.stream().map(info -> (float) info.doorsInferred) .collect(Collectors.toList())) ;			
			this.buttonsInferred = avrg(runs_.stream().map(info -> (float) info.buttonsInferred) .collect(Collectors.toList())) ;			
			this.stdDevButtonsInferred = stdDev(runs_.stream().map(info -> (float) info.buttonsInferred) .collect(Collectors.toList())) ;			
			this.roomsInferred = avrg(runs_.stream().map(info -> (float) info.roomsInferred) .collect(Collectors.toList())) ;			
			this.stdDevRoomsInferred = stdDev(runs_.stream().map(info -> (float) info.roomsInferred) .collect(Collectors.toList())) ;			
			this.numOfDoorAttemps = avrg(runs_.stream().map(info -> (float) info.numOfDoorAttemps) .collect(Collectors.toList())) ;			
			this.areaCoverage = avrg(runs_.stream().map(info -> (float) info.areaCoverage) .collect(Collectors.toList())) ;			
			this.stdDevAreaCoverage = stdDev(runs_.stream().map(info -> (float) info.areaCoverage) .collect(Collectors.toList())) ;			
			
			if (this.correctConnections > 0) {
				this.connectionsPrecision = this.correctConnections / this.connectionsInferred ;
				this.connectionsRecall = this.correctConnections / (float) this.numberOfConnections ;
			}
		}
		
		float avrg(List<Float> values) {
			double avrg = values.stream().collect(Collectors.averagingDouble(v -> (double) v)) ;
			return (float) avrg ;
		}
		
		float stdDev(List<Float> values) {
			double avrg = values.stream().collect(Collectors.averagingDouble(v -> (double) v)) ;
			double d = values.stream().map(v -> (v - avrg)*(v - avrg)).collect(Collectors.averagingDouble(v -> (double) v)) ;
			return (float) Math.sqrt(d) ;
		}
		
		@Override
		public String toString() {
			String z = "== level:" + level ;
			z +=     "\n== alg  :" + alg ;
			z +=     "\n== #success on goal:" + goalsolved + "/" + numberOfRuns ;
			z +=     "\n== avrg runtime(sec)  :" + runtime + " (" + stdDevRuntime + ")" ;
			z +=     "\n== avrg exploraion-time(sec):" + explorationtime ;
			z +=     "\n== avrg #door-attempts:" + numOfDoorAttemps ;	
			z +=     "\n== avrg #turns        :" + numberOfTurns ;
			z +=     "\n== avrg #rooms found  :" + roomsInferred + " (" + stdDevRoomsInferred + ")" ;
			z +=     "\n== avrg #doors found  :" + doorsInferred + "/" + numberOfDoors + " (" + stdDevDoorsInferred + ")" ;
			z +=     "\n== avrg #buttons found:" + buttonsInferred + "/" + numberOfButtons + " (" + stdDevButtonsInferred + ")" ;
			z +=     "\n== #connections    :" + numberOfConnections ;
			z +=     "\n==    avrg inferred:"    + connectionsInferred + " (" + stdDevConnectionsInferred + ")" ;
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
	
	/**
	 * For calculating covered tiles, given a set of visited locations.
	 */
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
	
	/**
	 * Run an experiment with online-search targeting multiple levels in a bench-mark set.
	 * It will run both the full variant of the algorithm, and the minus-variant (without
	 * model construction).
	 * 
	 * @param numberOfRunsPerLevel The number of runs the algorithm will be run on each level.
	 * 			Repeated runs are used to obtained averaged results.
	 * @param algVariant  The variant of the Online-search algorithm to use, either full or
	 * 			without constructing a model. 
	 * @param budget The budget. This is specified in an array, one value for each level.
	 * 			For the full-variant of the online-search algorithm, this budget is  
	 * 			expressed in the number of the agent's update cycles.
	 * 			For the minus-variant the budget is in seconds. For the minus-variant,
	 * 			a multiplier B will be applied to obtain the actual budget from the given budget.
	 * 			This B could be 1.2 or 1.5 (see in the code).	
	 */
	public void run_experiment(String benchMarkName,
			int numberOfRunsPerLevel,
			String agentName,
			String[] levels,
			String[] targetDoors,
			Vec3[] guidingLocations,
			int[] budget, 
			AlgorithmVariant algVariant
			) throws IOException, InterruptedException {
		
		var experimentStartTime = System.currentTimeMillis() ;
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
		
		ResultMultiRuns[] results = new ResultMultiRuns[levels.length] ;		
		for (int k=0; k<levels.length; k++) {
			
			if (algVariant == AlgorithmVariant.OnlineSearch) {
				results[k] = runOneLevel(
						benchMarkName,agentName,levels[k],targetDoors[k],guidingLocations[k],
						budget[k], null,
						numberOfRunsPerLevel,
						AlgorithmVariant.OnlineSearch) ;
			}
			else {
				results[k] = runOneLevel(
						benchMarkName,agentName,levels[k],targetDoors[k],guidingLocations[k],
						null, (int)((float) budget[k] * 1000 * 1.2) ,
						numberOfRunsPerLevel,
						AlgorithmVariant.OnlineMinus) ;
			}
		}	
		
		boolean allLevelsTheSame = Arrays.stream(levels).filter(z -> z.equals(levels[0])).count() == levels.length ;
		if (allLevelsTheSame) {
			// special part for LargeRandom, to calculate the sum of the area coverage of all tests
				
			String levelFile = Paths.get(levelsDir, levels[0] + ".csv").toString() ;
			var walkableTiles = LRFloorMap.firstFloorWalkableTiles(levelFile) ; 
			float[] totalAreaCoverage = new float[numberOfRunsPerLevel] ;
			float sum = 0 ;
			// calculate the combined coverage for each run:
			for (int runNr=0; runNr<numberOfRunsPerLevel; runNr++) {
				var info_0 = results[0].visitedTiles.get(runNr) ; // coverage of target-0
				// add the coverage of all targets to that of target-0
				for (int k=1; k<levels.length; k++) {
					var info_k = results[k].visitedTiles.get(runNr) ; // coverage of target-k
					// combine:
					info_0.addAll(info_k) ;
				}
				int covered = (int) info_0.stream().filter(tile -> walkableTiles.contains(tile)).count() ;
				totalAreaCoverage[runNr] = (float) covered / (float) walkableTiles.size() ;
				sum += totalAreaCoverage[runNr] ;
			}
			float avrgTotalAreaCoverage = sum / (float) numberOfRunsPerLevel ;
		
			appendWritelnToFile(dataDir.toString(),reportFileName,"=== Average TOTAL area coverage: "
					+ avrgTotalAreaCoverage
					,true) ;	
			}
			long totExperimentTime = (System.currentTimeMillis() - experimentStartTime)/1000 ;
			float hours = (float) totExperimentTime / 3600f ;
			appendWritelnToFile(dataDir.toString(),reportFileName,
				"=== tot experiment time: " + totExperimentTime + "s (" + hours + " hrs)"
				,true) ;
	}
	
	
	/**
	 * Running the Online-search algorithm multiple times on the same level. This is meant to
	 * obtained an averaged result.
	 * 
	 * @param targetDoor the target door in the given level. The testing task to do is to verify
	 * 					that this door can be openned.
	 * @param budgetInCycles Budget expressed in the number of update-cycles to use, if not null.
	 * @param budgetInMs Budget expressed in mille secs, if not null.
	 * @param algVariant  The variant of the Online-search algorithm to use, either full or
	 * 						without constructing a model. 
	 */
	public ResultMultiRuns runOneLevel(
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
		
		// for bubbling up area-coverage information, for the LargeRandom experiment only:
		
		combinedResult.visitedTiles = new LinkedList<>() ;
		for (int k=0; k<numberOfRuns; k++ ) {
			combinedResult.visitedTiles.add(results[k].visitedTiles) ;
		}
		return combinedResult ;
	}

	/**
	 * A single run of executing a testing task to verify that the target blocker 
	 * can be opened.
	 * 
	 * @param budgetInCycles Budget expressed in the number of update-cycles to use, if not null.
	 * @param budgetInMs Budget expressed in mille secs, if not null.
	 * @param algVariant  The variant of the Online-search algorithm to use, either full or
	 * 						without constructing a model. 
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
		startLR() ;
		
		// Launching LR and loading the level:
		var LRconfig = new LabRecruitsConfig(levelName, levelsDir);
		LRconfig.agent_speed = 0.1f;
		LRconfig.view_distance = 4f ;
		if (levelName.contains("largerandom")) {
			// using different conf for Large-Random:
			LRconfig.view_distance = 7f;
		}
		
		var environment = new LabRecruitsEnvironment(LRconfig);

		if (TestSettings.USE_GRAPHICS) {
			System.out.println(
					"You can drag then game window elsewhere for beter viewing. Then hit RETURN to continue.");
			new Scanner(System.in).nextLine();
		}

		BeliefStateExtended beliefState = null ;
		
		try {
			
			// preparing and creating the test agent; including prepping its state, data collector, etc:
			
			beliefState = new BeliefStateExtended();

			var prolog = new Prolog(beliefState);
			var testAgent = new LabRecruitsTestAgent(agentName) 
					.attachState(beliefState)
					.attachEnvironment(environment);

			beliefState.highLevelGragh.goalPosition = approximateTargetLocation;

			// Instantiate the Online-search algorithm here:
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
			Result.goalName = targetDoor ;
			
			long startTime = System.currentTimeMillis();
			testAgent.registerEvent(new TimeStampedObservationEvent("startTest"));
			
			// the loop to run the agent:
			int cycleNumber = 0;
			long runTime = 0 ;
			// positions sampled every 10 cycles, to facilitate stuck detection
			List<Vec3> sampledPositions = new LinkedList<>() ;
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
				if (cycleNumber == 1) {
					Thread.sleep(1500);
					if (levelName.contains("largerandom") || levelName.contains("BM2021") ) {
						// force nav-mesh fix here:
						LRNavGraphRepair.repairMissingEdges(beliefState) ;
						LRNavGraphRepair.checkUnreachableNodes(beliefState) ;
					}
					
				}
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
				
				if (cycleNumber % 100 == 0 && sampledPositions.size() >= 9) {
					var p0 = sampledPositions.get(0) ;
					if (sampledPositions.stream().allMatch(p -> Vec3.distSq(p,p0) <= 1f)) {
						// agent seems to be stuck!
						DebugUtil.log(">>>> The agent seems to be stuck. Terminating its run.");
						break ;
					}
					sampledPositions.clear();
				}
				if (cycleNumber % 10 == 0) {
					sampledPositions.add(testAgent.getState().worldmodel.position) ;
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
			String visitInfoFileName = levelName + "_" + Result.alg 
					+ "_" + targetDoor 
					+ "_visits_run" + runNumber + ".csv" ;
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
			Result.visitedTiles = getCoveredTiles2D(visitedLocations) ;		
			int covered = (int) Result.visitedTiles.stream().filter(tile -> walkableTiles.contains(tile)).count() ;
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
			String reportFileName = levelName + "_" + Result.alg 
					+ "_" + targetDoor
					+ "_all-info_run" + runNumber + ".txt" ;
			writelnToFile(dataDir,reportFileName, sb.toString(),true) ; // echo is true, to also print to screen
	        
	        return Result ;
			
		} finally {
			environment.close();
			beliefState.prolog().prolog.clearTheory() ;
			closeLR() ;
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
	
	
	private static LabRecruitsTestServer labRecruitsTestServer;

	
	static public void startLR() {
		// TestSettings.USE_SERVER_FOR_TEST = false ;
		// Uncomment this to make the game's graphic visible:
		System.out.println(">>>> launching LR.") ;
		TestSettings.USE_GRAPHICS = USE_GRAPHICS ;
		String projectRootDir = System.getProperty("user.dir");
		labRecruitsTestServer = TestSettings.start_LabRecruitsTestServer(projectRootDir);
	}

	static void closeLR() {
		if (labRecruitsTestServer != null) {
			System.out.println(">>>> closing LR.") ;
			labRecruitsTestServer.close();
		}
	}
	
	@BeforeEach
	void resetSomeThresholds() {
		// these are the standard values. Restore it here, because some tests
		// override them:
	    TacticLib.EXPLORATION_TARGET_DIST_THRESHOLD = 0.5f ;
		BeliefState.DIST_TO_WAYPOINT_UPDATE_THRESHOLD =  0.5f ;
	}

	
	//@Test
	public void test0() throws InterruptedException, IOException {
		/*
		executeTestingTask(1,"ATEST","agent0","BM2021_diff2_R7_2_2","door6",null,
				5000,null,
				AlgorithmVariant.OnlineSearch) ;
		*/
		
		/*
		executeTestingTask(1,"ATEST","agent0","BM2021_diff3_R4_2_2_M","door3",null,
				5000,null,
				AlgorithmVariant.OnlineMinus) ;
		*/
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
		executeTestingTask(1,"DDO","agent1","sanctuary_1","doorEntrance",null,
				30000,
				null,
				AlgorithmVariant.OnlineSearch) ;
		*/
		
		// Vec3 goalPosition = new Vec3(106f,0,81f); // guide for Durk DoorKey3	
		// new Vec3(67f,0,76f) for doorkey4
		//TacticLib.EXPLORATION_TARGET_DIST_THRESHOLD = 0.8f ;
		//BeliefState.DIST_TO_WAYPOINT_UPDATE_THRESHOLD = 0.8f ;
		/*
		executeTestingTask(1,"DDO","agent1","durk_1","doorKey4",null,
				30000,null,
				AlgorithmVariant.OnlineSearch) ;
		*/
		
		// d37 problem, seems like the algorihm runs out of node to select??
		
		TacticLib.EXPLORATION_TARGET_DIST_THRESHOLD = 0.8f ;
		BeliefState.DIST_TO_WAYPOINT_UPDATE_THRESHOLD = 0.8f ;
		// FBK_largerandom_R9_cleaned
		// FBK_largerandom_samiraorig
		//new Vec3(12,0,124), // d33
		//new Vec3(108,0,145), // d34
		//new Vec3(104,0,178), // d37
		//new Vec3(107,0,104), // d2
		//new Vec3(72,0,124), // d5
		//new Vec3(84,0,81),  //d16 
		//new Vec3(104,0,156), //d30
		//new Vec3(144,0,118)   //d9
		//new Vec3(144,0,61)   //d15
		
		executeTestingTask(1,"LargeRandom","agent1","FBK_largerandom_R9_cleaned","door15",
				new Vec3(144,0,57),		
				30000,null,
				AlgorithmVariant.OnlineSearch) ;
		
		/*
		executeTestingTask(1,"LargeRandom","agent1","FBK_largerandom_R9_cleaned","door16",
				null,
				//new Vec3(12,0,124), // d33
				30000,null,
				AlgorithmVariant.OnlineSearch) ;
		*/
		

	}
	

	
	//@Test
	public void run_onlineFull_on_ATEST_experiment_Test() throws Exception {
		run_experiment("ATEST",
				ATEST_repeatNumberPerRun,
				"agent0",
				ATEST_levels,
				ATEST_targetDoors,
				ATEST_guidingLocations,
				ATEST_fullOnline_budget, 
				AlgorithmVariant.OnlineSearch   
				) ;
	}
		
	//@Test
	public void run_onlineMinus_on_ATEST_experiment_Test() throws Exception {
		run_experiment("ATEST",
				ATEST_repeatNumberPerRun,
				"agent0",
				ATEST_levels,
				ATEST_targetDoors,
				ATEST_guidingLocations,
				ATEST_SAruntime,
				AlgorithmVariant.OnlineMinus
				) ;
	}
	
	//@Test
	public void run_onlineFull_on_DDO_experiment_Test() throws Exception {
	run_experiment("DDO",
				LargeLevels_repeatNumberPerRun,
				"agent1",
				DDO_levels,
				DDO_targetDoors,
				DDO_guidingLocations,
				DDO_fullOnline_budget, 
				AlgorithmVariant.OnlineSearch   
				) ;
	}
	
	//@Test
	public void run_onlineMinus_on_DDO_experiment_Test() throws Exception {
	run_experiment("DDO",
				LargeLevels_repeatNumberPerRun,
				"agent1",
				DDO_levels,
				DDO_targetDoors,
				DDO_guidingLocations,
				DDO_SAruntime,
				AlgorithmVariant.OnlineMinus
				) ;
	}
	
	//@Test
	public void run_onlineFull_on_LargeRandom_experiment_Test() throws Exception {
		TacticLib.EXPLORATION_TARGET_DIST_THRESHOLD = 0.8f ;
		BeliefState.DIST_TO_WAYPOINT_UPDATE_THRESHOLD = 0.8f ;
		run_experiment("LargeRandom",
				LargeLevels_repeatNumberPerRun,
				"agent1",
				LargeRandom_levels,
				LargeRandom_targetDoors,
				LargeRandom_guidingLocations,
				LargeRandom_fullOnline_budget,
				AlgorithmVariant.OnlineSearch   
				) ;
	}
	
	// @Test
	public void run_onlineMinus_on_LargeRandom_experiment_Test() throws Exception {
		TacticLib.EXPLORATION_TARGET_DIST_THRESHOLD = 0.8f ;
		BeliefState.DIST_TO_WAYPOINT_UPDATE_THRESHOLD = 0.8f ;
		run_experiment("LargeRandom",
				LargeLevels_repeatNumberPerRun,
				"agent1",
				LargeRandom_levels,
				LargeRandom_targetDoors,
				LargeRandom_guidingLocations,
				LargeRandom_SAruntime,  
				AlgorithmVariant.OnlineMinus   
				) ;
	}

}