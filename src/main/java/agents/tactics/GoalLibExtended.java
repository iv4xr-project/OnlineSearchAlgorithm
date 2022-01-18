package agents.tactics;

import static nl.uu.cs.aplib.AplibEDSL.*;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

import com.sun.net.httpserver.Authenticator.Success;

import alice.tuprolog.InvalidTheoryException;
import static eu.iv4xr.framework.mainConcepts.ObservationEvent.*;
import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import eu.iv4xr.framework.spatial.Vec3;
import nl.uu.cs.aplib.mainConcepts.Goal;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.mainConcepts.GoalStructure.GoalsCombinator;
import nl.uu.cs.aplib.utils.Pair;
import world.BeliefState;
import world.BeliefStateExtended;
import world.HighLevelGraph;
import world.LabEntity;


public class GoalLibExtended extends GoalLib{

	 
		/*observe the neighbors objects(nodes) and updated the high-level graph*/
	   public static <State>GoalStructure neighborsObjects(TestAgent agent) {

		 Goal goal =  goal("Update the neighbrs graph")
	     		.toSolve((Pair<String,BeliefState> s) -> { 				
					if(s.fst == null) {
						System.out.println("There is no new entity/neighbore");
						return false;
						}
					return true;
					}
				)
	     		.withTactic(
	     				SEQ(
	                     TacticLibExtended.updateGragh(agent),
	                     ABORT())
	     				);  	
	 	return goal.lift();
	   } 
	 
	 
		/* explore the environment when there is no entity in the agent visibility range, until it sees a new entity*/
	   public static <State> GoalStructure exploreTill(TestAgent agent,BeliefStateExtended beliefState) {
		 var g = goal("explore")
		 			.toSolve( 				
		 					(BeliefState s) -> {
		 						System.out.println("Explore till: ");
							return false;}
		 					)
		 				.withTactic(			
		 					FIRSTof(
		                     TacticLibExtended.newExplore(),				   
		                     ABORT()
		                     )
		 				)
		 			.lift();
		 g.maxbudget(8);
		 //return g;
		 return FIRSTof(SEQ(lift((BeliefStateExtended b) -> GoalLibExtended.clearPath(beliefState)),g), SUCCESS()) ;
	   }
	 
	   public static Boolean clearPath(BeliefStateExtended belief) {
		   
			if( belief.getMemorizedPath() != null && !belief.getMemorizedPath().isEmpty()) {
				belief.clearGoalLocation();
				}
			return true;		
	   }
	   
		/*Check if there is still unvisited node to discover or not*/
	   public static  Boolean checkExplore(BeliefStateExtended belief) {
		  
		//var explore = belief.pathfinder.explore(belief.getGoalLocation(), BeliefStateExtended.DIST_TO_FACE_THRESHOLD);
		var explore = belief.pathfinder().explore( belief.worldmodel().getFloorPosition() ,BeliefStateExtended.DIST_TO_FACE_THRESHOLD);
		System.out.println("checkExplore: " + explore);
		if(explore != null)
		{
			System.out.println("checkExplore: " + true);
			return false;
		}
		return true;
	 }
		/**
		 * This method will invoke when a dynamic goal to open the blocked door is added to the goal structure.
		 * To decide when to abort this intermediate goal, we need to check two things.
		 * firstly, check if there is still unvisited node to discover
		 * secondly, if there is not, the agent should try all exist buttons for this blocked door to be
		 * sure non can open it. 
		 * */
	   public static  Boolean checkExploreAndButtons(BeliefStateExtended belief) {
		  	
		var explore = belief.pathfinder().explore( belief.worldmodel().getFloorPosition() ,BeliefStateExtended.DIST_TO_FACE_THRESHOLD);
		List<String> allButtons = new LinkedList<>() ;
		String currentBlockedNode = belief.highLevelGragh.currentBlockedEntity; 
		System.out.println("check Explore: " + explore);
		if(explore != null)
		{		
			System.out.println("checkExplore: " + true);
			return false;
		}else {
			// Collect all button in the high-level graph.
			belief.highLevelGragh.entities.forEach(button -> {
				if(button.id.contains("button"))
				allButtons.add(button.id);
			});
			//check the list of visited button for this blocked entity.
			for(int i=0; i< allButtons.size(); i++) {
				if(!belief.buttonDoorConnection.get(currentBlockedNode).contains(allButtons.get(i)))
					return false;
			}
		}
		return true;
	 }
	   
