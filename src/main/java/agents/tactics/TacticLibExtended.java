package agents.tactics;

import static nl.uu.cs.aplib.AplibEDSL.FIRSTof;
import static nl.uu.cs.aplib.AplibEDSL.action;

import java.util.List;

import eu.iv4xr.framework.extensions.pathfinding.AStar;
import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import eu.iv4xr.framework.spatial.Vec3;
import nl.uu.cs.aplib.mainConcepts.Action;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.mainConcepts.Tactic;
import nl.uu.cs.aplib.utils.Pair;
import world.BeliefState;
import world.BeliefStateExtended;
import world.LabEntity;
import world.LabWorldModel;

public class TacticLibExtended extends TacticLib{

	/**
	 * To navigate to the location of an in-game element. Be mindful that the destination
	 * location is literally the location of the given game element. E.g. if it is a
	 * closed door, trying to literally get to its position won't work, since that position
	 * is blocked.
	 */
	public static Tactic navigateTo() {	
		return FIRSTof(
				 TacticLib.forceReplanPath(),
				 TacticLib.tryToUnstuck(),
				 rawNavigateTo()
			   )  ;
	}
	
    public static Tactic rawNavigateTo() {
	    	Action move = unguardedNavigateTo("Navigate to " )
				      // replacing its guard with this new one:
		              . on((BeliefStateExtended belief) -> {
		            	  //set the element you wan to navigate there while it is in the belief. 
		            	  var element  = belief.highLevelGragh.currentSelectedEntity;
		            	//  System.out.print("rownavigateto" + element);
		            	  if(element == null) return null;
		            	  else {
		            		var id = belief.highLevelGragh.entities.get(element).id;
		                	var e = (LabEntity) belief.worldmodel.getElement(id) ;
		    			    if (e==null) return null ;
		    			    var p = e.getFloorPosition() ;
		    		//	    System.out.print("rownavigateto position" + p);
		    			    // find path to p, but don't force re-calculation
		    			    return belief.findPathTo(p,false) ;
		    			    }
		                }) ;
		
		return move.lift() ;
	    }
	  
	  
	private static Action unguardedNavigateTo(String actionName) {
	    	Action move = action(actionName)
	                .do2((BeliefStateExtended belief) -> (Pair<Vec3,List<Vec3>> q)  -> {
	                	// q is a pair of (distination,path). Passing the destination is not necessary
	                	// for this tactic, but it will allows us to reuse the effect
	                	// part for other similar navigation-like tactics
	                	var destination = q.fst ;
	                	var path = q.snd ;

	                	//System.out.println("### tactic NavigateTo " + destination) ;

	                	//if a new path is received, memorize it as the current path to follow:
	                	if (path!= null) {
	                		belief.applyPath(belief.worldmodel.timestamp, destination, path) ;
	                	}
	                    //move towards the next way point of whatever the current path is:
	                	//System.out.println(">>> destination: " + destination) ;
	                	//System.out.println(">>> path: " + path) ;
	                	//System.out.println(">>> memorized dest: " + belief.getGoalLocation()) ;
	                	//System.out.println(">>> memorized path: " + belief.getMemorizedPath()) ;
	                	if (belief.getMemorizedPath() != null) {
	                		belief.worldmodel.moveToward(belief.env(),belief.getCurrentWayPoint());
	                		return belief ;
	                	}
	                	else return null ;
	                    })
	                // a dummy guard; override this when using this action:
	                .on((BeliefStateExtended belief) -> new Pair(null,null)) ;
	/* ----- conflict
	                    })
	                .on((BeliefState belief) -> {
	                	Vec3 currentDestination = belief.getGoalLocation() ;
	                	if (currentDestination==null || currentDestination.distance(position) >= 0.05
	                			|| !belief.mentalMap.hasActivePath()) {
	                		// the agent has no current location to go to, or the new goal location
	                		// is quite different from the current goal location, we will then calculate
	                		// a new path:
	                		var path = belief.findPathTo(position) ;
	                		if (path==null || path.length==0) return null ;
	                		return new Tuple(position,path) ;
	                	}
	                	else {
	                		// the agent is already going to the specified location. So there is
	                		// no need to calculate a new path. We will return a pair(position,null)
	                		// to signal this.
	                		return new Tuple(position,null) ;
	                	}}) ;
										*/
	    	return move ;
	    }

