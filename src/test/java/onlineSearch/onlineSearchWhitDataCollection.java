package onlineSearch;

import agents.LabRecruitsTestAgent;
import agents.TestSettings;
import static agents.TestSettings.*;
import agents.tactics.GoalLib;
import agents.tactics.TacticLib;
import alice.tuprolog.InvalidTheoryException;
import au.com.bytecode.opencsv.CSVReader;
import environments.LabRecruitsConfig;
import environments.LabRecruitsEnvironment;
import eu.iv4xr.framework.mainConcepts.ObservationEvent.ScalarTracingEvent;
import eu.iv4xr.framework.mainConcepts.ObservationEvent.TimeStampedObservationEvent;
import eu.iv4xr.framework.mainConcepts.ObservationEvent.VerdictEvent;
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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
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
import world.BeliefState;

import static nl.uu.cs.aplib.AplibEDSL.*;
import static nl.uu.cs.aplib.agents.PrologReasoner.and;
import static nl.uu.cs.aplib.agents.PrologReasoner.not;

import world.BeliefStateExtended;
import agents.tactics.GoalLibExtended;

public class onlineSearchWhitDataCollection {

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
	    
	    //***datacollectore
	    Pair<String,Number>[] instrumenter(BeliefStateExtended st) {
	    	Pair<String,Number>[] out = new Pair[3] ;
	    	out[0] = new Pair<String,Number>("posx",st.worldmodel.position.x) ;
	    	out[1] = new Pair<String,Number>("posy",st.worldmodel.position.y) ;
	    	out[2] = new Pair<String,Number>("posz",st.worldmodel.position.z) ;	    	
	    	return out ;
	    }
	    