	 /**
	  * explore the game world to till the agent sees a new objects
	  * */
	   public static GoalStructure findNeighbors(TestAgent agent,BeliefStateExtended belief) {
		   System.out.println(">>>>>>find new neighbors");
		   return SEQ(
				  SEQ(  
						  lift((b) -> GoalLibExtended.startExploreRecorder(agent)),
						  GoalLibExtended.exploreTill(agent,belief)
						  ,lift((b) -> GoalLibExtended.endExploreRecorder(agent))
				   )
				   ,GoalLibExtended.neighborsObjects(agent));
		   
			
	 }
	   
	   /**
	    * recording the data 
	    * */
	   public static Boolean  startExploreRecorder(TestAgent agent) {
		   System.out.println("predicate: start to save the data in explore goal structure " );
		   agent.registerEvent(new TimeStampedObservationEvent("startExploreRecorder"));
		   return true;
  		     

	   }
	   
	   /**
	    * recording the data 
	    * */
	   public static Boolean  endExploreRecorder(TestAgent agent) {
		   System.out.println("predicate: end to save the data in explore goal structure " );
		   agent.registerEvent(new TimeStampedObservationEvent("endExploreRecorder"));
		   return true;
  		     

	   }
	   
	  /** Apply AStar algorithm to find a path to the given goal */
	   public static GoalStructure aStar(BeliefState belief, String goalId) {		 
		return goal("aStar")
	 			.toSolve( 				
	 					(Pair<Boolean,BeliefState> s) -> {
	 						System.out.println("aStar goal" + s.fst);
	 						if((s.fst) && s.snd.isOpen(goalId)) return true;
						return false;}
	 					)
	 				.withTactic(	
	 					SEQ(
	                     TacticLibExtended.applyAStar(goalId),				   
	                     ABORT()
	                     )
	 				)
	 			.lift();
	   } 
	  
		/*select a nearest node from a list of neighbors*/
	   public static GoalStructure ExtendedAStar(BeliefState belief,TestAgent agent, Vec3 goalPosition, String goalID) {		 
		   
		  Goal goal1 = goal("select nearest node to the agent position")
	       		. toSolve(
	       				(Pair<String,BeliefState> s) -> {
	       					System.out.println("select nearest node to the agent position " );
	       					if(s.fst != null) {System.out.println(">>there is a node: " ); return true;}
	       					return false;
	       		});

	   	
	   	  Goal goal2 =  goal("check the selectedd node was not visited before")
	   			. toSolve(
	       				(Pair<String,BeliefState> s) -> {
	       					if(s.fst != null) return true;
	       					return false;
	       		});	
	   	
//	   	  Goal goal3 =  goal("check the selected node is a blocked entity")
//	   			. toSolve(
//	       				(Pair<String,BeliefState> s) -> {
//	       					if(s.fst != null) return true;
//	       					return false;
//	       		});	
	   	
	   	 GoalStructure g1 = goal1.withTactic(
	   	        		SEQ( 
	   	        		   TacticLibExtended.selectNearestNode(goalID), 
	   	                   ABORT()
	   	                   )) 
	   	                .lift();
	   	 GoalStructure g2 = goal2.withTactic(SEQ(
	   	        		   TacticLibExtended.unvisitedNode(),
	                       ABORT())).lift();
	   	        
//	   	 GoalStructure g3 = goal3.withTactic(SEQ(
//	   	    		TacticLib.checkEntityStatus(agent),
//	                 ABORT())).lift();
	   return SEQ(g1,g2);   
	   
	   } 
	   
	   
	   public static GoalStructure selectInactiveButton(BeliefState belief,TestAgent agent) {		
		   
		   return  goal("select nearest inactive button to the agent position")
		       		. toSolve(
		       				(Pair<String,BeliefState> s) -> {
		       					System.out.println("select nearest inactive button to the agent position " );
		       					if(s.fst != null) {System.out.println(">>there is a button: " ); return true;}
		       					System.out.println(">> there is no button! " );
		       					return false;
		       		}).withTactic(
		   	        		SEQ( 
	 	   	        		   TacticLibExtended.selectInactiveButton(), 
	 	   	                   ABORT()
	 	   	                   )) 
	 	   	                .lift();
	   }
	   
	   
	   public static GoalStructure checkEntityStatus(TestAgent agent) {
		   
		  	  Goal goal3 =  goal("check the selectedd node is a blocked entity")
		     			. toSolve(
		         				(Pair<Integer,BeliefStateExtended> s) -> {
		         					System.out.println("check entity state" + s.fst);
		         					if(s.fst != null) {System.out.println("check entity state returns true");		
		         					return true;}
		         					return false;
		         		});	
		  	 GoalStructure g3 = goal3.withTactic(
		  			 SEQ(
		   	    		 TacticLibExtended.checkEntityStatus(agent),
		                 ABORT()
		                 )
		  			 ).lift();
		  	 
		   return g3;
	   }
		
