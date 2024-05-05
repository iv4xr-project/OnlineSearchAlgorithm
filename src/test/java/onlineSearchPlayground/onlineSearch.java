package onlineSearchPlayground;

import agents.LabRecruitsTestAgent;
import agents.TestSettings;
import static agents.TestSettings.*;
import agents.tactics.GoalLib;
import agents.tactics.TacticLib;
import alice.tuprolog.InvalidTheoryException;
import environments.LabRecruitsConfig;
import environments.LabRecruitsEnvironment;
import eu.iv4xr.framework.mainConcepts.TestDataCollector;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import eu.iv4xr.framework.mainConcepts.WorldModel;
import eu.iv4xr.framework.spatial.Vec3;
import helperclasses.datastructures.linq.QArrayList;
import logger.JsonLoggerInstrument;
import nl.uu.cs.aplib.mainConcepts.Environment;
import nl.uu.cs.aplib.mainConcepts.Goal;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.mainConcepts.GoalStructure.GoalsCombinator;
import nl.uu.cs.aplib.mainConcepts.GoalStructure.PrimitiveGoal;
import nl.uu.cs.aplib.utils.Pair;

import static org.junit.jupiter.api.Assertions.* ;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import game.Platform;
import gameTestingContest.DebugUtil;
import gameTestingContest.Prolog;
import game.LabRecruitsTestServer;
import world.BeliefState;

import static nl.uu.cs.aplib.AplibEDSL.*;
import static nl.uu.cs.aplib.agents.PrologReasoner.and;
import static nl.uu.cs.aplib.agents.PrologReasoner.not;

import world.BeliefStateExtended;
import agents.tactics.GoalLibExtended;

public class onlineSearch {

	 private static LabRecruitsTestServer labRecruitsTestServer;
	    @BeforeAll
	    static public void start() {
	    	// TestSettings.USE_SERVER_FOR_TEST = false ;
	    	// Uncomment this to make the game's graphic visible:
	    	 TestSettings.USE_GRAPHICS = true ;
	    	String labRecruitesExeRootDir = System.getProperty("user.dir") ;
	    	labRecruitsTestServer = TestSettings.start_LabRecruitsTestServer(labRecruitesExeRootDir) ;
	    }

	  //methods for prolog 

	  		/**
	  		 * To keep track which button the agent toggled last.
	  		 */
	  		// FRAGILE!
	  		WorldEntity lastInteractedButton = null;
	 
	    @AfterAll
	    static void close() { if(labRecruitsTestServer!=null) labRecruitsTestServer.close(); }
	    
	    void instrument(Environment env) {
	    	env.registerInstrumenter(new JsonLoggerInstrument()).turnOnDebugInstrumentation();
	    }

		/* Calculate euclidean distance */
	    
