package agents.tactics;

import static nl.uu.cs.aplib.AplibEDSL.ABORT;
import static nl.uu.cs.aplib.AplibEDSL.FIRSTof;
import static nl.uu.cs.aplib.AplibEDSL.REPEAT;
import static nl.uu.cs.aplib.AplibEDSL.SEQ;
import static nl.uu.cs.aplib.AplibEDSL.action;
import static nl.uu.cs.aplib.AplibEDSL.goal;

import java.util.function.Predicate;

import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.spatial.Vec3;
import nl.uu.cs.aplib.mainConcepts.Goal;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.mainConcepts.GoalStructure.GoalsCombinator;
import nl.uu.cs.aplib.utils.Pair;
import world.BeliefState;
import world.BeliefStateExtended;
import world.LabEntity;


public class GoalLibExtended extends GoalLib{

	 
		/*observe the neighbors objects(nodes)*///BeliefState b
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
							return true;}
		 					)
		 				.withTactic(			
		 					FIRSTof(
		                     TacticLib.explore(),				   
		                     ABORT()
		                     )
		 				)
		 			.lift();
		// g.maxbudget(5);
		 return g;
	   }
	 
		/*Check if there is still unvisited node to be discovered or not*/
	   public static  Boolean checkExplore(BeliefStateExtended belief) {
		   System.out.println("belief.getCurrentWayPoint() " +belief.worldmodel.getFloorPosition() + "BeliefStateExtended.DIST_TO_FACE_THRESHOLD" + BeliefStateExtended.DIST_TO_FACE_THRESHOLD);
		//var explore = belief.pathfinder.explore(belief.getGoalLocation(), BeliefStateExtended.DIST_TO_FACE_THRESHOLD);
		var explore = belief.pathfinder.explore( belief.worldmodel.getFloorPosition() ,BeliefStateExtended.DIST_TO_FACE_THRESHOLD);
		System.out.println("checkExplore: " + explore);
		if(explore != null)
		{
			System.out.println("checkExplore: " + true);
			return false;}
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
	   public static GoalStructure ExtendedAStar(BeliefState belief,TestAgent agent) {		 

		   //ask why this does not work
		   //		return goal("slecet Nearst Node")
//		 			.toSolve( 				
//		 					(Pair<String,BeliefState> s) -> {
//		 						System.out.println("slecetNearstNode");
//		 						if(s.fst != null) return true;
//		 						return false;
//							})
//		 				.withTactic(	
//		 					SEQ(
//		                     TacticLib.selectNearestNode(),	
//		                     TacticLib.unvisitedNode(),
//		                     TacticLib.checkEntityStatus(agent),
//		                     ABORT()
//		                     )
//		 				)
//		 			.lift();
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
	   	
//	   	  Goal goal3 =  goal("check the selectedd node is a blocked entity")
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
	   
	   
	   public static GoalStructure checkEntityStatus(TestAgent agent) {
		   
		  	  Goal goal3 =  goal("check the selectedd node is a blocked entity")
		     			. toSolve(
		         				(Pair<Integer,BeliefState> s) -> {
		         					System.out.println("check entity state" + s.fst);
		         					if(s.fst != null) {System.out.println("check entity state returns true"); return true;}
		         					return false;
		         		});	
		  	 GoalStructure g3 = goal3.withTactic(SEQ(
		   	    		TacticLibExtended.checkEntityStatus(agent),
		                 ABORT())).lift();
		  	 
		   return g3;
	   }
		

	   
		/*Navigate to an entity while the id of the entity is not define at the beginning nor the position*/
	   public static GoalStructure navigateTo(BeliefStateExtended b) {
		   System.out.println("navigateTo");
	       var goal1 = 
	         	  goal("This entity is in interaction distance: [%s]")
	         	  . toSolve((BeliefStateExtended belief) -> {
	 
	         		  var entityId = belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id;
	         		           		   
	         		  var e = (LabEntity) belief.worldmodel.getElement(b.highLevelGragh.entities.get(b.highLevelGragh.currentSelectedEntity).id) ;
	         		  if (e==null) return false ;
	         		  var distsq = Vec3.sub(belief.worldmodel.getFloorPosition(), e.getFloorPosition()).lengthSq() ;
	         		// System.out.println("navigateTo: " + distsq + e.id);
	         		 if(entityId.contains("door")) {
	         			 if(distsq < 1.2) return true; 
	         			 }
	         		  if(distsq < 0.2) {System.out.println("navigateTo22222222");
	         		  return true ;} 
	         		  return false;
	         		  
	         	    })
	         	  . withTactic(
	                     FIRSTof( //the tactic used to solve the goal 
	                     TacticLibExtended.navigateTo(), //try to move to the entity
	                     TacticLib.explore(), //find the entity
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
		   System.out.println("predicate");
		   if(belief.isOpen(id)) {
	    		  return true; 
	    	   }
	    	   return false;

	   }
	   
	   
		/*Find a corresponding  button to the current blocked entity*/
	   public static GoalStructure findCorrespondingButton(BeliefStateExtended b, TestAgent agent) {	   
//		   var goal1 = 
//		         	  goal("find a corresponding button to opnen the current door")
//		         	  . toSolve((Pair<String,BeliefState> s) -> {
//		         		  System.out.println("findcoree");
//		         		  if(s.snd.isOn(s.snd.highLevelGragh.currentBlockedEntity))
//		         		  return true;
//		         		  return false;
//		         	    })
//		         	  . withTactic(
//		                     SEQ( 
//		                     TacticLib.selectNearestNode(), 
//		                     TacticLib.unvisitedNode(),
//		                     TacticLib.navigateTo(),
//		                     TacticLib.interact(),
//		                     TacticLib.checkBlockedEntityStatus(),
//		                     ABORT())) 
//		               . lift();
//		       
//			   return goal1;
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
			  	         		if(belief.isOn(entityId)) return true;
			  	         		return false;
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
			       					System.out.println("false");
			       					return false;
			  	         	    });
			   
			   GoalStructure g1 = goal1.withTactic(
	  	        		SEQ( 
	  	        		   TacticLibExtended.selectNearestNode(), 
	  	        		   TacticLibExtended.unvisitedNode(),
	  	                   ABORT()
	  	                   )) 
	  	                .lift();
			   
			   GoalStructure g2 = goal2.withTactic(
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
			   
			   GoalStructure g4 = goal4.withTactic(
		        		SEQ( 
			                TacticLibExtended.indirectNeighbors(),
			                ABORT()
		                   )) 
		                .lift();	
			   
			   return REPEAT(SEQ(FIRSTof(GoalLibExtended.ExtendedAStar(b, agent),g4,GoalLibExtended.findNeighbors(agent)),GoalLibExtended.navigateTo(b),g2,GoalLib.entityStateRefreshed(b.highLevelGragh.currentBlockedEntity),g3));
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
}