	   /**
	    * The type of the selected entity is not identified, based on the type f the entity we call
	    * different method to reach it. 
	    * */
	   public static Boolean  entityTypePredicate(BeliefStateExtended belief) {
		   System.out.println("predicate: diagnose the type of selected entity " );
		   var entityId = belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id;
		   if(entityId.contains("door")) {
			   return true;
		   }
		   return false;
  		     

	   }
	   
	   /**
	    * Navigate to the selected button which the id nor the position is not known up front. 
	    * */
	   
	   public static GoalStructure navigateToButton(BeliefStateExtended b) {
		   return goal("This entity is in visible distance: navigate to button")
	         	  . toSolve((BeliefStateExtended belief) -> {
	 
	         		  var entityId = belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id; 		   
	         		  var e = (LabEntity) belief.worldmodel.getElement(entityId) ;
	         		  if (e==null) return false ;
	         		  System.out.println(">>>>>>>>>>> navigateTo id of the selected button: " + e.id);
	         		  return Vec3.sub(belief.worldmodel().getFloorPosition(), e.getFloorPosition()).lengthSq() <= 1 ;
    
	         	    })
	         	  . withTactic(
	                     FIRSTof(  
	                     TacticLibExtended.navigateTo(), //try to move to the entity
	                     TacticLibExtended.guidedExplore(), //find the entity
	                     ABORT())) 
	               . lift();
	   }
	   
	   /**
	    * Navigate to the selected button which the id nor the position is not known up front. 
	    * */
	   
	   public static GoalStructure navigateToDoor(BeliefStateExtended b) {
	      return   goal("This entity is in visible distance: navigate to door")
	         	  . toSolve((BeliefStateExtended belief) -> {
	 
	         		  var entityId = belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id; 		   
	         		  var entity = (LabEntity) belief.worldmodel.getElement(entityId) ;
	         		  if (entity==null) return false ;
	         		  System.out.println(">>>>>>>>>>> navigateTo id of the selected door: " + entity.id);
	         		  System.out.println(">>>>>>>>>>> navigateTo id of the selected door: distance " + Vec3.sub(belief.worldmodel().getFloorPosition(), entity.getFloorPosition()).lengthSq());
//	         		 return   (b.evaluateEntity(entityId, e -> b.age(e) == 0)
//	                		  
//	                		  );
	         		if(Vec3.sub(belief.worldmodel().getFloorPosition(), entity.getFloorPosition()).lengthSq() <= 1.5
        					&& (belief.evaluateEntity(entity.id, e -> belief.age(e) == 0)))
        			//if(belief.canReach(position) != null)
        			return true ;
                    return false;
    
	         	    })
	         	  . withTactic(
	                     FIRSTof(  
	                     TacticLibExtended.navigateToClosestReachableNode(), //try to move to the entity
	                     TacticLibExtended.guidedExplore(), //find the entity
	                     ABORT())) 
	               . lift();
		
	   }
	   
	   
	   /**
	    * This is the same as original goal structure, but the id of the entity is not given up front.
	    * */
	    public static GoalStructure entityInCloseRange(BeliefStateExtended b) {
	    	//define the goal
	        Goal goal = new Goal("This entity is closeby: ")
	        		    . toSolve((BeliefStateExtended belief) -> {
	                        //check if the agent is close to the goal position
	        		    	var entityId = belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id; 		   
	  	         		  	var entity = (LabEntity) belief.worldmodel.getElement(entityId) ;
	  	         		  	if (entity==null) return false ;
	                        return Vec3.dist(belief.worldmodel().getFloorPosition(),entity.getFloorPosition()) <= 1.5 ;
	                    });
	        //define the goal structure
	        return goal.withTactic(
	        		 FIRSTof(//the tactic used to solve the goal
	                   TacticLibExtended.navigateToCloseByPosition(),//move to the goal position
	                   TacticLibExtended.guidedExplore(), //explore if the goal position is unknown
	                   ABORT())) 
	        	  . lift();
	    }
	   
