/*
This program has been developed by students from the bachelor Computer Science
at Utrecht University within the Software and Game project course.

©Copyright Utrecht University (Department of Information and Computing Sciences)
*/

package onlineSearch;

import agents.LabRecruitsTestAgent;
import agents.TestSettings;
import agents.tactics.GoalLib;
import agents.tactics.TacticLib;
import environments.LabRecruitsConfig;
import environments.LabRecruitsEnvironment;
import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.mainConcepts.TestDataCollector;
import game.LabRecruitsTestServer;
import game.Platform;
import eu.iv4xr.framework.spatial.Vec3;
import logger.JsonLoggerInstrument;
import nl.uu.cs.aplib.Logging;
import nl.uu.cs.aplib.mainConcepts.*;
import world.BeliefState;
import world.LabEntity;

import static agents.TestSettings.USE_GRAPHICS;
import static agents.TestSettings.USE_INSTRUMENT;
import static agents.TestSettings.USE_SERVER_FOR_TEST;
import static eu.iv4xr.framework.Iv4xrEDSL.assertTrue_;
import static nl.uu.cs.aplib.AplibEDSL.*;
import static org.junit.jupiter.api.Assertions.* ;

import java.io.File;
import java.util.Scanner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

/**
 * Test that agent can explore its way to find an entity (a button) in a simple
 * maze. The maze is made of walls and decorative items such as book cases.
 * These type of items are static and solid; so they will be substracted from
 * the navigation mesh.
 */
public class Test_BigLevel {
	
	private static LabRecruitsTestServer labRecruitsTestServer;

    @BeforeAll
    static void start() {
    	// Uncomment this to make the game's graphic visible:
        TestSettings.USE_GRAPHICS = true ;
    	String labRecruitesExeRootDir = System.getProperty("user.dir") ;
       	labRecruitsTestServer = TestSettings.start_LabRecruitsTestServer(labRecruitesExeRootDir) ;
    }

    @AfterAll
    static void close() { if(labRecruitsTestServer != null) labRecruitsTestServer.close(); }

    
    /**
     * Test that the agent can find button1 in a simple maze.
     */
    @Test
    public void test_explore_on_simplemaze() throws InterruptedException {

    	String levelName = "MutatedFiles\\MOSA\\selectedLevels\\1649941005014";
    	String fileName = "LabRecruits_level-original - Copy";
    	var config = new LabRecruitsConfig(fileName,Platform.LEVEL_PATH +File.separator+ levelName) ;
    	config.view_distance = 12 ;
    	
    	var environment = new LabRecruitsEnvironment(config);
        
        LabRecruitsTestAgent agent = new LabRecruitsTestAgent("agent1")
        		                     . attachState(new BeliefState())
        		                     . attachEnvironment(environment) ;
        
        var g = GoalLib.entityInteracted("button12") ;
        agent.setGoal(g) ;
        
    	if(TestSettings.USE_GRAPHICS) {
    		System.out.println("You can drag then game window elsewhere for beter viewing. Then hit RETURN to continue.");
    		new Scanner(System.in).nextLine();
    	}

        // press play in Unity
        if (! environment.startSimulation())
            throw new InterruptedException("Unity refuses to start the Simulation!");
        

        int i = 0 ;
        agent.state().pathfinder().perfect_memory_pathfinding = true ;
        agent.update();
        agent.update();
        while (g.getStatus().inProgress()) {
            agent.update();
            System.out.println("*** " + i + "/" 
               + agent.state().worldmodel.timestamp + ", "
               + agent.state().id + " @" + agent.state().worldmodel.position) ;
            Thread.sleep(30);
            i++ ;
            if (i>120) {
            	break ;
            }
        }
        
        var path0 = agent.state().pathfinder().findPath(new Vec3(102,0,103), new Vec3(104,0,101), 0.2f) ;
        var path1 = agent.state().pathfinder().findPath(new Vec3(102,0,103), new Vec3(124,0,89), 0.2f) ;
        var path1b = agent.state().pathfinder().findPath(new Vec3(102,0,103), new Vec3(105.5f,0,102), 0.2f) ;
        var path2 = agent.state().pathfinder().findPath(new Vec3(102,0,103), new Vec3(103,0,103), 0.2f) ;
        var path3 = agent.state().pathfinder().findPath(new Vec3(102,0,103), new Vec3(104.5f,0,104), 0.2f) ;
        var path4 = agent.state().pathfinder().findPath(new Vec3(102,0,103), new Vec3(105.5f,0,104), 0.2f) ;
        var path5 = agent.state().pathfinder().findPath(new Vec3(102,0,103), new Vec3(106f,0,104), 0.2f) ;
        var path6 = agent.state().pathfinder().findPath(new Vec3(102,0,103), new Vec3(107f,0,104), 0.2f) ;
        var path7 = agent.state().pathfinder().findPath(new Vec3(102,0,103), new Vec3(109f,0,104), 0.2f) ;
        
        System.out.println(">>>>> p0" + path0) ;
        System.out.println(">>>>> p1" + path1) ;
        System.out.println(">>>>> p1b" + path1b) ;
        System.out.println(">>>>> p2" + path2) ;
        System.out.println(">>>>> p3" + path3) ;
        System.out.println(">>>>> p4" + path4) ;
        System.out.println(">>>>> p5" + path5) ;
        System.out.println(">>>>> p6" + path5) ;
        System.out.println(">>>>> p7" + path5) ;
        
        new Scanner(System.in).nextLine();
             
        
        
        if (!environment.close())
            throw new InterruptedException("Unity refuses to close the Simulation!");
    }
    
}