package agents.tactics;

import static nl.uu.cs.aplib.AplibEDSL.*;

import java.util.function.Predicate;

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

	 
		/*observe the neighbors objects(nodes)*/
	   public static <State>GoalStructure neighborsObjects(TestAgent agent) {

		 Goal goal =  goal("Update the neighbrs graph")
	     		.toSolve((Pair<String,BeliefState> s) -> { 				
					if(s.fst == null) {
						System.out.println("neighbors Objects goal");
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
	   public static <State> GoalStructure exploreTill(TestAgent agent) {
		 var g = goal("explore")
		 			.toSolve( 				
		 					(BeliefState s) -> {
		 						System.out.println("Explore till: ");
							return false;}
		 					)
		 				.withTactic(			
		 					FIRSTof(
		                     TacticLib.explore(),				   
		                     ABORT()
		                     )
		 				)
		 			.lift();
		 g.maxbudget(8);
		 //return g;
		 return FIRSTof(g, SUCCESS()) ;
	   }
	 
		/*Check if there is still unvisited node to be discovered or not*/
	   public static  Boolean checkExplore(BeliefStateExtended belief) {
		  
		//var explore = belief.pathfinder.explore(belief.getGoalLocation(), BeliefStateExtended.DIST_TO_FACE_THRESHOLD);
		var explore = belief.pathfinder.explore( belief.worldmodel.getFloorPosition() ,BeliefStateExtended.DIST_TO_FACE_THRESHOLD);
		System.out.println("checkExplore: " + explore);
		if(explore != null)
		{
			System.out.println("checkExplore: " + true);
			return false;
		}
		else {
			var goalId = "door5";
			if(belief.highLevelGragh.getIndexById(goalId) != -1) {
				belief.highLevelGragh.currentSelectedEntity = belief.highLevelGragh.getIndexById(goalId);
			}
				
		}
		return true;
	 }
	 
	   
	   public static GoalStructure findNeighbors(TestAgent agent) {
		   
		   return SEQ(GoalLibExtended.exploreTill(agent),GoalLibExtended.neighborsObjects(agent));
		   
			
	 }
	   
		/* Apply AStar algorithm to find a path to the given goal */
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
	   public static GoalStructure ExtendedAStar(BeliefState belief,TestAgent agent, Vec3 goalPosition) {		 
		   
		  Goal goal1 = goal("select nearest node to the agent position")
	       		. toSolve(
	       				(Pair<String,BeliefState> s) -> {
	       					System.out.println("select nearest node to the agent position " );
	       					if(s.fst != null) {System.out.println("there is a node" ); return true;}
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
	   	        		   TacticLibExtended.selectNearestNode(), 
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
		   
		   return  goal("select nearest button to the agent position")
		       		. toSolve(
		       				(Pair<String,BeliefState> s) -> {
		       					System.out.println("select nearest button to the agent position " );
		       					if(s.fst != null) {System.out.println("there is a button" ); return true;}
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
		  	 GoalStructure g3 = goal3.withTactic(SEQ(
		   	    		TacticLibExtended.checkEntityStatus(agent),
		                 ABORT())).lift();
		  	 
		   return g3;
	   }
		

	   
		/*Navigate to an entity while the id of the entity is not define at the beginning nor the position*/
	   public static GoalStructure navigateTo(BeliefStateExtended b) {
	       var goal1 = 
	         	  goal("This entity is in visible distance: navigate to")
	         	  . toSolve((BeliefStateExtended belief) -> {
	 
	         		  var entityId = belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id;
	         		           		   
	         		  var e = (LabEntity) belief.worldmodel.getElement(entityId) ;
	         		  if (e==null) return false ;
	         		//  var distsq = Vec3.sub(belief.worldmodel.getFloorPosition(), e.getFloorPosition());
	         		 System.out.println("navigateTo id of the selected node " + e.id);
	         		 if(entityId.contains("door")) {
	         			System.out.println("navigateTo22222222 door: " + Vec3.dist(belief.worldmodel.getFloorPosition(),e.getFloorPosition()));
	         			 return Vec3.dist(belief.worldmodel.getFloorPosition(),e.getFloorPosition()) <= 4 ;
//	         			 if(distsq < 1.2) {System.out.println("navigateTo22222222 door: " + entityId);
//		         		  return true ;} 
	         			
	         			 }
	         		 else {
	         			System.out.println("navigateTo22222222 button: " + e.id+ Vec3.sub(belief.worldmodel.getFloorPosition(), e.getFloorPosition()).lengthSq());
	         			 return	Vec3.sub(belief.worldmodel.getFloorPosition(), e.getFloorPosition()).lengthSq() <= 1 ;
	         		 }
	         		  
	         	    })
	         	  . withTactic(
	                     FIRSTof( //the tactic used to solve the goal 
	                     TacticLibExtended.navigateTo(), //try to move to the entity
	                    // TacticLibExtended.guidedExplore(), //find the entity
	                     TacticLib.explore(),
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
			   Goal goal1 = goal("find a corresponding button to opnen the current door")
					   . toSolve(
			       				(Pair<String,BeliefState> s) -> {
			       					System.out.println("find a corresponding button to opnen the current door");
			       					if(s.fst != null) return true;
			       					return false;
			       		});	
			   
			   Goal goal2 = goal("approach the current node and interact with it")
			       		. toSolve((BeliefStateExtended belief) -> {
			  	         	System.out.println("approach the current node and interact with it");
			  	         		var entityId = belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id;		  	         	
			  	         		System.out.println("interacted button" + entityId + belief.isOn(entityId));
			  	         		//if(belief.isOn(entityId)) return true;
			  	         		return true;
			  	         	    });
			   
			   Goal goal3 = goal("checking the blocked node's state")
			       		. toSolve(
			       				(BeliefStateExtended belief) -> {
			       				System.out.println("checking the blocked node's state" + belief.highLevelGragh.currentBlockedEntity + "status" + belief.isOpen(belief.highLevelGragh.currentBlockedEntity));
				  	         	if(belief.isOpen(belief.highLevelGragh.currentBlockedEntity))
				  	         	return true;
				  	         	return false;
			  	         	    });
			
			   Goal goal4 = goal("look for the not direct neighbors for interactin")
			       		. toSolve(
			       				(Pair<String,BeliefState> s) -> {
			       					System.out.println("look for the not direct neighbors for interactin");
			       					if(s.fst != null) {System.out.println("if");return true;}			     					
			       					return false;
			  	         	    });

			   
			   GoalStructure g1 = goal1.withTactic(
	  	        		SEQ( 
	  	        		   TacticLibExtended.selectNearestNode(), 
	  	        		   TacticLibExtended.unvisitedNode(),
	  	                   ABORT()
	  	                   )) 
	  	                .lift();
			   
			   GoalStructure interact = goal2.withTactic(
	 	        		SEQ( 
	 		                TacticLibExtended.interact(),
	 	                    ABORT()
	 	                   )) 
	 	                .lift();
			   
			   GoalStructure g3 = goal3.withTactic(
		        		SEQ( 
			                TacticLibExtended.checkBlockedEntityStatus(b,agent),
			                ABORT()
		                   )) 
		                .lift();	
			   
			   GoalStructure indirectNeighbors = goal4.withTactic(
		        		SEQ( 
			                TacticLibExtended.indirectNeighbors(),
			                ABORT()
		                   )) 
		                .lift();	
			   
			   
			   return REPEAT(
					   SEQ(
							   //look for a button, if there is a neighbor select one, if not select indirect neighbor.after
							   //trying all has seen button, the agent will explore the world to find a inactive button.
							   // if there is no- button, it reset all buttons once(because of multi-connection setup)
							   FIRSTof(
									   GoalLibExtended.selectInactiveButton(b, agent),indirectNeighbors,
									   SEQ(GoalLibExtended.findNeighbors(agent),GoalLibExtended.selectInactiveButton(b, agent))						 
									   ),
							   GoalLibExtended.navigateTo(b),interact,
							   
//					 		   SEQ(  GoalLib.entityStateRefreshed(b.highLevelGragh.currentBlockedEntity)	,				   
//							 GoalLib.entityInCloseRange(b.highLevelGragh.currentBlockedEntity)
//							 ,g3)
							   SEQ(
									   GoalLibExtended.ExplorationTo(b.worldmodel.getElement(b.highLevelGragh.currentBlockedEntity).position,b.highLevelGragh.currentBlockedEntity)
								//	   GoalLib.entityStateRefreshed(b.highLevelGragh.currentBlockedEntity)
									 //  ,
									  // GoalLib.positionInCloseRange(b.worldmodel.getElement(b.highLevelGragh.currentBlockedEntity).position).lift()
									   ,
									   g3)
									   
							   )
					   );
	   }
	   
	   //just for test
	 public static GoalStructure entityInCloseRangeTest(String entityId) {
	    	//define the goal
	        Goal goal = new Goal("This entity is closeby: " + entityId)
	        		    . toSolve((BeliefStateExtended belief) -> {
	                        //check if the agent is close to the goal position
	        		    	var e = belief.worldmodel.getElement(entityId) ;
	        		    	if (e == null) return false ;
	                        //return Vec3.dist(belief.worldmodel.getFloorPosition(),e.getFloorPosition()) <= 1 ;
	                        return Vec3.dist(belief.worldmodel.getFloorPosition(),e.getFloorPosition()) <= 1.55 ;
	                    });
	        //define the goal structure
	        return goal.withTactic(
	        		 FIRSTof(//the tactic used to solve the goal
	                   TacticLib.navigateToCloseByPosition(entityId),//move to the goal position
	                   TacticLib.explore(), //explore if the goal position is unknown
	                   ABORT())) 
	        	  . lift();
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
	 public static GoalStructure removeDynamicGoal(TestAgent agent) {	    	
	    	 return goal("Remove dynamicly added goal structures")
    		.toSolve((BeliefStateExtended belief) -> { 
    	       	    	   return true;
    	       	       }
    				)
    		.withTactic(
				action("remove dynamic goal if exist one").do1(
						(BeliefStateExtended belief)-> {
							var blockedNode = belief.highLevelGragh.currentBlockedEntity;
							System.out.println("checkBlockedEntityStatus " + blockedNode);							
							if(!belief.goalsmap.isEmpty()) {
								//dynamic goal should be removed 
								System.out.println("remove a goal : " + belief.goalsmap.get(blockedNode));
								agent.remove(belief.goalsmap.get(blockedNode));	
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
	 * Explore the word to go to the given path
	 *  */
	 public static GoalStructure ExplorationTo(Vec3 position, String id) {
	    	Goal goal =  goal("Explor to the given direction")
	        		.toSolve((BeliefState belief) -> { 
	        			System.out.println("position Has Seen" + position +" , "+ belief.worldmodel.getFloorPosition());      			
	                    
	        			if(Vec3.sub(belief.worldmodel.getFloorPosition(), position).lengthSq() <= 1.5
	        					|| (belief.evaluateEntity(id, e -> belief.age(e) == 0)))
	        			//if(belief.canReach(position) != null)
	        			return true  ;
	                    return false;
	        	       	       }
	        				)
	        		.withTactic(
	        			FIRSTof(
	        				//TacticLib.navigateTo(position),
	        				TacticLib.navigateToClosestReachableNode(id),
	                        TacticLibExtended.guidedExplore(position),        				
	                        ABORT()))
	        	
	        		;  
	    	
	    	return goal.lift();
	    }
	 
		/**
		 * Explore the word to go to the given path: here it is selected node 
		 *  */
		 public static GoalStructure ExplorationTo() {
			
     		Goal goal =  goal("Explor to the given direction")
		        		.toSolve((BeliefStateExtended belief) -> { 		       			     				        					
		        			var entityId = belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id;
  		           		   
			         		var ee = (LabEntity) belief.worldmodel.getElement(entityId) ;
			         		var position = ee.position;
		        			System.out.println("position Has Seen" + position +" , "+ belief.worldmodel.getFloorPosition());      					                    
		        			if(Vec3.sub(belief.worldmodel.getFloorPosition(), position).lengthSq() <= 1.5
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
}