		/*Navigate to an entity while the id of the entity is not define at the beginning nor the position*/
	   public static GoalStructure navigateTo(BeliefStateExtended b) {
	       var goal1 = 
	         	  goal("This entity is in visible distance: navigate to")
	         	  . toSolve((BeliefStateExtended belief) -> {
	 
	         		  var entityId = belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id;
	         		  Boolean result ;    		   
	         		  var e = (LabEntity) belief.worldmodel.getElement(entityId) ;
	         		  if (e==null) return false ;
	         		//  var distsq = Vec3.sub(belief.worldmodel.getFloorPosition(), e.getFloorPosition());
	         		  System.out.println(">>>>>>>>>>> navigateTo id of the selected node: " + e.id);
	         		 if(entityId.contains("door")) {
	         			System.out.println("navigateTo a door: " + Vec3.dist(belief.worldmodel().getFloorPosition(),e.getFloorPosition()));
	         			result = Vec3.sub(belief.worldmodel().getFloorPosition(), e.getFloorPosition()).lengthSq() <= 4  ; 
	         			
	         			 }
	         		 else {
	         			System.out.println("navigateTo a button: " + e.id+ " ,dis, "+ Vec3.sub(belief.worldmodel().getFloorPosition(), e.getFloorPosition()).lengthSq()
	         					+ " , dis2, " + Vec3.dist(belief.worldmodel().getFloorPosition(), e.getFloorPosition())
	         					);

	         			result = Vec3.sub(belief.worldmodel().getFloorPosition(), e.getFloorPosition()).lengthSq() <= 1 ;
	         		 }
				        return result;
	         	    })
	         	  . withTactic(
	                     FIRSTof( 
	                     TacticLibExtended.navigateTo(), 
	                     TacticLibExtended.guidedExplore(), 
	                     ABORT())) 
	               . lift();
	       
		   return goal1;
		
	   }
	   
	   
	   
	   
	   
	   //If then combinator
//	   public static <State>GoalStructure IFTHEN(Predicate<State> p, GoalStructure subgoal) {
//		 GoalStructure[] subgoals = new GoalStructure[2];
//		 subgoals[0] =  GoalLib.lift____(p);
//			return new GoalStructure(GoalsCombinator.FIRSTOF,
//					subgoals[0]
//					,subgoal				
//					) ;
//	   }
	   //new Repeat structure
	 public static <State>GoalStructure lift____(Predicate<State> p) {
			 return goal("Lifting predicate to goal")
			            .toSolve((Boolean b) ->  b ) 
			            .withTactic(
			            		SEQ(
			            		action("lifting a predicate").do1((State belief)-> {
		     						return p.test(belief) ;
		     						}
		     						).lift()
			            		,
			                    ABORT()
			                    ))
			            .lift() ;
			
		 }
		 
