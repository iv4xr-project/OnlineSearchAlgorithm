package onlineSearch;

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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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
import gameTestingContest.TestingTaskStack;
import game.LabRecruitsTestServer;
import world.BeliefState;

import static nl.uu.cs.aplib.AplibEDSL.*;
import static nl.uu.cs.aplib.agents.PrologReasoner.and;
import static nl.uu.cs.aplib.agents.PrologReasoner.not;

import world.BeliefStateExtended;
import world.Utils;
import agents.tactics.GoalLibExtended;


public class multiAgent3 {

	 private static LabRecruitsTestServer labRecruitsTestServer;
	    @BeforeAll
	    static public void start() {
	    	// TestSettings.USE_SERVER_FOR_TEST = false ;
	    	// Uncomment this to make the game's graphic visible:
	    	TestSettings.USE_GRAPHICS = true ;
	    	String labRecruitesExeRootDir = System.getProperty("user.dir") ;
	    	labRecruitsTestServer = TestSettings.start_LabRecruitsTestServer(labRecruitesExeRootDir) ;
	    }

	

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
 
	    /**
	     * A test to verify that the east closet is reachable.
	     * a few things to change for different tests:
	     * 1-  while loop, the sharing agents,  and the tasks in testingTaskStack should be changed
	     * 2- distance according to the level and single or multi
	     * @throws IOException 
	     */
	    @Test
	    public void closetReachableTest() throws InterruptedException, IOException {
	    	//String levelName = "";
	    	String levelName = "CompetitionGrander\\bm2021";
	   // 	String fileName = "durk_LR_map - withoutfakedoors";
	    //	String fileName = "simple-level-second - narrow - random buttons"; //this works
	    //	String fileName = "simple-level-second - narrow-two";//this works
	    //	String fileName = "simple-level-second - narrow - complex"; //this works
	    //	String fileName = "simple-level-second - narrow-two - prolog"; //this works with the setup of commenting if
	    	String fileName = "simple-level-second - basic level - 100 - 3Agent";
	    	// Create an environment
	    	var LRconfig = new LabRecruitsConfig(fileName,Platform.LEVEL_PATH +File.separator+ levelName) ;
	    	LRconfig.agent_speed = 0.1f ;
	    	LRconfig.view_distance = 18f;
	    	String treasureDoor = "door3";
	    	String treasureDoor2 = "door6";
	    	String label = "-explore-basic-3agent-18-2";
	    	Vec3 goalPosition =  null; 
	        var environment = new LabRecruitsEnvironment(LRconfig);
	        if(USE_INSTRUMENT) instrument(environment) ;
	        int cycleNumber = 0 ;
	        long totalTime = 0;
	        String finalResult = "null";
	        boolean advanceSync = false;
	        int distance = 100; // 2500 for 50 , 10000 for experiment
	        int totalTasks = 10;
	        
	        try {
	        	if(TestSettings.USE_GRAPHICS) {
	        		System.out.println("You can drag then game window elsewhere for beter viewing. Then hit RETURN to continue.") ;
	        		new Scanner(System.in). nextLine() ;
	        	}
	            var beliefState = new BeliefStateExtended();
	            
	            var prolog = new Prolog(beliefState);
		        // create a test agent
		        var testAgent1 = new LabRecruitsTestAgent("agent1") // matches the ID in the CSV file
	        		    . attachState(beliefState)
	        		    . attachEnvironment(environment);    

		        var beliefState2 = new BeliefStateExtended();
		        var testAgent2 = new LabRecruitsTestAgent("agent2") // matches the ID in the CSV file
	        		    . attachState(beliefState2)
	        		    . attachEnvironment(environment)
	        		    ;    
		        var beliefState3 = new BeliefStateExtended();
		        var testAgent3 = new LabRecruitsTestAgent("agent3") // matches the ID in the CSV file
	        		    . attachState(beliefState3)
	        		    . attachEnvironment(environment)
	        		    ;    
		        
		        
		        TestingTaskStack testingTaskList = new TestingTaskStack();
		     
		        beliefState.highLevelGragh.goalPosition = goalPosition;	     
	
		        
		        //agent with high weight priority
		        var testingTask = SEQ( 
		        		WHILEDO(
        					(BeliefStateExtended b) -> GoalLibExtended.checkExploreAndTasks(b, testingTaskList, testAgent1, "higher", 5, totalTasks),
        					SEQ(
        					IFELSE(
			        			(BeliefStateExtended b) -> GoalLibExtended.highWeightTasks(b,testAgent1, testingTaskList, 5),
				        		
				        		SEQ(
				        				GoalLibExtended.selectedNodeByTask(beliefState,testAgent1,5, testingTaskList, "higher", distance),
				    	        		// navigate to selected node
				        	//	IFELSE(
				        		IF2(
				    	        	(BeliefStateExtended b) -> GoalLibExtended.entityTypePredicateByTask(beliefState),
				    	        //	FIRSTof(GoalLibExtended.navigateToDoorByTask(beliefState, testingTaskList, testAgent1), SUCCESS()),
				    	        	dummy -> GoalLibExtended.navigateToDoorByTask(beliefState, testingTaskList, testAgent1),
				    	        	dummy -> GoalLibExtended.navigateToButtonByTask(beliefState, testingTaskList, testAgent1)
				    	        //	GoalLibExtended.navigateToButtonByTask(beliefState, testingTaskList, testAgent1)
				        				)
				    	        		,
				    	        		//check if the selected node is a blocker. if it is, firstly check the prolog and then add a new goal if it is not solved.
				    	        		IFELSE(
				    	        				(BeliefStateExtended b) -> GoalLibExtended.checkEntityStatePredicate(beliefState,testingTaskList,5, testAgent1.getId(),"higher"),GoalLibExtended.dynamicGoal(beliefState,testAgent1,testingTaskList,beliefState2, beliefState3),SUCCESS())			    	        					    	    
				    	        		,
				    	        		SUCCESS()
				        				)	
				        		
				        		,
				        		
		        				 SEQ(
			    	        		FIRSTof(
			    	        				GoalLibExtended.newObservedNodes(testAgent1, testingTaskList),
			    	        				WHILEDO(
			    	        						(BeliefStateExtended b) -> GoalLibExtended.checkExplore(b, testingTaskList, totalTasks, "higher",5,distance),
			    	        						SEQ(
			    	        							//in the list of newly observed entities, if there is an entity from the list of tasks add it to the stack	
			    	        							GoalLibExtended.findNodes(testAgent1,beliefState,testingTaskList)
			    	        						))
			    	        				),		
					    	        		GoalLibExtended.checkExplore2(beliefState2, testingTaskList, totalTasks) // if all tasks are solved no need to explore more
				        				)
			        		)
        					,FAIL()
        					))
//		        		, 
//		        		GoalLibExtended.moveAgent(testAgent1)
		        		
		        		);
		        
		        
		        
		        //=============================Second agent=============================
		        
		      //agent with low weight priority
//		        var testingTask = SEQ( 
//		        		WHILEDO(
//    					(BeliefStateExtended b) -> GoalLibExtended.checkExploreAndTasks(b, testingTaskList, testAgent1, "lower", 6, totalTasks),
//    					SEQ(
//    					IFELSE(
//		        			(BeliefStateExtended b) -> GoalLibExtended.lowWeightTasks(b,testAgent1, testingTaskList, 6),
//			        		
//			        		SEQ(
//			        				GoalLibExtended.selectedNodeByTask(beliefState,testAgent1,6, testingTaskList, "lower", distance),
//			    	        		// navigate to selected node
//			        	//	IFELSE(
//			        		IF2(
//			    	        	(BeliefStateExtended b) -> GoalLibExtended.entityTypePredicateByTask(beliefState),
//			    	        //	FIRSTof(GoalLibExtended.navigateToDoorByTask(beliefState, testingTaskList, testAgent1), SUCCESS()),
//			    	        	dummy -> GoalLibExtended.navigateToDoorByTask(beliefState, testingTaskList, testAgent1),
//			    	        	dummy -> GoalLibExtended.navigateToButtonByTask(beliefState, testingTaskList, testAgent1)
//			    	        //	GoalLibExtended.navigateToButtonByTask(beliefState, testingTaskList, testAgent1)
//			        				)
//			    	        		,
//			    	        		//check if the selected node is a blocker. if it is, firstly check the prolog and then add a new goal if it is not solved.
//			    	        		IFELSE(
//			    	        				(BeliefStateExtended b) -> GoalLibExtended.checkEntityStatePredicate(beliefState,testingTaskList,6, testAgent1.getId(),"lower"),GoalLibExtended.dynamicGoal(beliefState,testAgent1,testingTaskList,beliefState2),SUCCESS())			    	        					    	    
//			    	        		,
//			    	        		SUCCESS()
//			        				)	
//			        		
//			        		,
//			        		
//	        				 SEQ(
//		    	        		FIRSTof(
//		    	        				GoalLibExtended.newObservedNodes(testAgent1, testingTaskList),
//		    	        				WHILEDO(
//		    	        						(BeliefStateExtended b) -> GoalLibExtended.checkExplore(b, testingTaskList, totalTasks, "lower",6,distance),
//		    	        						SEQ(
//		    	        							//in the list of newly observed entities, if there is an entity from the list of tasks add it to the stack	
//		    	        							GoalLibExtended.findNodes(testAgent1,beliefState,testingTaskList)
//		    	        						))
//		    	        				)    		
//				    	        		,		
//				    	        		GoalLibExtended.checkExplore2(beliefState, testingTaskList, totalTasks) // if all tasks are solved no need to explore more
//			        				)
//		        		)
//    					,FAIL()
//    					))
////	        		, 
////	        		GoalLibExtended.moveAgent(testAgent1)
//	        		
//	        		);
		        
		      //agent with low weight priority
		        var testingTask2 = SEQ( 
		        		WHILEDO(
	        					(BeliefStateExtended b) -> GoalLibExtended.checkExploreAndTasks(b, testingTaskList, testAgent2, "lower", 6, totalTasks),
				        		
	        				SEQ(
	        					IFELSE(
				        			(BeliefStateExtended b) -> GoalLibExtended.lowWeightTasks(b,testAgent2, testingTaskList, 6),
					        		
					        		SEQ(
					        				GoalLibExtended.selectedNodeByTask(beliefState2,testAgent2,6, testingTaskList, "lower", distance),
							        		IF2(
							    	        	(BeliefStateExtended b) -> GoalLibExtended.entityTypePredicateByTask(beliefState2),							    	        
							    	        	dummy -> GoalLibExtended.navigateToDoorByTask(beliefState2, testingTaskList, testAgent2) ,
							    	        	dummy -> GoalLibExtended.navigateToButtonByTask(beliefState2, testingTaskList, testAgent2)					   	        
							        				)
					    	        		,
					    	        		//check if the selected node is a blocker. if it is, firstly check the prolog and then add a new goal if it is not solved.
					    	        		IFELSE(
					    	        				(BeliefStateExtended b) -> GoalLibExtended.checkEntityStatePredicate(beliefState2,testingTaskList,6, testAgent2.getId(),"lower"),GoalLibExtended.dynamicGoal(beliefState2,testAgent2,testingTaskList,beliefState,beliefState3),SUCCESS())			    	        					    	    
					    	        		,
					    	        		SUCCESS()
					        				)
					        		,
			        				 SEQ(
				    	        		FIRSTof(
				    	        				GoalLibExtended.newObservedNodes(testAgent2, testingTaskList),
				    	        				WHILEDO(
				    	        						(BeliefStateExtended b) -> GoalLibExtended.checkExplore(b, testingTaskList, totalTasks, "lower" ,6, distance),
				    	        						SEQ(
				    	        							//in the list of newly observed entities, if there is an entity from the list of tasks add it to the stack	
				    	        							GoalLibExtended.findNodes(testAgent2,beliefState2,testingTaskList)
				    	        						))
				    	        				)    		
				    	        		,		
				    	        		GoalLibExtended.checkExplore2(beliefState2, testingTaskList, totalTasks)   	        						
			        						 )     			
				        		)
	        					
	        					, FAIL()
	        					))
//		        		, 
//		        		GoalLibExtended.moveAgent(testAgent)
		        		
			        		
			        		);
		        
		        //==========================end of second testing task ===================
		        
		        //=============================third agent=============================
		      //agent random
//		        var testingTask3 = SEQ( 
//		        		WHILEDO(
//	        					(BeliefStateExtended b) -> GoalLibExtended.checkExplore(b),
//				        		
//	        				SEQ(
//	        					IFELSE(
//				        			(BeliefStateExtended b) -> GoalLibExtended.randomTasks(b,testAgent3, testingTaskList, 0),
//					        		
//					        		SEQ(
//					        				GoalLibExtended.selectedNodeByTask(beliefState3,testAgent3,6, testingTaskList, "random"),
//					    	        		// navigate to selected node
//					    	        		IFELSE(
//					    	        				(BeliefStateExtended b) -> GoalLibExtended.entityTypePredicateByTask(beliefState3),
//					    	        				//SEQ(GoalLibExtended.navigateToDoor(beliefState),lift((BeliefStateExtended b) -> GoalLibExtended.clearPath(beliefState)),GoalLibExtended.entityInCloseRange(beliefState)), 
//					    	        				FIRSTof(GoalLibExtended.navigateToDoorByTask(beliefState3, testingTaskList,testAgent3), SUCCESS()),
//					    	        				GoalLibExtended.navigateToButtonByTask(beliefState3, testingTaskList, testAgent3)
//					    	        				)
//					    	        		,
//					    	        		//check if the selected node is a blocker. if it is, firstly check the prolog and then add a new goal if it is not solved.
//					    	        		IFELSE(
//					    	        				(BeliefStateExtended b) -> GoalLibExtended.checkEntityStatePredicate(beliefState3,testingTaskList,6, testAgent3.getId(),"random"),GoalLibExtended.dynamicGoal(beliefState3,testAgent3,testingTaskList),SUCCESS())			    	        					    	    
//					    	        		,
//					    	        		//GoalLibExtended.removeDynamicGoal(testAgent2, null),
//					    	        		SUCCESS()
//					        				)
//					        		,
//			        				 SEQ(
//				    	        		FIRSTof(
//				    	        				GoalLibExtended.newObservedNodes(testAgent3, testingTaskList),
//				    	        				WHILEDO(
//				    	        						(BeliefStateExtended b) -> GoalLibExtended.checkExplore(b),
//				    	        						SEQ(
//				    	        							//in the list of newly observed entities, if there is an entity from the list of tasks add it to the stack	
//				    	        							GoalLibExtended.findNodes(testAgent3,beliefState3,testingTaskList)
//				    	        						))
//				    	        				)    		
//				    	        		,		
//				    	        		
//		    	        				//if during the exploration to find a new entity, agent sees the goal, we check that it is open or not
//		    	        				//GoalLibExtended.finalGoal(treasureDoor),
//		    	        				// we select an entity to navigate to it based on some specific policies
//		    	        				// the goal will be selected if it is in the list of goal
//				    	        		// if you select something from the stack, status of that item should change
//				    	        		GoalLibExtended.selectedNodeByTask(beliefState3,testAgent3,6, testingTaskList, "random")
//				    	        		,	
//				    	        		// navigate to selected node
//				    	        		IFELSE(
//				    	        				(BeliefStateExtended b) -> GoalLibExtended.entityTypePredicateByTask(beliefState3),
//				    	        				//SEQ(GoalLibExtended.navigateToDoor(beliefState),lift((BeliefStateExtended b) -> GoalLibExtended.clearPath(beliefState)),GoalLibExtended.entityInCloseRange(beliefState)), 
//				    	        				FIRSTof(GoalLibExtended.navigateToDoorByTask(beliefState3, testingTaskList, testAgent3), SUCCESS()),
//				    	        				GoalLibExtended.navigateToButtonByTask(beliefState3, testingTaskList, testAgent3)
//				    	        				)
//				    	        		,
//				    	        		//check if the selected node is a blocker. if it is, firstly check the prolog and then add a new goal if it is not solved.
//				    	        		IFELSE(
//				    	        				(BeliefStateExtended b) -> GoalLibExtended.checkEntityStatePredicate(beliefState3,testingTaskList,6,testAgent3.getId(),"random"),GoalLibExtended.dynamicGoal(beliefState3,testAgent3,testingTaskList),SUCCESS())
//				    	        		
//			        				 // , GoalLibExtended.removeDynamicGoal(testAgent2, null)
//			        				  , FAIL()
//			        						 )     			
//				        		)
//	        					
//	        					, FAIL()
//	        					)
//	        					)
//		        				
//			        		
//			        		);
		        
		        //==========================end of second testing task ===================
		        
		        // agent3 with low priority
			      //agent with low weight priority
		        var testingTask3 = SEQ( 
		        		WHILEDO(
	        					(BeliefStateExtended b) -> GoalLibExtended.checkExploreAndTasks(b, testingTaskList, testAgent3, "lower", 6, totalTasks),
				        		
	        				SEQ(
	        					IFELSE(
				        			(BeliefStateExtended b) -> GoalLibExtended.lowWeightTasks(b,testAgent3, testingTaskList, 6),
					        		
					        		SEQ(
					        				GoalLibExtended.selectedNodeByTask(beliefState3,testAgent3,6, testingTaskList, "lower", distance),
							        		IF2(
							    	        	(BeliefStateExtended b) -> GoalLibExtended.entityTypePredicateByTask(beliefState3),							    	        
							    	        	dummy -> GoalLibExtended.navigateToDoorByTask(beliefState3, testingTaskList, testAgent3) ,
							    	        	dummy -> GoalLibExtended.navigateToButtonByTask(beliefState3, testingTaskList, testAgent3)					   	        
							        				)
					    	        		,
					    	        		//check if the selected node is a blocker. if it is, firstly check the prolog and then add a new goal if it is not solved.
					    	        		IFELSE(
					    	        				(BeliefStateExtended b) -> GoalLibExtended.checkEntityStatePredicate(beliefState3,testingTaskList,6, testAgent3.getId(),"lower"),GoalLibExtended.dynamicGoal(beliefState3,testAgent3,testingTaskList,beliefState,beliefState2),SUCCESS())			    	        					    	    
					    	        		,
					    	        		SUCCESS()
					        				)
					        		,
			        				 SEQ(
				    	        		FIRSTof(
				    	        				GoalLibExtended.newObservedNodes(testAgent3, testingTaskList),
				    	        				WHILEDO(
				    	        						(BeliefStateExtended b) -> GoalLibExtended.checkExplore(b, testingTaskList, totalTasks, "lower" ,6, distance),
				    	        						SEQ(
				    	        							//in the list of newly observed entities, if there is an entity from the list of tasks add it to the stack	
				    	        							GoalLibExtended.findNodes(testAgent3,beliefState3,testingTaskList)
				    	        						))
				    	        				)    		
				    	        		,		
				    	        		GoalLibExtended.checkExplore2(beliefState3, testingTaskList, totalTasks)   	        						
			        						 )     			
				        		)
	        					
	        					, FAIL()
	        					))
//		        		, 
//		        		GoalLibExtended.moveAgent(testAgent)
		        		
			        		
			        		);
		        
		        
	//=================================================================
		        // agent4 with higher than 10 for exploring only
				      
		        int threashhold4 = 11;
		        String type4 = "higher";
		        
		        
			        var testingTask4 = SEQ( 
			        		WHILEDO(
		        					(BeliefStateExtended b) -> GoalLibExtended.checkExploreAndTasks(b, testingTaskList, testAgent3, type4, threashhold4, totalTasks),
					        		
		        				SEQ(
		        					IFELSE(
					        			(BeliefStateExtended b) -> GoalLibExtended.lowWeightTasks(b,testAgent3, testingTaskList, threashhold4),
						        		
						        		SEQ(
						        				GoalLibExtended.selectedNodeByTask(beliefState3,testAgent3,threashhold4, testingTaskList, type4, distance),
								        		IF2(
								    	        	(BeliefStateExtended b) -> GoalLibExtended.entityTypePredicateByTask(beliefState3),							    	        
								    	        	dummy -> GoalLibExtended.navigateToDoorByTask(beliefState3, testingTaskList, testAgent3) ,
								    	        	dummy -> GoalLibExtended.navigateToButtonByTask(beliefState3, testingTaskList, testAgent3)					   	        
								        				)
						    	        		,
						    	        		//check if the selected node is a blocker. if it is, firstly check the prolog and then add a new goal if it is not solved.
						    	        		IFELSE(
						    	        				(BeliefStateExtended b) -> GoalLibExtended.checkEntityStatePredicate(beliefState3,testingTaskList,threashhold4, testAgent3.getId(),type4),GoalLibExtended.dynamicGoal(beliefState3,testAgent3,testingTaskList,beliefState,beliefState2),SUCCESS())			    	        					    	    
						    	        		,
						    	        		SUCCESS()
						        				)
						        		,
				        				 SEQ(
					    	        		FIRSTof(
					    	        				GoalLibExtended.newObservedNodes(testAgent3, testingTaskList),
					    	        				WHILEDO(
					    	        						(BeliefStateExtended b) -> GoalLibExtended.checkExplore(b, testingTaskList, totalTasks, type4 , threashhold4, distance),
					    	        						SEQ(
					    	        							//in the list of newly observed entities, if there is an entity from the list of tasks add it to the stack	
					    	        							GoalLibExtended.findNodes(testAgent3,beliefState3,testingTaskList)
					    	        						))
					    	        				)    		
					    	        		,		
					    	        		GoalLibExtended.checkExplore2(beliefState3, testingTaskList, totalTasks)   	        						
				        						 )     			
					        		)
		        					
		        					, FAIL()
		        					))
//			        		, 
//			        		GoalLibExtended.moveAgent(testAgent)
			        		
				        		
				        		);
			        var testingTask5 = SEQ( 
			        		WHILEDO(
		        					(BeliefStateExtended b) -> GoalLibExtended.checkExploreAndTasks(b, testingTaskList, testAgent3, type4, threashhold4, totalTasks),
				        				 SEQ(
					    	        		FIRSTof(
					    	        				GoalLibExtended.newObservedNodes(testAgent3, testingTaskList),
					    	        				WHILEDO(
					    	        						(BeliefStateExtended b) -> GoalLibExtended.checkExplore(b, testingTaskList, totalTasks, type4 , threashhold4, distance),
					    	        						SEQ(
					    	        							//in the list of newly observed entities, if there is an entity from the list of tasks add it to the stack	
					    	        							GoalLibExtended.findNodes(testAgent3,beliefState3,testingTaskList)
					    	        						))
					    	        				)    		
					    	        		,		
					    	        		GoalLibExtended.checkExplore2(beliefState3, testingTaskList, totalTasks)   	        						
					    	        		, FAIL()
				        						 )     			
				        				 
		        					)
//			        		, 
//			        		GoalLibExtended.moveAgent(testAgent)
			        		
				        		
				        		);
		        // attaching the goal and test data-collector
		        var dataCollector = new TestDataCollector();
		        testAgent1 . setTestDataCollector(dataCollector).setGoal(testingTask) ;
		        testAgent2 . setTestDataCollector(new TestDataCollector()).setGoal(testingTask2) ;
		        testAgent3 . setTestDataCollector(new TestDataCollector()).setGoal(testingTask5) ;
		        
		        environment.startSimulation();
		         // this will press the "Play" button in the game for you
		        //goal not achieved yet
		   //     assertFalse(testAgent.success());
		    //    assertFalse(testAgent2.success());
		      //  assertFalse(testAgent3.success());
		        String newFileLocation = "C:\\Users\\shirz002\\OneDrive - Universiteit Utrecht\\Samira\\Ph.D\\Multi Agent Automated Testing\\Multi-Agent" + File.separator +"result"+File.separator+"multi" +File.separator+fileName+label+".csv" ;		
				BufferedWriter br = new BufferedWriter(new FileWriter(newFileLocation));
				StringBuilder sb = new StringBuilder();
				List<int[]> timeWeight = new ArrayList<>();
				
				
				
		        // keep updating the agent
		        long startTime = System.currentTimeMillis();
	        
		        // Runnable agent2R_ = () -> testAgent1.update();
		      //  Runnable agent3R_ = () -> testAgent3.update();
		        try{
		    	 
		    	 var numberOItems = testingTaskList.getdoneTasks2().size();
				 DebugUtil.log(">> number of done tasks" + numberOItems  + numberOItems);
					
		    	// while (testingTask2.getStatus().inProgress() || numberOItems == totalTasks) {
		        while (testingTask.getStatus().inProgress() ||  testingTask2.getStatus().inProgress() || numberOItems == totalTasks) {
		        	//System.out.println("*** " + cycleNumber + ", " + testAgent1.getState().id + " @" + testAgent1.getState().worldmodel.position) ;				
				 Thread.sleep(100);
		            
		            cycleNumber++ ; 
		            System.out.println("=======================Agent2=======================  ") ;
		            testAgent2.update();	  	
		            testAgent2.updateGraph();
		        	// check if a button is just interacted:
					for(WorldEntity e: testAgent2.getState().changedEntities) {
						if(e.type.equals("Switch") && e.hasPreviousState()) {
							DebugUtil.log(">> detecting interaction with " + e.id) ;
							lastInteractedButton = e ;					
						}
					}
					// check doors that change state, and add connections to lastInteractedButton:
					if(lastInteractedButton != null) {
						for(WorldEntity e: testAgent2.getState().changedEntities) {							
							if(e.type.equals("Door") && e.hasPreviousState()) {
								try {
									beliefState2.prolog.registerConnection(lastInteractedButton.id,e.id) ;
									//add it to the Done list and remove from the toDo tasks list	
									if(beliefState2.isOpen(e.id)) {
										var itemExist = testingTaskList.getdoneTasks2().stream().filter(g-> e.id.equals(g)).collect(Collectors.toList());
										DebugUtil.log(">> removing from tasks" + itemExist  + itemExist.size());
										if(itemExist.size() == 0) {
											
											TestingTaskStack x  = (TestingTaskStack) testingTaskList.tasksList.stream().filter(s-> e.id.equals(s.getitemId())).findAny().orElse(null);
											DebugUtil.log(">> removing from tasks agent id and item" + testAgent2.getId() + " : " + e.id ) ;
											
											
											if(x != null && x.gettestedBy().size()< 2) {
												testingTaskList.setdoneTasks2(new ArrayList<>(List.of(e.id,testAgent2.getId())));
												System.out.println( " status " + x.getstatus());
											testingTaskList.tasksList.remove(x);
											}
										}
									}
								} catch (InvalidTheoryException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
							}	
						}
					}
		            
		            
		            System.out.println("=======================End Agent2=======================  ") ;
		            
		            System.out.println("*****************Agent1************************  ") ;
		        	
		           
		            //synchronization
//		            Thread thread1 = new Thread(agent2R_);
//		            thread1.start();
//		            thread1.join();
		            testAgent1.update();
		        	testAgent1.updateGraph();
		        	
		        	// check if a button is just interacted:
					for(WorldEntity e: testAgent1.getState().changedEntities) {
						if(e.type.equals("Switch") && e.hasPreviousState()) {
							DebugUtil.log(">> detecting interaction with " + e.id) ;
							lastInteractedButton = e ;					
						}
					}
				//	 check doors that change state, and add connections to lastInteractedButton:
					if(lastInteractedButton != null) {
						for(WorldEntity e: testAgent1.getState().changedEntities) {							
							if(e.type.equals("Door") && e.hasPreviousState()) {
								try {
									beliefState.prolog.registerConnection(lastInteractedButton.id,e.id) ;
									//add it to the Done list and remove from the toDo tasks list	
									if(beliefState.isOpen(e.id)) {
										DebugUtil.log(">> removing from tasks agent id and the item " + testAgent1.getId() + " : " +  e.id) ;
										var itemExist = testingTaskList.getdoneTasks2().stream().filter(g-> e.id.equals(g)).collect(Collectors.toList());
										DebugUtil.log(">> removing from tasks" + itemExist  + itemExist.size() );
										if(itemExist.size() == 0) {
											TestingTaskStack x = (TestingTaskStack) testingTaskList.tasksList.stream().filter(s-> e.id.equals(s.getitemId())).findAny().orElse(null);
											if(x != null && x.gettestedBy().size() < 2) {
												testingTaskList.setdoneTasks2(new ArrayList<>(List.of(e.id,testAgent1.getId())));
												System.out.println( " status " + x.getstatus());
											testingTaskList.tasksList.remove(x);
											}
										}
									}
								} catch (InvalidTheoryException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
							}	
						}
					}
		        	System.out.println("*****************End Agent1************************  ") ;
		        	
		            System.out.println("*****************Agent3************************  ") ;
		        	
			           
		            //synchronization
//		            Thread thread2 = new Thread(agent3R_);
//		            thread2.start();
//		            thread2.join();
		            
		            
		            testAgent3.update();
		        	testAgent3.updateGraph();
		        	
		        	// check if a button is just interacted:
					for(WorldEntity e: testAgent3.getState().changedEntities) {
						if(e.type.equals("Switch") && e.hasPreviousState()) {
							DebugUtil.log(">> detecting interaction with " + e.id) ;
							lastInteractedButton = e ;					
						}
					}
				//	 check doors that change state, and add connections to lastInteractedButton:
					if(lastInteractedButton != null) {
						for(WorldEntity e: testAgent3.getState().changedEntities) {							
							if(e.type.equals("Door") && e.hasPreviousState()) {
								try {
									beliefState3.prolog.registerConnection(lastInteractedButton.id,e.id) ;
									//add it to the Done list and remove from the toDo tasks list	
									if(beliefState3.isOpen(e.id)) {
										DebugUtil.log(">> removing from tasks agent id and the item " + testAgent3.getId() + " : " +  e.id) ;
										var itemExist = testingTaskList.getdoneTasks2().stream().filter(g-> e.id.equals(g)).collect(Collectors.toList());
										DebugUtil.log(">> removing from tasks" + itemExist  + itemExist.size() );
										if(itemExist.size() == 0) {
											TestingTaskStack x = (TestingTaskStack) testingTaskList.tasksList.stream().filter(s-> e.id.equals(s.getitemId())).findAny().orElse(null);
											if(x != null && x.gettestedBy().size() < 2) {
												testingTaskList.setdoneTasks2(new ArrayList<>(List.of(e.id,testAgent3.getId())));
												System.out.println( " status " + x.getstatus());
											testingTaskList.tasksList.remove(x);
											}
										}
									}
								} catch (InvalidTheoryException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
							}	
						}
					}
		        	System.out.println("*****************End Agent3************************  ") ;
		        	
		        	
		        	
					if(advanceSync) {	        	
		        	
			        	Utils.mergeBelief(testAgent1,testAgent2);
			          	Utils.mergeBelief(testAgent1,testAgent3);
			        	Utils.mergeBelief(testAgent2,testAgent1);		      
			        	Utils.mergeBelief(testAgent2,testAgent3);
			        	Utils.mergeBelief(testAgent3,testAgent2);
			        	Utils.mergeBelief(testAgent3,testAgent1);
		        	}	
		        	System.out.println("////////////////////////////////////////// testing tasks " )  ; 
		        	for (TestingTaskStack tasks : testingTaskList.tasksList) {
		        		System.out.println("id of the task in toDo list: " + tasks.getitemId() + " tested by: " + tasks.gettestedBy().toString() + " tried number: " + tasks.gettriedNumber() + " observed by the agent: " + tasks.getseenByAgent())  ; 
		        	}
		        	
//		        	for(Object tasks: testingTaskList.getdoneTasks()) {
//		        		Object[] objArray = (Object[]) tasks;	        		
//		        		System.out.println("tasks in Done list: " + objArray[0] + objArray[1]) ;
//		        	}
		        	int sum = 0;
		        	 
		        	 for (List<String> task : testingTaskList.getdoneTasks2()) {
		 	              System.out.println("tasks in Done list: " + task.get(0) + " " + task.get(1) );		 	               
		 	             if(testingTaskList.goalsTem.contains(task.get(0))) {
		 	            	sum = sum + 10;
		 	    			}else {
		 	    				sum = sum + 5;}		 	                
		 	        }


		        	long midTime = System.currentTimeMillis();
		        	var timeNowmili = (midTime - startTime) /1000 ;
		        	var timeNow = timeNowmili /60;
		        	 if(Math.floor(timeNow) == timeNow || Math.ceil(timeNow) == timeNow) {
		        		 System.out.println(">>>>>>>>>>>************  Time and the number of tasks: ********" + timeNow + " tasks: " + testingTaskList.getdoneTasks2().size() + " weight: " + sum);
		        		 int[] arr = {(int) timeNow,testingTaskList.getdoneTasks2().size(), sum};
		        		 timeWeight.add(arr);
		        	 }
		        	 
		        	 System.out.println("items in the lucked list size "  + testingTaskList.getluckedItems().size());
		        	 testingTaskList.getluckedItems().forEach(e-> System.out.println("items in the lucked list"  + e));
		        	 
		        	
					if(beliefState.worldmodel().health <= 0) {
						DebugUtil.log(">>>> the agent died. Aaaw.");
					//	throw new AgentDieException() ;
					}
	        	
		        	if (cycleNumber>20000) {
		        		break ;
		        	}
		        }
		        
	        }catch (Error e) {
	        	testingTask.printGoalStructureStatus() ;
	        	throw e ;
			}
		        long endTime = System.currentTimeMillis();
		        totalTime = endTime - startTime;
		        testingTask.printGoalStructureStatus();
		    //    testingTask2.printGoalStructureStatus();
		        
		        
		        testAgent1.printStatus();
		        testAgent2.printStatus();
		      //  testAgent3.printStatus();
		        
		        var agentneTimeStamss = testAgent1.getState().knownEntities();
		        
		        totalTime = endTime - startTime;
		        System.out.println("******run time******");
			    System.out.println(totalTime/1000);
			    System.out.println("******cycle number******");
			    System.out.println(cycleNumber);
			    
			    for (int[] a : timeWeight) {
			    	for (int b : a) {
			    		
						sb.append(b);
						sb.append(","); 
					}
					sb.append("\n"); 			
				}
			    
			    br.write(sb.toString());
				br.close();	
//		   		 prolog.report();
//		   		var trace2 = testAgent1
//						. getTestDataCollector()
//						. getTestAgentTrace(testAgent1.getId()).stream().map( e-> e.getFamilyName()).collect(Collectors.toList());
//				        ;
//				
//				 System.out.println("trace2  " + trace2) ;	   		 
	        }
	        finally { environment.close(); }
	
	
}
}