	    /**
	     * A test to verify that the east closet is reachable.
	     * @throws IOException 
	     */
	    @Test
	    public void closetReachableTest() throws InterruptedException, IOException {
	    	
	    	//String levelName = "";
	    	String levelName = "MutatedFiles\\MOSA\\selectedLevels\\1649941005014";
	    	String fileName = "LabRecruits_level-original";
	    	//String fileName = "moreRome-multiconnection";
	        // Create an environment
	    	var LRconfig = new LabRecruitsConfig(fileName,Platform.LEVEL_PATH +File.separator+ levelName) ;
	    	LRconfig.agent_speed = 0.1f ;
	    	LRconfig.view_distance = 4f;
	    	String treasureDoor = "door38";
	    	Vec3 goalPosition =  new Vec3(188f,0,44f); 
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

		       
		    //   var agentPosiion = environment.observe("agent1").position;
		       
		       
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
		        				(BeliefStateExtended b) -> GoalLibExtended.openDoorPredicate(b,treasureDoor)	
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
			    	        		FIRSTof(
			    	        				//if during the exploration to find a new entity, agent sees the goal, we check that it is open or not
			    	        				GoalLibExtended.finalGoal(treasureDoor),
			    	        				// if the goal is not achieved yet, we select an entity to navigate to it based on some specific policies
			    	        				//if the all neighbors of the current position has seen before
			    	        				GoalLibExtended.selectedNode(beliefState,testAgent,goalPosition, treasureDoor)
			    	        				)
			    	        		,		    	        		
			    	        		IFELSE(
			    	        				(BeliefStateExtended b) -> GoalLibExtended.entityTypePredicate(beliefState),
			    	        				//SEQ(GoalLibExtended.navigateToDoor(beliefState),lift((BeliefStateExtended b) -> GoalLibExtended.clearPath(beliefState)),GoalLibExtended.entityInCloseRange(beliefState)), 
			    	        				GoalLibExtended.navigateToDoor(beliefState),
			    	        				GoalLibExtended.navigateToButton(beliefState))
			    	        		,
			    	        		IFELSE(
			    	        				(BeliefStateExtended b) -> GoalLibExtended.checkEntityStatePredicate(beliefState),GoalLibExtended.dynamicGoal(beliefState,testAgent),FAIL())
			    	        		//,
			    	        		//GoalLibExtended.checkEntityStatus(testAgent)
			    	        		,
			    	        		GoalLibExtended.removeDynamicGoal(testAgent, null)
			    	     //   		,
			    	        //		GoalLibExtended.aStar(beliefState,"door3")	
			    	        		,			    	        		
			    	        		GoalLibExtended.finalGoal(treasureDoor)
		        				))		        		
		        		);
		        
		        
		    //    var testingTask = SEQ(GoalLibExtended.ExplorationTo(new Vec3(4,0,19)));  
		        // attaching the goal and test data-collector
		        var dataCollector = new TestDataCollector();
		        testAgent . setTestDataCollector(dataCollector).setGoal(testingTask) ;
	     
		        testAgent.withScalarInstrumenter(b -> instrumenter((BeliefStateExtended) beliefState));		       
		        testAgent.registerEvent(new TimeStampedObservationEvent());
		        
		        
		        environment.startSimulation();
		        
		       
		         // this will press the "Play" button in the game for you
		        //goal not achieved yet
		        assertFalse(testAgent.success());
		
		    	//testAgent.update();
                //(48,0,21)  (48,0,20)  25,0, 20  21,0,20
 	           
		        // keep updating the agent
		        long startTime = System.currentTimeMillis();
		        testAgent.registerEvent(new TimeStampedObservationEvent("startTest"));
		        while (testingTask.getStatus().inProgress()) {
		        	if (testAgent.getState().worldmodel.position != null) {
		        		dataCollector.registerEvent(testAgent.getId(), 
			        			new ScalarTracingEvent(
			        					new Pair("posx",testAgent.getState().worldmodel.position.x),
			        					new Pair("posy",testAgent.getState().worldmodel.position.y),
			        					new Pair("posz",testAgent.getState().worldmodel.position.z),
			        					new Pair("turn",cycleNumber),
			        					new Pair("tick",1)));
		        	}
		        	if(cycleNumber == 1) Thread.sleep(1500);
		        	System.out.println("*** " + cycleNumber + ", " + testAgent.getState().id + " @" + testAgent.getState().worldmodel.position) ;
		            Thread.sleep(100);
		            
		            cycleNumber++ ; 
		        	testAgent.update();	               	               
		        	
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
		        	if (cycleNumber>100000) {
		        		break ;
		        	}
		        }
		        dataCollector.saveTestAgentScalarsTraceAsCSV(testAgent.getId(),"visits.csv");
		        testAgent.registerEvent(new TimeStampedObservationEvent("endTest"));
		        long endTime = System.currentTimeMillis();
		        totalTime = endTime - startTime;
		        testAgent.printStatus();
		        
		        
		        System.out.println("******run time******");
			    System.out.println(totalTime/1000);
			    System.out.println("******cycle number******");
			    System.out.println(cycleNumber);
			        
		        
		    
		       ///*************************** data collector
		        long sumMiliSec = 0;
		        long sumMinutes = 0;
		        
		        //trace the information about exploration time
		        var traceExplore = testAgent
						. getTestDataCollector()
						. getTestAgentTrace(testAgent.getId()).stream().filter(e-> e.getFamilyName() == "startExploreRecorder" || e.getFamilyName() == "endExploreRecorder" ).collect(Collectors.toList());
				        ;
		        //System.out.println("trace2222 " + traceExplore + traceExplore.size()) ;
		        	         
		         //[start,end,start,end....]  
		        for(int i=0; i<traceExplore.size() ; i++) {
		        	
		        	if(traceExplore.get(i) != null &&  i % 2 == 0 && i < traceExplore.size()-1) {
		        		//System.out.println("trace " +  i + traceExplore.get(i)) ;
		        	    var diff = Duration.between (traceExplore.get(i).getTimestamp() , traceExplore.get(i+1).getTimestamp()).toMillis();
		        	    sumMiliSec = sumMiliSec + diff;
		        	  //  System.out.println("diff milliis  " +  diff) ;
		        	    var diff2 = Duration.between (traceExplore.get(i).getTimestamp() , traceExplore.get(i+1).getTimestamp()).toMinutes();
		        	  //  System.out.println("diff min  " +  diff2) ;
		        	    sumMinutes = sumMinutes + diff2;
		        	}        	
		        }
		        System.out.println("sum miliii " +  sumMiliSec + " , "+sumMiliSec/1000) ;
		        System.out.println("sum min  " + sumMinutes) ;
		        
		        // trace the name of the tried doors
		        List<String> traceTriedDoors = testAgent
						. getTestDataCollector()
						. getTestAgentTrace(testAgent.getId()).stream()
						. map(e -> e.getFamilyName()).collect(Collectors.toList());
				        ;
				        
			   traceTriedDoors.forEach(e -> {
				        	if(e != null  && e.contains("door"))
				        	System.out.println("traceTriedDoors  " + e);} );
				
		       
			   
			   
				 //trace the position of the agent
				 List<Map<String,Number>> tracePosition = testAgent
							. getTestDataCollector()
							. getTestAgentScalarsTrace(testAgent.getId())
					        . stream()
					        . map(event -> event.values) . collect(Collectors.toList());
				 
				// System.out.println("tracePosition  " + tracePosition) ;
				  
				 
				 // save the recorded data
				 // save the position 
				 try {
					testAgent.getTestDataCollector()
					 .saveTestAgentScalarsTraceAsCSV(testAgent.getId(),Platform.LEVEL_PATH +File.separator+"Result"+File.separator+"FBK" +File.separator+fileName+ "positionTraceViewDis.csv");							
					testAgent.getTestDataCollector()
					 .saveTestAgentEventsTraceAsCSV(testAgent.getId(),Platform.LEVEL_PATH +File.separator+"Result"+File.separator+"FBK"+File.separator+fileName+ "EventTraceViewDis.csv");
				 } catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			
				 
		        var agentneTimeStamss = testAgent.getState().knownEntities();
		   		// print the prolog data
		        prolog.report();  
		        System.out.println("get all connection : " );
				if(!beliefState.highLevelGragh.edges.isEmpty()) beliefState.highLevelGragh.getEntityConnections().forEach(e-> System.out.print(e.toString()));
				
				String newFileLocation = Platform.LEVEL_PATH + File.separator +"Result"+File.separator+"FBK" +File.separator+fileName+"all-info.csv" ;		
				BufferedWriter br = new BufferedWriter(new FileWriter(newFileLocation));
				StringBuilder sb = new StringBuilder();
				
				//add all connections
					for (ArrayList<String> a : beliefState.highLevelGragh.getEntityConnections()) {					
						for (String b : a) {
							sb.append(b);
							sb.append(","); 
						}
						sb.append("\n"); 
						}
	
					// add tried doors
					
					for (String a : traceTriedDoors) {
						if(a != null  && a.contains("door")) {
							sb.append("Tried Door");
							sb.append(a);
							sb.append(","); 
							sb.append("\n"); 
						}
					}
					
					
					// add exploration
					sb.append("exploration time");
					sb.append(","); 
					sb.append(sumMiliSec/1000);
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
					sb.append(totalTime/1000);
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
			   
					
	        }
	        finally { environment.close(); }
	
	
}
}