	 public static <State> GoalStructure success__() {
		 return goal(String.format("success"))
	     		. toSolve((State belief) -> { return true;})
	     		. withTactic(action("").do1((State state) -> state).lift()) 
	     		.lift();
		
	  }
	 
	 public static <State>GoalStructure NEWREPEAT(Predicate<State> p, GoalStructure subgoal) {
		 GoalStructure[] subgoals = new GoalStructure[2];
		 subgoals[0] =  lift____(p);
		 subgoals[1] = success__();
			return new GoalStructure(GoalsCombinator.REPEAT, 
							new GoalStructure (GoalsCombinator.FIRSTOF,
							new GoalStructure (GoalsCombinator.SEQ , subgoals)
							,subgoal
							)				
					) ;
		}
	   
	 public static Boolean  openDoorPredicate(BeliefState belief,String id) {
		   System.out.println("predicate: " + id + belief.isOpen(id));
		   if(belief.isOpen(id)) {
	    		  return true; 
	    	   }
	    	   return false;

	   }
	      
	   /*Find a corresponding  button to the current blocked entity*/
	 public static GoalStructure findCorrespondingButton(BeliefStateExtended b, TestAgent agent) {	   

		   System.out.println(" find corresponding button");
			  
			   Goal goal2 = goal("approach the current node and interact with it")
			       		. toSolve((BeliefStateExtended belief) -> {
			  	         	System.out.println("approach the current node and interact with it");
			  	         		var entityId = belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id;		  	         	
			  	         		System.out.println("interacted button" + entityId + belief.isOn(entityId));
			  	         		//if(belief.isOn(entityId)) return true;
			  	         		return true;
			  	         	    });
			   
			  
			
			   Goal goalIndirectNeighbors = goal("look for the not direct neighbors for interactin")
			       		. toSolve(
			       				(Pair<String,BeliefState> s) -> {
			       					System.out.println("look for the not direct neighbors for interactin");
			       					if(s.fst != null) {System.out.println(">> there is a indirect button to interact: " ); return true;}			     					
			       					System.out.println(">> there is no indirect button to interact!!");
			       					return false;
			  	         	    });
			   // this goal structure should always returns false. because if there is a button, so a goal is
			   // added after this one. because of the goal combinator structure, this should be false that the
			   // next goal could be invoked.
			   	   
			   GoalStructure interact = goal2.withTactic(
	 	        		SEQ( 
	 		                TacticLibExtended.interact(),
	 	                    ABORT()
	 	                   )) 
	 	                .lift();
			
			   
			   GoalStructure indirectNeighbors = goalIndirectNeighbors.withTactic(
		        		SEQ( 
			                TacticLibExtended.indirectNeighbors(),
			                ABORT()
		                   )) 
		                .lift();	
			   
			   	   
			   
			   
			   return NEWREPEAT(
					   (BeliefStateExtended belif) -> GoalLibExtended.checkExploreAndButtons(belif),
					   SEQ(
							   //look for a button, if there is a neighbor select one, if not select indirect neighbor.after
							   //trying all has seen button, the agent will explore the world to find a inactive button.
							   // if there is no- button, it reset all buttons once(because of multi-connection setup)
							   FIRSTof(
									   GoalLibExtended.selectInactiveButton(b, agent),indirectNeighbors,
									   SEQ(GoalLibExtended.findNeighbors(agent,b),GoalLibExtended.selectInactiveButton(b, agent))						 
									   ),
							   //navigate to the selected button and interact with it
							   //if the agent can not navigate to it, means there is something that blocks its way
							   // therefore it has to unblocked the way first.
							   
							   // with out checking the prolog 
							    GoalLibExtended.navigateTo(b) , interact
							   // if we want to use prolog to unblock the agent, in the situaction that the path to the selected 
							   // button is locked
//							   SEQ( 
//								  IFELSE2(
//									   GoalLibExtended.navigateTo(b)
//									   , 
//									    SUCCESS()
//									   ,
//									  SEQ(
//											  findingAButtonToUnlockedAgent(b,agent),
//											  GoalLibExtended.navigateTo(b)	  
//											,removeDynamicGoal(agent, "temporaryDoor")
//											  
//											  )
//									  )
//								  ,interact
//								)
							  , 
							// with out checking the prolog
							  GoalLibExtended.ExplorationTo(b.worldmodel.getElement(b.highLevelGragh.currentBlockedEntity).position,b.highLevelGragh.currentBlockedEntity)
							  ,  checkBlockedEntityStatus(b, agent)
							//if we want to use prolog to unblock the agent
//							  SEQ(
//								   IFELSE2(
//										   GoalLibExtended.ExplorationTo(b.worldmodel.getElement(b.highLevelGragh.currentBlockedEntity).position,b.highLevelGragh.currentBlockedEntity)
//										   , 
//										    SUCCESS()
//										   ,
//										   //findingAButtonToUnlockedAgent
//										  SEQ(findingAButtonToUnlockedAgent(b,agent),GoalLibExtended.ExplorationTo(b.worldmodel.getElement(b.highLevelGragh.currentBlockedEntity).position,b.highLevelGragh.currentBlockedEntity), removeDynamicGoal(agent, "temporaryDoor"))
//										   )
//								   , checkBlockedEntityStatus(b, agent)
//								   )
							   
							   )
					   );
	   }
	   
	 
	public static GoalStructure findingAButtonToUnlockedAgent(BeliefStateExtended b, TestAgent agent) {
		return goal("using the prolog")
	       		. toSolve(
	       				(BeliefStateExtended belief) -> {
	       				System.out.println(">>checkin the prolog to find a button to unlocked the agent");
		  	         //	if(belief.isOpen(belief.highLevelGragh.currentBlockedEntity))
		  	         //	return true;
		  	         	return true;
	  	         	    }).withTactic(
		     		SEQ( 
			                TacticLibExtended.unlockAgent(b,agent),
		     			//TacticLibExtended.unlockAgentWithTheLastInteractedButton(b, agent),
			                ABORT()
		                )) 
             .lift();
		}
	   /**
	    * After facing with a blocked entity, the agent looks for a button to open it
	    * At the ends, it needs to check the blocked entity status.If it is open,
	    * to continue exploring forward, we again set the currentSelectedNode to the 
	    * door id. 
	    * */
	   public static GoalStructure checkBlockedEntityStatus(BeliefStateExtended b, TestAgent agent) {
	   
		   return goal("checking the blocked node's state")
		       		. toSolve(
		       				(BeliefStateExtended belief) -> {
		       				System.out.println(">>checking the blocked node's state: id, " + belief.highLevelGragh.currentBlockedEntity + ", status: " + belief.isOpen(belief.highLevelGragh.currentBlockedEntity));
		       				belief.highLevelGragh.currentSelectedEntity = belief.highLevelGragh.getIndexById(belief.highLevelGragh.currentBlockedEntity);
		       				if(belief.isOpen(belief.highLevelGragh.currentBlockedEntity)) {	
			  	         		System.out.println("selected node:::" + belief.highLevelGragh.currentSelectedEntity);
			  	         		return true;
			  	         	}  	
			  	         	return false;
		  	         	    }).withTactic(
	      		SEQ( 
	      			TacticLib.observe(),	
		                TacticLibExtended.checkBlockedEntityStatus(b,agent),
		                ABORT()
	                 )) 
	              .lift();	
	 
	 
	   }
	 

