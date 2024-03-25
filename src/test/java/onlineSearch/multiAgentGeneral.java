package onlineSearch;

import agents.LabRecruitsTestAgent;
import agents.TestSettings;
import static agents.TestSettings.*;
import agents.tactics.GoalLib;
import agents.tactics.TacticLib;
import alice.tuprolog.InvalidTheoryException;
import environments.LabRecruitsConfig;
import environments.LabRecruitsEnvironment;
import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.mainConcepts.TestDataCollector;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import eu.iv4xr.framework.mainConcepts.WorldModel;
import eu.iv4xr.framework.spatial.Vec3;
import helperclasses.datastructures.linq.QArrayList;
import logger.JsonLoggerInstrument;
import nl.uu.cs.aplib.mainConcepts.BasicAgent;
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


public class multiAgentGeneral {

	 private static LabRecruitsTestServer labRecruitsTestServer;
	    @BeforeAll
	    static public void start() {
	    	// TestSettings.USE_SERVER_FOR_TEST = false ;
	    	// Uncomment this to make the game's graphic visible:
	    	// TestSettings.USE_GRAPHICS = true ;
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
	    	String fileName = "simple-level-second - basic level - 30";
	    	// Create an environment
	    	var LRconfig = new LabRecruitsConfig(fileName,Platform.LEVEL_PATH +File.separator+ levelName) ;
	    	LRconfig.agent_speed = 0.1f ;
	    	LRconfig.view_distance = 6f;
	    	String label = "-0-no priority-3";
	    	Vec3 goalPosition =  null;
	        var environment = new LabRecruitsEnvironment(LRconfig);
	        if(USE_INSTRUMENT) instrument(environment) ;
	        int cycleNumber = 0;
	        long totalTime = 0;
	        String finalResult = "null";
	        boolean advanceSync = true;
	        int distance = 100; 
	        int totalTasks = 10;
	        
	        try {
	        	if(TestSettings.USE_GRAPHICS) {
	        		System.out.println("You can drag then game window elsewhere for beter viewing. Then hit RETURN to continue.") ;
	        		new Scanner(System.in). nextLine() ;
	        	}
	        	
	        	TestingTaskStack testingTaskList = new TestingTaskStack();
	           
	        	// creating beliefs
	        	var beliefState1 = new BeliefStateExtended();
	        	var beliefState2 = new BeliefStateExtended();
	        	var beliefState3 = new BeliefStateExtended();
	        	var beliefState4 = new BeliefStateExtended();
	        	var beliefState5 = new BeliefStateExtended();
	        	
	        	// create a test agent
		        var testAgent1 = new LabRecruitsTestAgent("agent1") // matches the ID in the CSV file
	        		    . attachState(beliefState1)
	        		    . attachEnvironment(environment);    

		        
		        var testAgent2 = new LabRecruitsTestAgent("agent2") // matches the ID in the CSV file
	        		    . attachState(beliefState2)
	        		    . attachEnvironment(environment)
	        		    ;    
		        
		        var testAgent3 = new LabRecruitsTestAgent("agent3") // matches the ID in the CSV file
	        		    . attachState(beliefState3)
	        		    . attachEnvironment(environment)
	        		    ;    
		        
		        var testAgent4 = new LabRecruitsTestAgent("agent4") // matches the ID in the CSV file
	        		    . attachState(beliefState4)
	        		    . attachEnvironment(environment)
	        		    ;    
		        
		        var testAgent5 = new LabRecruitsTestAgent("agent5") // matches the ID in the CSV file
	        		    . attachState(beliefState5)
	        		    . attachEnvironment(environment)
	        		    ;
		      // creating testing tasks
		        // agent4 with higher than 10 for exploring only
		      
		        //agent with high weight priority
			    var testingTask1 = testingTask(testAgent1, beliefState1,"higher",5 , testingTaskList,distance, totalTasks, beliefState2 );
			    //agent with low weight priority
			    var testingTask4 = testingTask(testAgent1, beliefState1,"lower",6 , testingTaskList,distance, totalTasks, beliefState2);
			  
			    //agent with low weight priority
		        var testingTask2 = testingTask(testAgent2, beliefState2, "lower", 6,   testingTaskList, distance, totalTasks, beliefState1);
		        //agent with low weight priority
			 //   var testingTask3 = testingTask(testAgent3, beliefState3,"lower",6 , testingTaskList,distance, totalTasks, beliefState1, beliefState2,beliefState4 );

			    // agent with random test selection
			    //var testingTaskRandom = testingTask(testAgent3, beliefState3,"random",6 , testingTaskList,distance, totalTasks, beliefState1, beliefState2);
			    
			    //just to explore
			    var testingTaskExplore = testingTaskExplore(testAgent4, beliefState4, "higher", 11, testingTaskList, distance,totalTasks);
			  //agent with low weight priority
			 //   var testingTask5 = testingTask(testAgent4, beliefState4,"lower",6 , testingTaskList,distance, totalTasks, beliefState1, beliefState2,beliefState3  );			    
			        
			  //agent with low weight priority
			//    var testingTask6 = testingTask(testAgent5, beliefState5,"lower",6 , testingTaskList,distance, totalTasks, beliefState1, beliefState2,beliefState3  );			    
			        
			       
			        
		        // attaching the goal and test data-collector

			    var dataCollector = new TestDataCollector();
		        testAgent1 . setTestDataCollector(dataCollector).setGoal(testingTask4) ;
		        testAgent2 . setTestDataCollector(new TestDataCollector()).setGoal(testingTask2) ;
//		        testAgent3 . setTestDataCollector(new TestDataCollector()).setGoal(testingTask3) ;
//		        testAgent4 . setTestDataCollector(new TestDataCollector()).setGoal(testingTask5) ;
//		        testAgent5 . setTestDataCollector(new TestDataCollector()).setGoal(testingTask6) ;
		        
		        environment.startSimulation();
		        String newFileLocation = "C:\\Users\\shirz002\\OneDrive - Universiteit Utrecht\\Samira\\Ph.D\\Multi Agent Automated Testing\\Multi-Agent" + File.separator +"result"+File.separator+"Random-Connection\\new" +File.separator+fileName+label+".csv" ;		
				BufferedWriter br = new BufferedWriter(new FileWriter(newFileLocation));
				StringBuilder sb = new StringBuilder();
				List<int[]> timeWeight = new ArrayList<>();
				long startTime =  System.currentTimeMillis();;
				
				
		        // keep updating the agent
		        

		        beliefState1.pathfinder().multifloor = false ;
		        beliefState2.pathfinder().multifloor = false ;
//		        beliefState3.pathfinder().multifloor = false ;
//		        beliefState4.pathfinder().multifloor = false ;
//		        beliefState5.pathfinder().multifloor = false ;
		        
		        Runnable agent1R_ = () -> doUpdateAgent(testAgent1, beliefState1, testingTaskList);
		        Runnable agent2R_ = () -> doUpdateAgent(testAgent2, beliefState2, testingTaskList);
//		        Runnable agent3R_ = () -> doUpdateAgent(testAgent3, beliefState3, testingTaskList);
//		        Runnable agent4R_ = () -> doUpdateAgent(testAgent4, beliefState4, testingTaskList);
//		        Runnable agent5R_ = () -> doUpdateAgent(testAgent5, beliefState5, testingTaskList);
		        
		        ArrayList<String> accidentalTasks = new ArrayList<>();
		        try{
		    	 
		    	 var numberOItems = testingTaskList.getdoneTasks2().size();
				 DebugUtil.log(">> number of done tasks" + numberOItems  );
					
		    //	 while (testingTask4.getStatus().inProgress() || numberOItems == totalTasks) {
		        while (testingTask4.getStatus().inProgress() ||  testingTask2.getStatus().inProgress() || numberOItems == totalTasks) {
		        	//System.out.println("*** " + cycleNumber + ", " + testAgent1.getState().id + " @" + testAgent1.getState().worldmodel.position) ;				
				 Thread.sleep(100);
				
//				 if(cycleNumber == 10) {
//					 System.out.println("Starting time>>>>>" + startTime);
//		            	startTime = System.currentTimeMillis();
//		            }
		            cycleNumber++ ; 
		            
		            Thread thread1 = new Thread(agent1R_);
		        	Thread thread2 = new Thread(agent2R_);
//		        	Thread thread3 = new Thread(agent3R_);
//		        	Thread thread4 = new Thread(agent4R_);
//		        	Thread thread5 = new Thread(agent5R_);
		        	
		        	thread1.start();
		        	thread2.start();
//		        	thread3.start();
//		        	thread4.start();
//		        	thread5.start();
		        	
		        	thread1.join();
		        	thread2.join();
//		        	thread3.join();	        	
//		        	thread4.join();	  
//		        	thread5.join();	  
		        	
					if(advanceSync) {      	
			        	Utils.mergeBelief(testAgent1,testAgent2);			          	
			        	Utils.mergeBelief(testAgent2,testAgent1);		      
			        	
			        	
			        	
			        	//When we have agent 3
//			        	Utils.mergeBelief(testAgent1,testAgent3);
//			        	Utils.mergeBelief(testAgent2,testAgent3);
//			        	Utils.mergeBelief(testAgent3,testAgent2);
//			        	Utils.mergeBelief(testAgent3,testAgent1);
			        	
			        	//When we have agent 4
//			        	Utils.mergeBelief(testAgent1,testAgent4);
//			        	Utils.mergeBelief(testAgent4,testAgent1);
//			        	Utils.mergeBelief(testAgent2,testAgent4);
//			        	Utils.mergeBelief(testAgent4,testAgent2);
//			        	Utils.mergeBelief(testAgent3,testAgent4);
//			        	Utils.mergeBelief(testAgent4,testAgent3);
			        	
			        	//When we have agent 5
//			        	Utils.mergeBelief(testAgent1,testAgent5);
//			        	Utils.mergeBelief(testAgent2,testAgent5);
//			        	Utils.mergeBelief(testAgent3,testAgent5);
//			        	Utils.mergeBelief(testAgent4,testAgent5);
//			        	Utils.mergeBelief(testAgent5,testAgent1);
//			        	Utils.mergeBelief(testAgent5,testAgent2);
//			        	Utils.mergeBelief(testAgent5,testAgent3);
//			        	Utils.mergeBelief(testAgent5,testAgent4);
//			        	
			        	   	
		        	}	
		        	System.out.println("////////////////////////////////////////// testing tasks " )  ; 
		        	for (TestingTaskStack tasks : testingTaskList.tasksList) {
		        		System.out.println("id of the task in toDo list: " + tasks.getitemId() + " tested by: " + tasks.gettestedBy().toString() + " tried number: " + tasks.gettriedNumber() + " observed by the agent: " + tasks.getseenByAgent())  ; 
			        	if(!accidentalTasks.contains(tasks.getitemId())) {
			        		accidentalTasks.add(tasks.getitemId());
			        		}
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
		 	    				sum = sum + 1;}		 	                
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
		        	 
		        	
					if(beliefState1.worldmodel().health <= 0) {
						DebugUtil.log(">>>> the agent died. Aaaw.");
					//	throw new AgentDieException() ;
					}
	        	
		        	if (cycleNumber>20000) {
		        		break ;
		        	}
		        }
		        
	        }catch (Error e) {
	        //	testingTask1.printGoalStructureStatus() ;
	        	throw e ;
			}
		        long endTime = System.currentTimeMillis();
		        totalTime = endTime - startTime;
		     //   testingTask1.printGoalStructureStatus();
		    //    testingTask2.printGoalStructureStatus();
		        
		        
		        testAgent1.printStatus();
		        testAgent2.printStatus();
		        testAgent3.printStatus();
		        
		        var agentneTimeStamss = testAgent1.getState().knownEntities();
		        
		        totalTime = endTime - startTime;
		        System.out.println("******run time******");
			    System.out.println(totalTime/1000  );
			    System.out.println("******cycle number******");
			    System.out.println(cycleNumber);
			    System.out.println("*****Number of tasks*****" + testingTaskList.getdoneTasks2().size());			    
			    System.out.println("*****Number of done tasks*****" + accidentalTasks.size()
			    		+ "number of accidental tasks: " + (totalTasks-accidentalTasks.size())
			    		);	
			    for(String j: accidentalTasks) {
			    	System.out.println("accidental tasks: " + j);
			    }
			    
			    for (int[] a : timeWeight) {
			    	for (int b : a) {
			    		
						sb.append(b);
						sb.append(","); 
					}
					sb.append("\n"); 			
				}
			    
			    sb.append("totalTime:");
			    sb.append(",");
			    sb.append(totalTime/1000);
			    sb.append("\n");
			    sb.append("number of cycle:");
			    sb.append(",");
			    sb.append(cycleNumber);
			    sb.append("\n");
			    sb.append("number of tasks:");
			    sb.append(",");
			    sb.append(testingTaskList.getdoneTasks2().size());
			    sb.append("\n");
			    sb.append("number of done tasks:(not accidental)");
			    sb.append(",");
			    sb.append(accidentalTasks.size());
			    sb.append("\n");
			    
			    
			    br.write(sb.toString());
				br.close();	   		 
	        }
	        finally { environment.close(); }
	
	
}
	    