	/* update the graph of object after visiting new entities*/
	public static Tactic updateGragh(TestAgent agent) {
		
		return action("update high level gragh with new neighbors nodes")
                . do1((BeliefStateExtended belief)-> {
                    //State update   
                	//prints entities in the same time stamp
                	belief.newlyObservedEntities().forEach (	
                			e -> System.out.println("seen in the same time stamp " + e.id + " e timestam: " +  e.timestamp + " curenttimestamp : " 
                    + belief.worldmodel.timestamp + " id :" + e.id)	
                			);     	
                	var x = belief.newlyObservedEntities();
                	if(x == null) {System.out.println("newlyObservedEntities is null"); return new Pair(null,belief);}
                	//belief.mergeNewlyObservedEntities(belief.newlyObservedEntities());
                	var y = belief.mergeNewlyObservedEntities(belief.newlyObservedEntities());
                	if( y == false) { System.out.println("mergeNewlyObservedEntities is null"); return new Pair(null,belief); } 	
                	return new Pair(true,belief);
                }).lift();
}
	
	/* Apply AStar algorithm on the current graph*/
	public static Tactic applyAStar(String goalId) {
		System.out.println("applyAStar");
		 Tactic interact = action("Interact")
	               . do2((BeliefStateExtended belief) -> (Boolean e) -> {
	            	  if(!e) return new Pair(false, belief);
	            	     var aStar = new AStar();
	            	   // it always start to apply the algorithm from the first node in the graph
	            	   	 var start = 0;	            	   	
	            		 var goal = belief.highLevelGragh.getIndexById(goalId); 
	            		 System.out.println("applyAStar goal" + goal);
	            		 if(goal == -1) return new Pair(false, belief);
	            		 var x = aStar.findPath(belief.highLevelGragh, start  ,  goal);
	            		 if(x == null) return new Pair(false, belief);
	            		 return new Pair(true, belief);
	                    })
	               . on((BeliefStateExtended belief) -> {
	                	if (belief.highLevelGragh.entities.size() > 0) {return true;}
	                	return false ;
	                    })
	               . lift();
	        return interact;
	}

	/* Select the nearest node to the current agent position*/
	public static Tactic selectNearestNode() {
		
		Tactic nearestNode =  action("find the nearest neighbor").do1(
				(BeliefStateExtended belief)-> {
					System.out.println("nearestNode"); 
					var currentNode  = belief.highLevelGragh.currentSelectedEntity;
					float distance   = Float.valueOf(0);
					Integer selectedNode = null; 
					/* this is the first step at the beginning*/
					if(currentNode == null) {						
						var agentLocation = belief.worldmodel.position;
						/* because it is the beginning of the game, all vertices are the first set of
						 * vertices that agent has seen. we choose the nearest one. 
						 * */
						for(int i = 0 ; i<belief.highLevelGragh.entities.size(); i++) {
							var y  = Vec3.dist(belief.highLevelGragh.vertices.get(i), agentLocation);			
							if(distance == 0) {
								distance = y; 
								selectedNode = i;	
							}
							else {			
								if(y>distance) { distance = y; selectedNode = i; }
							}
						}					
					} 
					else {
						//agent has seen some nodes before, in order to avoid loop, wee ned to select the
						// new node which is not visited before. 
						var agentLocation = belief.highLevelGragh.currentSelectedEntity;
						// get the neighbors of the current agent position(current node)
						var neighbors = belief.highLevelGragh.neighboursNew(agentLocation);
						System.out.println(" select Nearest Node when it is not the first time" + neighbors + "currrentnode: " + agentLocation);
						for(Integer element : neighbors) {
							if(!belief.highLevelGragh.visitedNodes.contains(element)) {
								System.out.println(" neighbor of the current position" + element);
								var y  = Vec3.dist(belief.highLevelGragh.vertices.get(element), belief.highLevelGragh.vertices.get(agentLocation));
								if(distance == 0) {
									distance = y; 
									selectedNode = element;}
								else {
									if(y>distance) { distance = y; selectedNode = element; }
								}
							}	
						}					
					}
					if(selectedNode == null) {System.out.println("there is no node to select" ); return new Pair(null,belief);}
					belief.highLevelGragh.currentSelectedEntity = selectedNode;
					System.out.println("idddd " + belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id + 
					belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).position);
					return new Pair(selectedNode,belief);				
					}).lift();
		