	 public static GoalStructure checkDoorState(String id) {
	    	Goal goal =  goal("Ckecking door state")
	        		.toSolve((BeliefState belief) -> { 
	        	       	       if(belief.isOpen(id)) {
	        	       	    		  return true; 
	        	       	    	   }
	        	       	    	   return false;
	        	       	       }
	        				)
	        		.withTactic(
	        				SEQ(
	                        TacticLib.observe(),
	                        ABORT()))
	        	
	        		;  
	    	
	    	return goal.lift();
	    }
	    
	    //To avoid repeating the previous dynamic goals that has been added, we need to remove them at the end of the testing task
	 public static GoalStructure removeDynamicGoal(TestAgent agent, String goalId) {	    	
	    	 return goal("Remove dynamicly added goal structures")
    		.toSolve((BeliefStateExtended belief) -> { 
    	       	    	   return true;
    	       	       }
    				)
    		.withTactic(
				action("remove dynamic goal if exist one").do1(
						(BeliefStateExtended belief)-> {
							String removedGoal;
							removedGoal = belief.highLevelGragh.currentBlockedEntity;
							if(goalId != null) removedGoal = goalId;
							System.out.println("check Blocked Entity Status " + removedGoal + goalId);
							if(!belief.goalsmap.isEmpty()) {
								//dynamic goal should be removed 
								System.out.println("remove a goal : " + belief.goalsmap.get(removedGoal));
								agent.remove(belief.goalsmap.get(removedGoal));	
								belief.goalsmap.clear();	
							}
							return belief;
							}).lift()
				)
    		.lift();  
	    }
	 