	    void doUpdateAgent( LabRecruitsTestAgent testAgent, BeliefStateExtended beliefState,TestingTaskStack testingTaskList) {
        	
	    	System.out.println(">>>>>>>>Start Id of the agent:: " + testAgent.getId());
	    	testAgent.update();
	    	testAgent.updateGraph();
	    	// check if a button is just interacted:
			for(WorldEntity e: testAgent.getState().changedEntities) {
				if(e.type.equals("Switch") && e.hasPreviousState()) {
					DebugUtil.log(">> detecting interaction with " + e.id) ;
					lastInteractedButton = e ;					
				}
			}
		//	 check doors that change state, and add connections to lastInteractedButton:
			if(lastInteractedButton != null) {
				for(WorldEntity e: testAgent.getState().changedEntities) {							
					if(e.type.equals("Door") && e.hasPreviousState()) {
						try {
							beliefState.prolog.registerConnection(lastInteractedButton.id,e.id) ;
							//add it to the Done list and remove from the toDo tasks list	
							if(beliefState.isOpen(e.id)) {
								DebugUtil.log(">> removing from tasks agent id and the item " + testAgent.getId() + " : " +  e.id) ;
								var itemExist = testingTaskList.getdoneTasks2().stream().filter(g-> e.id.equals(g)).collect(Collectors.toList());
								DebugUtil.log(">> removing from tasks" + itemExist  + itemExist.size() );
								if(itemExist.size() == 0) {
									TestingTaskStack x = (TestingTaskStack) testingTaskList.tasksList.stream().filter(s-> e.id.equals(s.getitemId())).findAny().orElse(null);
									if(x != null && x.gettestedBy().size() < 2) {
										testingTaskList.setdoneTasks2(new ArrayList<>(List.of(e.id,testAgent.getId())));
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
			System.out.println(">>>>>>>>END Id of the agent:: " + testAgent.getId());
	    }
	    
	     GoalStructure testingTask(LabRecruitsTestAgent testAgent1, BeliefStateExtended beliefState1,String taskPriority, int taskThreshold,  TestingTaskStack testingTaskList, int distance, int totalTasks, BeliefStateExtended beliefState2 ) {
	        
			return  SEQ( 
	        		WHILEDO(
    					(BeliefStateExtended b) -> GoalLibExtended.checkExploreAndTasks(b, testingTaskList, testAgent1, taskPriority, taskThreshold, totalTasks),
    					SEQ(
    					IFELSE(
		        			(BeliefStateExtended b) -> GoalLibExtended.checkTasksList(b,testAgent1, testingTaskList, taskThreshold,taskPriority),
			        		
			        		SEQ(
			        				GoalLibExtended.selectedNodeByTask(beliefState1,testAgent1,taskThreshold, testingTaskList, taskPriority, distance),
			    	        		// navigate to selected node
			        	//	IFELSE(
			        		IF2(
			    	        	(BeliefStateExtended b) -> GoalLibExtended.entityTypePredicateByTask(beliefState1),
			    	        //	FIRSTof(GoalLibExtended.navigateToDoorByTask(beliefState, testingTaskList, testAgent1), SUCCESS()),
			    	        	dummy -> GoalLibExtended.navigateToDoorByTask(beliefState1, testingTaskList, testAgent1),
			    	        	dummy -> GoalLibExtended.navigateToButtonByTask(beliefState1, testingTaskList, testAgent1)
			    	        //	GoalLibExtended.navigateToButtonByTask(beliefState, testingTaskList, testAgent1)
			        				)
			    	        		,
			    	        		//check if the selected node is a blocker. if it is, firstly check the prolog and then add a new goal if it is not solved.
			    	        		IFELSE(
			    	        				(BeliefStateExtended b) -> GoalLibExtended.checkEntityStatePredicate(beliefState1,testingTaskList,taskThreshold, testAgent1.getId(),taskPriority),GoalLibExtended.dynamicGoal(beliefState1,testAgent1,testingTaskList,beliefState2),SUCCESS())		//, beliefState3, beliefState4	    	        					    	    
			    	        		,
			    	        		SUCCESS()
			        				)	
			        		
			        		,
			        		
	        				 SEQ(
		    	        		FIRSTof(
		    	        				GoalLibExtended.newObservedNodes(testAgent1, testingTaskList),
		    	        				WHILEDO(
		    	        						(BeliefStateExtended b) -> GoalLibExtended.checkExplore(b, testingTaskList, totalTasks, taskPriority,taskThreshold,distance),
		    	        						SEQ(
		    	        							//in the list of newly observed entities, if there is an entity from the list of tasks add it to the stack	
		    	        							GoalLibExtended.findNodes(testAgent1,beliefState1,testingTaskList)
		    	        						))
		    	        				),		
				    	        		GoalLibExtended.checkExplore2(beliefState2, testingTaskList, totalTasks) // if all tasks are solved no need to explore more
			        				)
		        		)
    					,FAIL()
    					))
	//        		, 
	//        		GoalLibExtended.moveAgent(testAgent1)
	        		
	        		);
	    }
	      
	     GoalStructure testingTaskExplore(LabRecruitsTestAgent testAgent, BeliefStateExtended beliefState,String taskPriority, int taskThreshold,  TestingTaskStack testingTaskList, int distance, int totalTasks) {
	    	 return SEQ( 
		        		WHILEDO(
	        					(BeliefStateExtended b) -> GoalLibExtended.checkExploreAndTasks(b, testingTaskList, testAgent, taskPriority, taskThreshold, totalTasks),
			        				 SEQ(
				    	        		FIRSTof(
				    	        				GoalLibExtended.newObservedNodes(testAgent, testingTaskList),
				    	        				WHILEDO(
				    	        						(BeliefStateExtended b) -> GoalLibExtended.checkExplore(b, testingTaskList, totalTasks, taskPriority , taskThreshold, distance),
				    	        						SEQ(
				    	        							//in the list of newly observed entities, if there is an entity from the list of tasks add it to the stack	
				    	        							GoalLibExtended.findNodes(testAgent,beliefState,testingTaskList)
				    	        						))
				    	        				)    		
				    	        		,		
				    	        		GoalLibExtended.checkExplore2(beliefState, testingTaskList, totalTasks)   	        						
				    	        		, FAIL()
			        						 )     			
			        				 
	        					)
//		        		, 
//		        		GoalLibExtended.moveAgent(testAgent)
		        		
			        		
			        		);
	     }
}