		return nearestNode;
	}
	
	
	/* Selected node should be unvisited to avoid being stock in the loop */
	public static Tactic unvisitedNode() {
			
		return  action("check the node is not visited").do1(
				(BeliefStateExtended belief)-> {
					System.out.println("unvisitedNode");
					var selectedNode = belief.highLevelGragh.currentSelectedEntity;
					if(selectedNode != null) {						
//						if(!belief.highLevelGragh.visitedNodes.contains(selectedNode)){
							belief.highLevelGragh.visitedNodes.add(selectedNode);
							return new Pair(selectedNode,belief);
//						}
//						return null;
					}
					
					// if this list is empty means the agent is at the first step
//					if(belief.highLevelGragh.visitedNodes.isEmpty()) return true;
										
					return null;
					}).lift();
	}
	
	/* checking entity status in order to add a new subgoal if the status is blocked */
	public static Tactic checkEntityStatus(TestAgent agent) {
			
		return  action("check the selected node's status").do1(
				(BeliefStateExtended belief)-> {
					System.out.println("checkEntityStatus");
					var selectedNode = belief.highLevelGragh.currentSelectedEntity;
					if(selectedNode != null) {		
						var entity = belief.highLevelGragh.entities.get(selectedNode);
						System.out.println("checkEntityStatus: " + entity.id);
						
						// if the current node is a door and it is blocked add a new goal
						if(entity.id.contains("door")){
							if(!belief.isOpen(entity.id)) {
								System.out.println("checkEntityStatus and add a new goal" + entity.id);
								belief.highLevelGragh.currentBlockedEntity = entity.id;
								belief.highLevelGragh.visitedNodes.clear();
								//when we clean the all visited nodes to force agent to check the previous visited nodes
								// the agent won't add the current position which is the blocked node to the visited node
								belief.highLevelGragh.visitedNodes.add(belief.highLevelGragh.getIndexById(belief.highLevelGragh.currentBlockedEntity));
								System.out.println("clear list");
								GoalStructure unblockedDoor = GoalLibExtended.findCorrespondingButton(belief,agent);
								agent.addAfter(unblockedDoor);	
								belief.goalsmap.put(entity.id,unblockedDoor);
							
							}
							//return new Pair(selectedNode,belief);
						}
						return new Pair(selectedNode,belief);	
					}
					return new Pair(selectedNode,belief);	
					}).lift();
				
	}
	
    /**
     * Send an interact command if the agent is close enough.
     * @param the object is selected dynamically
     * @return A tactic in which the agent will interact with the object
     */
    public static Tactic interact() {
        Tactic interact = action("Interact")
               . do2((BeliefStateExtended belief) -> (WorldEntity e) -> {
            	   System.out.println(">>> interact dynamicly " ) ;
                	  var obs = belief.worldmodel.interact(belief.env(), LabWorldModel.INTERACT, e)  ;
                	  belief.mergeNewObservationIntoWOM(obs);
                      return belief;
                    })
               . on((BeliefStateExtended belief) -> {
            	   
            	    var selectedNode = belief.highLevelGragh.currentSelectedEntity;
            	    if(selectedNode == null ) return null;
            	    var objectID = belief.highLevelGragh.entities.get(selectedNode).id;
                	var e = belief.worldmodel.getElement(objectID) ;
                	//System.out.println(">>>> " + objectID + ": " + e) ;
                	if (e==null) return null ;
                	System.out.println(">>> interact dynamicly " + e.id  + belief.worldmodel.canInteract(LabWorldModel.INTERACT, e)) ;
                	if (belief.worldmodel.canInteract(LabWorldModel.INTERACT, e)) {
                		return e ;
                	}
                	System.out.println(">>> cannot interact with " + e.id) ;
            		System.out.println("    Agent pos: " + belief.worldmodel.getFloorPosition()) ;
            		System.out.println("    Entity pos:" + e.getFloorPosition()) ;
            		System.out.println("    Entity extent:" + e.extent) ;
            		var dist = Vec3.dist(belief.worldmodel.getFloorPosition(), e.getFloorPosition()) ;
            		System.out.println("    Dist: " + dist) ;
            		
                	return null ;
                    })
               . lift();
        return interact;
    }

    
    /* checking blocked entity status */
	public static Tactic checkBlockedEntityStatus(BeliefState b, TestAgent agent) {
			
		var checkingState =   action("back to the current blocked entity to check the status").do1(
				(BeliefStateExtended belief)-> {
					var blockedNode = belief.highLevelGragh.currentBlockedEntity;
					System.out.println("checkBlockedEntityStatus " + blockedNode);
					if(blockedNode != null) {
						belief.highLevelGragh.currentSelectedEntity = belief.highLevelGragh.getIndexById(blockedNode);
						if(belief.isOpen(blockedNode)) { 
							//dynamic goal should be removed 
							//agent.remove(belief.goalsmap.get(blockedNode));;
							System.out.println("remove a goal : " + belief.goalsmap.get(blockedNode));
							return belief;}
						return belief;
					}
					return belief;
					}).lift();
		
		
			return  checkingState;	
	}	
    
	/* find the nearest indirect neighbors in order to interact with them to open the current blocked node*/
	public static Tactic indirectNeighbors() {
		
		var checkingState =   action("find a button which is not a direct neighbor").do1(
				(BeliefStateExtended belief)-> {
					var visitedNode = belief.highLevelGragh.visitedNodes;
					float distance   = Float.valueOf(0);
					Integer selectedNode = null; 
					var agentLocation = belief.highLevelGragh.currentSelectedEntity;
					System.out.println("visited nodes to open a blocked door " + visitedNode);
					var entities = belief.highLevelGragh.entities;
					//if there is no direct neighbors to interact with them, check the other indirect neighbors
					// which means other entities in the list 
					for(int i=0; i<entities.size(); i++) {
							if(visitedNode.contains(i)) continue;
							if(entities.get(i).getBooleanProperty("isOn") || entities.get(i).id.contains("door")) continue;
							
							var y  = Vec3.dist(belief.highLevelGragh.vertices.get(i), belief.highLevelGragh.vertices.get(agentLocation));
							if(distance == 0) {
								distance = y; 
								selectedNode = i;}
							else {
								if(y>distance) { distance = y; selectedNode = i; }
							}						
						
					}
					System.out.println("not visited node " + selectedNode);
					if(selectedNode != null) { belief.highLevelGragh.currentSelectedEntity = selectedNode; belief.highLevelGragh.visitedNodes.add(selectedNode); return new Pair(selectedNode,belief);}
					return new Pair(null,belief);
					})
				.on((BeliefStateExtended belief) -> {		
					if(belief.highLevelGragh.entities.equals(belief.highLevelGragh.visitedNodes)) {System.out.println(">> All nodes are visited"); return null;}						
					return true;
				})
				.lift();
		
		
			return  checkingState;	
	}	
}