	 //check the belief to see if the goal is visible and it is open
	 public static GoalStructure finalGoal(String goalId) {		 
		   return goal("Check the final goal")				  
//		    		.toSolve( (Pair<Boolean,BeliefState> s) -> {
// 						System.out.println("aStar goal" + s.fst);
// 						if((s.fst)) return true;
//					return false;
//		    		})
	        		.toSolve((BeliefState belief) -> { 
     	       	       if(belief.isOpen(goalId)) {
     	       	    		  return true; 
     	       	    	   }
     	       	    	   return false;
     	       	       }
     				)
		    		.withTactic(
//						action("check if the goal is visible and open").do1(
//								(BeliefStateExtended belief)-> {
//									var goal = belief.highLevelGragh.getIndexById(goalId);
//			
//									if(!belief.isOpen(goalId)) {
//										//dynamic goal should be removed 
//										System.out.println("the goal is open ");	
//										return new Pair(true, belief);	
//									}
//									return new Pair(false, belief);	
//									}).lift()
//			               . on((BeliefStateExtended belief) -> {
//			            	   System.out.println("goal: " + belief.highLevelGragh.getIndexById(goalId));
//			            	   if(belief.highLevelGragh.getIndexById(goalId) != -1) return new Pair(true, belief); 
//			            	   return new Pair(false, belief); 
//			                    })
		    				SEQ(
		                            TacticLib.observe(),
		                            ABORT())
						)
		    		.lift();  
	   } 
	 
	/**
	 * Explore the world to go to the given path
	 *  */
	 public static GoalStructure ExplorationTo(Vec3 position, String id) {
	    	Goal goal =  goal("Explor to the given direction")
	        		.toSolve((BeliefState belief) -> { 
	        		//	System.out.println("position Has Seen" + position +" , "+ belief.worldmodel.getFloorPosition());      			       
	        			if(Vec3.sub(belief.worldmodel().getFloorPosition(), position).lengthSq() <= 1.5
	        					|| (belief.evaluateEntity(id, e -> belief.age(e) == 0)))
	        			//if(belief.canReach(position) != null)
	        			return true ;
	                    return false;
	        	       	       }
	        				)
	        		.withTactic(
	        			FIRSTof(
	        				//TacticLib.navigateTo(position),
	        				TacticLibExtended.navigateToClosestReachableNode(id),
	                        TacticLibExtended.guidedExplore(position),        				
	                        ABORT()
	                        )
	        			)        	
	        		;  
	    	return goal.lift();
	    }
	 
	 
	 
		/**
		 * Explore the world to go to the given path: here it is selected node 
		 *  */
		 public static GoalStructure ExplorationTo() {
			
     		Goal goal =  goal("Explor to the given direction")
		        		.toSolve((BeliefStateExtended belief) -> { 		       			     				        					
		        			var entityId = belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id;
  		           		   
			         		var ee = (LabEntity) belief.worldmodel.getElement(entityId) ;
			         		var position = ee.position;
		        			System.out.println("position Has Seen" + position +" , "+ belief.worldmodel().getFloorPosition());      					                    
		        			if(Vec3.sub(belief.worldmodel().getFloorPosition(), position).lengthSq() <= 1.5
		        					|| (belief.evaluateEntity(entityId, e -> belief.age(e) == 0)))
		        			//if(belief.canReach(position) != null)
		        			return true  ;
		                    return false;
		        	       	       }
		        				)
		        		.withTactic(
		        			FIRSTof(
		        				//TacticLib.navigateTo(position),
		        				TacticLibExtended.navigateToClosestReachableNode(),
		                        TacticLibExtended.guidedExplore(),        				
		                        ABORT()))		        	
		        		;  
		    	
		    	return goal.lift();
		    }

