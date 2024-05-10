package stvrExperiment;

import static agents.tactics.OnlineSearch.onlineSearchAlgorithm1;

import java.nio.file.Paths;
import java.util.Scanner;

import org.junit.jupiter.api.*;

import agents.LabRecruitsTestAgent;
import agents.TestSettings;
import agents.tactics.GoalLibExtended;
import environments.LabRecruitsConfig;
import environments.LabRecruitsEnvironment;
import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.spatial.Vec3;
import eu.iv4xr.framework.spatial.meshes.Edge;
import game.LabRecruitsTestServer;
import world.BeliefStateExtended;
import world.LRNavGraphRepair;

import static nl.uu.cs.aplib.AplibEDSL.* ;

/**
 * For testing out issues with the LargeRandom level.
 */
public class NavMeshIntegrity_Test {
	
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
	
	static String projectRootDir = System.getProperty("user.dir") ;
	static String levelsDir = Paths.get(projectRootDir, "src", "test", "resources", "levels", "STVR").toString() ;
	
	
	TestAgent launchLR() {
		
		//var LRconfig = new LabRecruitsConfig("FBK_largerandom_R9_cleaned", levelsDir);
		var LRconfig = new LabRecruitsConfig("FBK_largerandom_samiraorig", levelsDir);
		//var LRconfig = new LabRecruitsConfig("sanctuary_1", levelsDir);
		//var LRconfig = new LabRecruitsConfig("BM2021_diff3_R7_3_3", levelsDir);
		LRconfig.view_distance = 1f;
		var environment = new LabRecruitsEnvironment(LRconfig);

		if (TestSettings.USE_GRAPHICS) {
			System.out.println("You can drag then game window elsewhere for beter viewing. Then hit RETURN to continue.");
			new Scanner(System.in).nextLine();
		}
		
		var beliefState = new BeliefStateExtended();

		var testAgent = new LabRecruitsTestAgent("agent1") 
							.attachState(beliefState)
							.attachEnvironment(environment);
		
		testAgent.setGoal(SEQ(SUCCESS(),SUCCESS(),SUCCESS()));
		
		return testAgent ;
	}
	
	
	@Test
	public void test_navmesh_connectivity() {
		var agent = launchLR() ;
		var state = (BeliefStateExtended) agent.state() ;
		agent.update();
		agent.update();
		var pf = state.pathfinder() ;
		pf.perfect_memory_pathfinding = true ;
		System.out.println(">>> start test") ;
		LRNavGraphRepair.getCorners(state) ;
		LRNavGraphRepair.checkUnreachableNodes(state) ;
		LRNavGraphRepair.repairMissingEdges(state) ;
		LRNavGraphRepair.checkUnreachableNodes(state) ;	
		LRNavGraphRepair.getCorners(state) ;
	}

}