	    public double euclideanDistance(Vec3 from, Vec3 to) {
	    	var  x = Math.abs(from.x - to.x);
	    	var  y = Math.abs(from.y - to.y);
	    	var  z = Math.abs(from.z - to.z);
	    	return Math.sqrt(x+y+z);
	    } 
	    /**
	     * A test to verify that the east closet is reachable.
	     */
	    @SuppressWarnings("deprecation")
		@Test
	    public void closetReachableTest() throws InterruptedException {
	    	//String levelName = "";
	    	String levelName = "CompetitionGrander//bm2021";
	    	String fileName = "BM2021_diff1_R4_1_1_M";

	        // Create an environment
	    	var LRconfig = new LabRecruitsConfig(fileName,Platform.LEVEL_PATH +File.separator+ levelName) ;
	    	LRconfig.agent_speed = 0.1f ;
	    	LRconfig.view_distance = 4f;
	    	String treasureDoor = "door5";
	    	Vec3 goalPosition =  null; 
	        var environment = new LabRecruitsEnvironment(LRconfig);
	        if(USE_INSTRUMENT) instrument(environment) ;
	        int cycleNumber = 0 ;
	        long totalTime = 0;
	        String finalResult = "null";
	        try {
	        	if(TestSettings.USE_GRAPHICS) {
	        		System.out.println("You can drag then game window elsewhere for beter viewing. Then hit RETURN to continue.") ;
	        		new Scanner(System.in). nextLine() ;
	        	}
	            var beliefState = new BeliefStateExtended();
	            
	            var prolog = new Prolog(beliefState);
		        // create a test agent
		        var testAgent = new LabRecruitsTestAgent("agent1") // matches the ID in the CSV file
	        		    . attachState(beliefState)
	        		    . attachEnvironment(environment);    

		       
		       var agentPosiion = environment.observe("agent1").position;
		       
		       
			   /* calculate the euclidean distance from agent position to the treasure door, the treasure door
				 * distance is estimated */
		       // euclideanDistance(agentPosiion, goalPosition);
		       // System.out.println("euclidean dis " + euclideanDistance(agentPosiion, goalPosition));		        
		        beliefState.highLevelGragh.goalPosition = goalPosition;	     
		        
//		        var testingTask = SEQ(
//		        		//GoalLibExtended.entityStateRefreshed(treasureDoor)
//		        		GoalLibExtended.entityInCloseRange("button25")
//		        		);
		        
		        var testingTask = SEQ( 
		        		WHILEDO(
		        				(BeliefStateExtended b) -> GoalLibExtended.isDoorClosedPredicate(b,treasureDoor)	
		        				, 
		        				 SEQ(
			    	        		FIRSTof(
			    	        				GoalLibExtended.newObservedNodes(testAgent),
			    	        				WHILEDO(
			    	        						(BeliefStateExtended b) -> GoalLibExtended.checkExplore(b),
			    	        						SEQ(
			    	        								GoalLibExtended.findNodes(testAgent,beliefState)
			    	        						))
			    	        				)    		
			    	        		,		
			    	        		//FIRSTof(
			    	        				//if during the exploration to find a new entity, agent sees the goal, we check that it is open or not
			    	        				//GoalLibExtended.finalGoal(treasureDoor),
			    	        				// we select an entity to navigate to it based on some specific policies
			    	        				// the goal will be selected if it is in the list of goal
			    	        			GoalLibExtended.selectedNode(beliefState,testAgent,goalPosition, treasureDoor)
			    	        		//		)
			    	        		,	
			    	        		// navigate to selected node
			    	        		IFELSE(
			    	        				(BeliefStateExtended b) -> GoalLibExtended.entityTypePredicate(beliefState),
			    	        				//SEQ(GoalLibExtended.navigateToDoor(beliefState),lift((BeliefStateExtended b) -> GoalLibExtended.clearPath(beliefState)),GoalLibExtended.entityInCloseRange(beliefState)), 
			    	        				GoalLibExtended.navigateToDoor(beliefState),
			    	        				GoalLibExtended.navigateToButton(beliefState)
			    	        				)
			    	        		,
			    	        		//check if the selected node is a blocker. if it is, firstly check the prolog and then add a new goal if it is not solved.
			    	        		IFELSE(
			    	        				(BeliefStateExtended b) -> GoalLibExtended.checkEntityStatePredicate(beliefState),GoalLibExtended.dynamicGoal(beliefState,testAgent),FAIL())			    	        					    	        		
		        				)
		        			)		        		
		        		);
		        
		        // attaching the goal and test data-collector
		        var dataCollector = new TestDataCollector();
		        testAgent . setTestDataCollector(dataCollector).setGoal(testingTask) ;

		        environment.startSimulation();
		         // this will press the "Play" button in the game for you
		        //goal not achieved yet
		        assertFalse(testAgent.success());
		
		    	
 	           
		        // keep updating the agent
		        long startTime = System.currentTimeMillis();
		        while (testingTask.getStatus().inProgress()) {

		        	System.out.println("*** " + cycleNumber + ", " + testAgent.getState().id + " @" + testAgent.getState().worldmodel.position) ;
		            Thread.sleep(100);
		            
		            cycleNumber++ ; 
		        	testAgent.update();
		        	// not implemented:  
		        	// testAgent.updateGraph();
	               
		        	// check if a button is just interacted:
					for(WorldEntity e: testAgent.getState().changedEntities) {
						if(e.type.equals("Switch") && e.hasPreviousState()) {
							DebugUtil.log(">> detecting interaction with " + e.id) ;
							lastInteractedButton = e ;					
						}
					}
					// check doors that change state, and add connections to lastInteractedButton:
					if(lastInteractedButton != null) {
						for(WorldEntity e: testAgent.getState().changedEntities) {							
							if(e.type.equals("Door") && e.hasPreviousState()) {
								try {
									beliefState.prolog.registerConnection(lastInteractedButton.id,e.id) ;
								} catch (InvalidTheoryException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
							}	
						}
					}
					
					if(beliefState.worldmodel().health <= 0) {
						DebugUtil.log(">>>> the agent died. Aaaw.");
					//	throw new AgentDieException() ;
					}
	        	
		        	if (cycleNumber>2000) {
		        		break ;
		        	}
		        }
		        long endTime = System.currentTimeMillis();
		        totalTime = endTime - startTime;
		        testingTask.printGoalStructureStatus();
		        		       
		        
		        testAgent.printStatus();
		        var agentneTimeStamss = testAgent.getState().knownEntities();
		   		 prolog.report();
		   		var trace2 = testAgent
						. getTestDataCollector()
						. getTestAgentTrace(testAgent.getId()).stream().map( e-> e.getFamilyName()).collect(Collectors.toList());
				        ;
				
				 System.out.println("trace2  " + trace2) ;	   		 
	        }
	        finally { environment.close(); }
	
	
}
}