	/* Predicate to check an entity state*/ 
	 public static Boolean  checkEntityStatePredicate(BeliefStateExtended belief) {
		 System.out.println("check selected entity state, predicate: " );
		 var selectedNode = belief.highLevelGragh.currentSelectedEntity;
			if(selectedNode != null) {		
				var entity = belief.highLevelGragh.entities.get(selectedNode);
				if(entity.id.contains("door")){
					if(!belief.isOpen(entity.id)) {
						return true;
					}}
			
			}	   
	    	return false;

	   }

	 
	 //interact with a button and check the corresponding door
	 
	 public static GoalStructure interactWithAButtonAndCheckDoor(String buttonId, String doorID) {
		 System.out.println("chand bar miad interactWithAButtonAndCheckDoor");
		 return SEQ(GoalLib.entityInteracted(buttonId), GoalLib.entityStateRefreshed(doorID));
	 }
	 	
	 
	/**
	 * Checking the prolog data set to find a corresponding button to open the current blocked entity 
	 **/
	 public static GoalStructure checkPrologToFindACorrespondinButton(TestAgent agent) {
		 
		 Goal goal3 =  goal("check the prolog data set to find a corresponding button")
	     			. toSolve(
	         				(Pair<Boolean,BeliefStateExtended> s) -> {
	         					System.out.println("is there a button?" + s.fst);
	         					if(s.fst) {System.out.println("YES!!");
	         					return true;}
	         					System.out.println("NO!!");
	         					return false;
	         		});	
	  	 GoalStructure g3 = goal3.withTactic(
	  			 SEQ(
	   	    		 TacticLibExtended.lookForAbutton(agent),
	                 ABORT()
	                 )
	  			 ).lift();
	  	 return g3;
	 }
	 
	/** Look for a button to open the blocked selected entity
	 * If the selected entity is a blocked door, we firstly check the prolog
	 * data set to know is there any button related to this door, if not we
	 * interact with has seen buttons. 
	 * */
	   public static GoalStructure findingAButton(BeliefStateExtended b,TestAgent agent) {
		   //if we want to use the data constructed, uncomment below line
		   //return IFELSE2(checkPrologToFindACorrespondinButton(agent), checkBlockedEntityStatus(b, agent), checkKnownButtonToFindButton(agent));
	   return IFELSE2(FAIL(), checkBlockedEntityStatus(b, agent), checkKnownButtonToFindButton(agent));
	   }

	   /**
	    * Dynamically add a new goal in order to open the blocked entity. the goal is
	    * to interact with the selected node which has seen before, and check the blocked
	    * entity status. 
	    * */
	   public static GoalStructure checkKnownButtonToFindButton(TestAgent agent) {
		   return  goal("add a new goal to open the door, looking for a button to open the correspanding door")
	     			. toSolve(
	         				(Pair<Integer,BeliefStateExtended> s) -> {
	         					System.out.println("find a nearest button to interact" + s.fst);
	         					//if(s.fst != null) {System.out.println("there is a button");		
	         					//return true;}
	         					//this goal should always returns false because the parents combinator is FirstOF
	         					//if we use IFELSE combinator. therefore to add a new goal after this one and check it
	         					//this should return false. if we do not want to use IFELSE then the above comments are true. 
	         					return false;
	         		})	
	     			.withTactic(
			  			 SEQ(
			   	    		 TacticLibExtended.dynamicallyAddGoalToFindButton(agent),        
			   	    		 ABORT()
			                 )
		  			 ).lift();
	   }
}
