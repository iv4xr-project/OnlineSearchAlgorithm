package agents.tactics;

import static nl.uu.cs.aplib.AplibEDSL.FIRSTof;
import static nl.uu.cs.aplib.AplibEDSL.action;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import eu.iv4xr.framework.extensions.pathfinding.AStar;
import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import eu.iv4xr.framework.spatial.Vec3;
import nl.uu.cs.aplib.agents.MiniMemory;
import nl.uu.cs.aplib.mainConcepts.Action;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.mainConcepts.Tactic;
import nl.uu.cs.aplib.utils.Pair;
import world.BeliefState;
import world.LabEntity;
import world.LabWorldModel;
import world.BeliefStateExtended;

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
		            	  
		            	  var id = belief.highLevelGragh.entities.get(element).id;
		            	  
		            	  var e = (LabEntity) belief.worldmodel.getElement(id) ;
		            	  
		            	  var p = e.getFloorPosition() ;		            	  		            	  		            	  		            	  		            	  		            		                			    			    		    			   
		    			    System.out.print("***row navigate to position " + p +"id "+ id +" , path "+ belief.findPathTo(p,false));
		    			    // find path to p, but don't force re-calculation
		    			    return belief.findPathTo(p,false) ;		    			    
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

	/* update the graph of objects after visiting new entities*/
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
	            		 System.out.println("applyAStar x" + x);
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
	public static Tactic selectNearestNode(String goalId) {
		
		Tactic nearestNode =  action("find the nearest neighbor").do1(
				(BeliefStateExtended belief)-> {
					System.out.println("apply different polices to select a node"); 
					var currentNode  = belief.highLevelGragh.currentSelectedEntity;
					float distance   = Float.valueOf(0); 
					ArrayList<Float> distances = new ArrayList<Float>();
					List<Integer> doors = new ArrayList<Integer>();
					Integer selectedNode = null; 
					var goalPosition = belief.highLevelGragh.goalPosition;
					/* this is the first step at the beginning*/
					if(currentNode == null) {						
						// if the goal is in the list of entities, it be selected as the next node
						if(belief.highLevelGragh.getIndexById(goalId) != -1)
						{
							System.out.println("The goal has seen: " + goalId);
							selectedNode = belief.highLevelGragh.getIndexById(goalId);
							belief.highLevelGragh.currentSelectedEntity = selectedNode;
							return new Pair(selectedNode,belief);	 
						}
						
						var agentLocation = belief.worldmodel.position;
						/* because it is the beginning of the game, all vertices are the first set of
						 * vertices that agent has seen. we choose the nearest one. 
						 * */
						for(int i = 0 ; i<belief.highLevelGragh.entities.size(); i++) {
							var goalDistance  = goalPosition != null ? Vec3.dist(belief.highLevelGragh.vertices.get(i), goalPosition) : null;
							var y  = Vec3.dist(belief.highLevelGragh.vertices.get(i), agentLocation);			
							if(distance == 0) {
								distance = y; 
								selectedNode = i;	
								//considering agent position and nearest node to the goal position
								if(goalPosition != null) {
								distances.add(y);
								distances.add(goalDistance);}
							}
							else {	
								if(goalPosition == null) {
							//just consider the nearest node to the agent position
								if(y>distance) { distance = y; selectedNode = i; }
								}else {
							//	System.out.println("distances"+ belief.highLevelGragh.entities.get(i).id+ distances.get(0) +","+ distances.get(1) +","+ y +","+  goalDistance);
							// considering agent position and nearest node to the goal position
								if((y>distances.get(0) && goalDistance < distances.get(1)) || 
										(y<distances.get(0) && goalDistance < distances.get(1)) ) {
									distances.set(0, y); distances.set(1, goalDistance);  selectedNode = i; 
									}	}							
							}
						}					
					} 
					else {
						
						// if the goal is in the list of entities, it be selected as the next node
						if(belief.highLevelGragh.getIndexById(goalId) != -1)
						{
							System.out.println("The goal has seen: " + goalId);
							selectedNode = belief.highLevelGragh.getIndexById(goalId);
							belief.highLevelGragh.currentSelectedEntity = selectedNode;
							return new Pair(selectedNode,belief);	 
						}
						//agent has seen some nodes before, in order to avoid loop, we need to select the
						// new node which is not visited before. 
						// get the neighbors of the current agent position(current node)
						var neighbors = belief.highLevelGragh.neighboursNew(currentNode);
						System.out.println(" select Nearest Node when it is not the first time" + neighbors + "currrentnode: " + currentNode);
						
						for(Integer element : neighbors) {
							//check if the node is not selected before: unvisitedNode
							if(!belief.highLevelGragh.visitedNodes.contains(element)) {
								if(belief.highLevelGragh.entities.get(element).id.contains("door")) {doors.add(element);}
								System.out.println(" neighbor of the current position" + element + belief.highLevelGragh.entities.get(element).id);
								var goalDistance  = goalPosition != null ? Vec3.dist(belief.highLevelGragh.vertices.get(element), goalPosition) : null;
								var y  = Vec3.dist(belief.highLevelGragh.vertices.get(element), belief.highLevelGragh.vertices.get(currentNode));
								
								if(distance == 0) {
									distance = y; 
									selectedNode = element;
									//considering agent position and nearest node to the goal position
									if(goalPosition != null) {
									distances.add(y);
									distances.add(goalDistance);}
								}
								else {
									if(goalPosition == null) {
										//just consider the nearest node to the agent position
										if(y<distance) { distance = y; selectedNode = element; }
									}else {
										// considering agent position and nearest node to the goal position
										if((y>distances.get(0) && goalDistance < distances.get(1)) || 
												(y<distances.get(0) && goalDistance < distances.get(1)) ) {
											distances.set(0, y); distances.set(1, goalDistance);  selectedNode = element; 
											}
										}
								}
							}	
						}	

					}
					var tempSelectedNode  = selectedNode;
					distances.clear();
					System.out.println("selected any entity in neighborhood: " + selectedNode);
					
					if(selectedNode == null) {
						System.out.println("there is no node in the neighborhood to select" ); 
						
						/*
						 * if there is no entity in the neighborhood to select, it means the agent has
						 * explore the word and there is noting more to discover. Therefore, we should
						 * select a node not in the neighborhood. there might be some doors and buttons
						 * in the entities'list, but we give the priority to the door
						 * firstly, we check if the goal is in the list.otherwise, we select a door which
						 * is near to the goal approximate position. 
						 */
						
						if(belief.highLevelGragh.getIndexById(goalId) != -1) {
							System.out.println("the goal has seen but has not selected before. now, we select the goal" ); 
							selectedNode = belief.highLevelGragh.getIndexById(goalId);
						}else {												
							for(int j = 0 ; j<belief.highLevelGragh.entities.size(); j++) {
								if(!belief.highLevelGragh.visitedNodes.contains(j)) {
									if(belief.highLevelGragh.entities.get(j).id.contains("door")) {doors.add(j);}
									var goalDistance  = goalPosition != null ? Vec3.dist(belief.highLevelGragh.vertices.get(j), goalPosition) : null;
									var y = Vec3.dist(belief.highLevelGragh.vertices.get(j), belief.highLevelGragh.vertices.get(currentNode));
									if(distance == 0) {
										distance = y; 
										selectedNode = j;
										//considering agent position and nearest node to the goal position
										if(goalPosition != null) {
										distances.add(y);
										distances.add(goalDistance);}
									}
									else {
										if(goalPosition == null) {
											//just consider the nearest node to the agent position
											if(y<distance) { distance = y; selectedNode = j; }
										}else {
										// considering agent position and nearest node to the goal position
											if((y>distances.get(0) && goalDistance < distances.get(1)) || 
												(y<distances.get(0) && goalDistance < distances.get(1)) ) {
												distances.set(0, y); distances.set(1, goalDistance);  selectedNode = j; 
											}
										}
									}
								}
							}									
						}
						System.out.println("Select one unchecked entity" + selectedNode); 
					}
					
					//if there are some unvisited doors in the neighbors, we give the priority to select between them.
					
					if(!doors.isEmpty() && !belief.highLevelGragh.entities.get(tempSelectedNode).id.contains("door")) {							
						if(doors.size()>1) {								
							distance = 0;	
							for(Integer element : doors) {
								var goalDistance  = goalPosition != null ? Vec3.dist(belief.highLevelGragh.vertices.get(element), goalPosition) : null;
								System.out.println("doors in neighborhood: " + element);
								var y  = Vec3.dist(belief.highLevelGragh.vertices.get(element), belief.highLevelGragh.vertices.get(currentNode));
								if(distance == 0) {
									distance = y; 
									selectedNode = element;
									//considering agent position and nearest node to the goal position
									if(goalPosition != null) {
										distances.add(y);
										distances.add(goalDistance);
									}	
								}
								else {
									if(goalPosition == null) {
										//just consider the nearest node to the agent position
										if(y<distance) { distance = y; selectedNode = element; }
										}
									else {
										// considering agent position and nearest node to the goal position
										if((y>distances.get(0) && goalDistance < distances.get(1)) || 
												(y<distances.get(0) && goalDistance < distances.get(1)) ) {
											distances.set(0, y); distances.set(1, goalDistance);  selectedNode = element; 
											}
										}
								}
							}
							
						
						}else {
							selectedNode = doors.get(0);
						}
					}
					System.out.println("selected a door in neighborhood: " + selectedNode);
					//end of selecting the nearest door
					
					/*
					 * in the situation that the current position of the agent is a open door means
					 * that the agent will start to explore (the algorithm structure) the world till it 
					 * sees an entity. It might go farther and find a new button which is near to the goal
					 * position(if the goal position is known). In this situation we give the priority to the 
					 * new entity rather than a door near to current position which was visited before.( two
					 * doors in neighborhood that we selected one of them to open). 
					 */
					if(!doors.isEmpty() && !belief.highLevelGragh.entities.get(tempSelectedNode).id.contains("door")) {	
						if(goalPosition != null && tempSelectedNode != null) {
							var goalDistanceTo1  =  Vec3.dist(belief.highLevelGragh.vertices.get(tempSelectedNode), goalPosition) ;
							var goalDistanceTo2  =   Vec3.dist(belief.highLevelGragh.vertices.get(selectedNode), goalPosition) ;
							var disBetweenToEntity  =   Vec3.dist(belief.highLevelGragh.vertices.get(selectedNode), belief.highLevelGragh.vertices.get(tempSelectedNode)) ;
							System.out.println("is the agent in the situation to select between a door or a button");
							if(goalDistanceTo1 < goalDistanceTo2 && disBetweenToEntity > belief.viewDistance) {
								selectedNode = tempSelectedNode;
							System.out.println("select a button which is near to the goal position rather than a door");
							}
						}
					}
					
					belief.highLevelGragh.currentSelectedEntity = selectedNode;
					System.out.println(">>selected node: " + belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id + 
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
						// the node is selected at the nearest selected tactic 
//						if(!belief.highLevelGragh.visitedNodes.contains(selectedNode)){
							belief.highLevelGragh.visitedNodes.add(selectedNode);
							return new Pair(selectedNode,belief);
//						}
//						return null;
					}
											
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
								//belief.highLevelGragh.visitedNodes.clear();
								//when we clean the all visited nodes to force agent to check the previous visited nodes
								// the agent won't add the current position which is the blocked node to the visited node
								belief.highLevelGragh.visitedNodes.add(belief.highLevelGragh.getIndexById(belief.highLevelGragh.currentBlockedEntity));
								//System.out.println("clear list");
								GoalStructure unblockedDoor = GoalLibExtended.findCorrespondingButton(belief,agent);
								agent.addAfter(unblockedDoor);	
								belief.goalsmap.put(entity.id,unblockedDoor);
								System.out.println("chand bar miad inja?");
								List<String> list = new ArrayList<>();
								belief.buttonDoorConnection.put(entity.id, list);						
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
					System.out.println(">>Check Blocked Entity Status " + blockedNode + belief.isOpen(blockedNode));
					return belief;
					}).lift();
		
		
			return  checkingState;	
	}	
    
	/* find the nearest indirect neighbors in order to interact with them to open the current blocked node*/
	public static Tactic indirectNeighbors() {	
		var checkingState =   action("find a button which is not a direct neighbor").do1(
				(BeliefStateExtended belief)-> {
					//var visitedNode = belief.highLevelGragh.visitedNodes;
					float distance   = Float.valueOf(0);
					ArrayList<Float> distances = new ArrayList<Float>();
					Integer selectedNode = null; 
					//var agentLocation = belief.highLevelGragh.currentSelectedEntity;
					// we always call this tactic when there we are looking for a button to open a blocked door
					// so, it should always select an entity based on this blocked entity
					var agentLocation  = belief.highLevelGragh.getIndexById(belief.highLevelGragh.currentBlockedEntity);
					var currentNodeId = belief.highLevelGragh.entities.get(agentLocation).id;
					var neighbor = belief.highLevelGragh.neighboursNew(agentLocation);
					var goalPosition = belief.highLevelGragh.goalPosition;
					//System.out.println("visited nodes to open a blocked door " + visitedNode);
					var entities = belief.highLevelGragh.entities;
					//if there is no direct neighbors to interact with them, check the other indirect neighbors
					// which means other entities in the list 
					System.out.println(">> all buttons that the agent has tried for a door: " +", door id: "+ belief.highLevelGragh.entities.get(agentLocation).id +", buttons: "+ belief.buttonDoorConnection.get(belief.highLevelGragh.entities.get(agentLocation).id ));
					for(int i=0; i<entities.size(); i++) {
						//	if(visitedNode.contains(i)) continue;
						//System.out.println("indirect neigh: " + entities.get(i).id +" state of the button " + belief.isOn(entities.get(i).id) );
						//System.out.println(" is the button selected for the current door before "+ (belief.buttonDoorConnection.get(currentNodeId).contains(entities.get(i).id)));
						// TODO we can also check the selected node is not in the neighbors set. 	
						if( belief.highLevelGragh.entities.get(i).type.equals(LabEntity.SWITCH) 
								//&&!belief.isOn(entities.get(i).id) 
								&&!(belief.buttonDoorConnection.get(currentNodeId).contains(entities.get(i).id))
								&& !entities.get(i).id.contains("door")) {	
							var goalDistance  = goalPosition != null ? Vec3.dist(belief.highLevelGragh.vertices.get(i), goalPosition) : null;	
							var y  = Vec3.dist(belief.highLevelGragh.vertices.get(i), belief.highLevelGragh.vertices.get(agentLocation));
								if(distance == 0) {
									distance = y; 
									selectedNode = i;
									//considering agent position and nearest node to the goal position
								if(goalPosition != null) {
									distances.add(y);
									distances.add(goalDistance);}
									}
								else {						
									if(goalPosition == null) {
										//just consider the nearest node to the agent position
										if(y<distance) { distance = y; selectedNode = i; }
									}else {
										// considering agent position and nearest node to the goal position
										if((y>distances.get(0) && goalDistance < distances.get(1)) || 
												(y<distances.get(0) && goalDistance < distances.get(1)) ) {
											distances.set(0, y); distances.set(1, goalDistance);  selectedNode = i; 
											}	
									}
								}	
							}							
					}
				//	System.out.println("indirect inactive button " + selectedNode + entities.get(selectedNode).id);
					if(selectedNode != null) {
						belief.highLevelGragh.currentSelectedEntity = selectedNode; 
						if(!belief.highLevelGragh.visitedNodes.contains(selectedNode)) belief.highLevelGragh.visitedNodes.add(selectedNode); 		
						belief.buttonDoorConnection.get(currentNodeId).add(belief.highLevelGragh.entities.get(selectedNode).id);												
									
						return new Pair(selectedNode,belief);}
					return new Pair(null,belief);
					})
				.on((BeliefStateExtended belief) -> {		
					if(belief.highLevelGragh.entities.equals(belief.highLevelGragh.visitedNodes)) {System.out.println(">> All nodes are visited"); return null;}						
					return true;
				})
				.lift();
		
		
			return  checkingState;	
	}	
	/* Select the nearest node to the current agent position*/
	public static Tactic selectInactiveButton() {
		
		Tactic nearestNode =  action("find the nearest inactive button").do1(
				(BeliefStateExtended belief)-> {	
					// we always call this tactic when there we are looking for a button to open a blocked door
					// so, it should always select an entity based on this blocked entity
					var currentNode  = belief.highLevelGragh.getIndexById(belief.highLevelGragh.currentBlockedEntity);
					float distance   = Float.valueOf(0); 		
					Integer selectedNode = null; 
					//agent has interacted with some nodes before, in order to avoid loop, we need to select the
					// new node which is not interacted before. 
					var agentLocation = currentNode;
					String currentNodeId = belief.highLevelGragh.entities.get(agentLocation).id;
					// get the neighbors of the current agent position(current node)
					var neighbors = belief.highLevelGragh.neighboursNew(agentLocation);
					System.out.println(" select nearest inactive button: the neighbors of the current position " + neighbors + "currrentnode: " + agentLocation);						
					
					
					for(Integer element : neighbors) {
						var entity  = belief.highLevelGragh.entities.get(element);
						//check if the node is not interacted before and it is not a door nor any other entity
						if(entity.type.equals(LabEntity.SWITCH)
						//		&& !belief.isOn(belief.highLevelGragh.entities.get(element).id) 
								&& !(belief.buttonDoorConnection.get(currentNodeId ).contains(entity.id))
								&& !entity.id.contains("door")				
								) {								
							System.out.println(" neighbor of the current position " + element + entity.id +currentNodeId);
							System.out.println(" is it set" + (belief.buttonDoorConnection.get(currentNodeId).contains(entity.id))								
									
									);
							var y  = Vec3.dist(belief.highLevelGragh.vertices.get(element), belief.highLevelGragh.vertices.get(agentLocation));							;						
							if(distance == 0) {
								distance = y; 
								selectedNode = element;}
							else {
								if(y<distance) { 
									distance = y; selectedNode = element; 
									
									
									}
							}
						}	
					}				
					
					if(selectedNode == null) {System.out.println("there is no button in the direct neighbourhood to interact" ); return new Pair(null,belief);}
					belief.highLevelGragh.currentSelectedEntity = selectedNode;					
					belief.buttonDoorConnection.get(currentNodeId).add(belief.highLevelGragh.entities.get(selectedNode).id);						
					System.out.println("after put: " + belief.buttonDoorConnection.get(currentNodeId));
					System.out.println("selected button whit the position: " + belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id + 
					belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).position);
					return new Pair(selectedNode,belief);				
					}).lift();
		
		return nearestNode;
	}
	
	/**
	 * Explore the word in the specific direction
	 * when the agent has seen the entity before,knows the position, but the path to the entity
	 * is not known yet. To guide the agent to the right direction, we give the position to the
	 * explore Tactic.    
	 * */
    public static Tactic guidedExplore(Vec3 destination) {

    	var memo = new MiniMemory("S0") ;
    	// three states:
    	//  S0 ; initial exploration state, a new exploration target must be set
    	//  inTransit: when the agent is traveling to the set exploration target
    	//  exhausted: there is no more exploration target left
    	//
    	
    	var explore_ =
    			unguardedNavigateTo("Explore: traveling to an exploration target")
    			. on((BeliefState belief) -> {  
    				System.out.println("guided explore" + memo.stateIs("S0") + memo.stateIs("inTransit")) ;
    				if(!memo.stateIs("inTransit")) {
    					 // in this state we must decide a new exploration target:
    					 System.out.println("if ### Explore: explore" ) ;
                         //get the location of the closest unexplored node
        				 var position = belief.worldmodel.getFloorPosition() ;        				 
        				 //System.out.println(">>> #explored nodes:" + belief.pathfinder.numberOfSeen()) ;
        				 var path = belief.pathfinder.explore(position,destination,BeliefState.DIST_TO_FACE_THRESHOLD,belief.viewDistance) ;   				 
        				 if (path==null || path.isEmpty()) {
        					memo.moveState("exhausted") ;
                            System.out.println("###*** no new and reachable navigation point found; agent is @" + belief.worldmodel.position) ;
                            return null ;
        				 }
        				 List<Vec3> explorationPath = path.stream()
        						            .map(v -> belief.pathfinder.vertices.get(v))
        						            .collect(Collectors.toList()) ;
        				         				 				         				 
        				 var target = explorationPath.get(explorationPath.size() - 1) ;        	
        				 System.out.println("###***** original : " + destination) ;
        				 System.out.println("###***** setting a new exploration target: " + target) ;
                         System.out.println("### abspath to exploration target: " + path) ;
                         System.out.println("### path to exploration target: " + explorationPath) ;
                         memo.memorized.clear();
                         memo.memorize(target);
                         memo.moveState("inTransit") ; // move the exploration state to inTransit...
                         return new Pair(target, explorationPath);//return the path finding information
    				}else {
                         
    					 System.out.println("***inTransit***" ) ;
    					 Vec3 exploration_target = (Vec3) memo.memorized.get(0) ;
                         // note that exploration_target won't be null because we are in the state
                         // in-Transit
                         Vec3 agentLocation = belief.worldmodel.getFloorPosition() ;
                         Vec3 currentDestination = belief.getGoalLocation() ;
                         var distToExplorationTarget = Vec3.dist(agentLocation,exploration_target) ;
                         if (distToExplorationTarget <= EXPLORATION_TARGET_DIST_THRESHOLD // current exploration target is reached
                             || currentDestination==null
                             || Vec3.dist(currentDestination,exploration_target) > 0.3) {
                        	 System.out.println("inTransit if part" ) ;
                        	 // in all these cases we need to select a new exploration target.
                        	 // This is done by moving back the exploration state to S0.
                        	 memo.moveState("S0");
                         }
                         if (distToExplorationTarget<=EXPLORATION_TARGET_DIST_THRESHOLD) {
                        	 System.out.println("### dist to explroration target " + distToExplorationTarget) ;
                         }
                         //System.out.println("### inTransit: " + distToExplorationTarget) ;
                         // System.out.println(">>> explore in-transit: " + memo.stateIs("inTransit")) ;
                         // System.out.println(">>> exploration target: " + exploration_target) ;
                         // We should not need to re-calculate the path. If we are "inTransit" the path is
                         // already in the agent's memory
                         // return new Tuple(g, belief.findPathTo(g));
                         System.out.println("inTransit"  + exploration_target) ;
                         return new Pair(exploration_target,null);
    				}
                     // in all other cases, the guard is not enabled:
    				// return null ;
                 })
               . lift();


        return FIRSTof(
        		 forceReplanPath(),
				 tryToUnstuck(),
				 explore_) ;
    }
	
    
    
    /* checking blocked entity status */
  	public static Tactic unlockAgent(BeliefState b, TestAgent agent) {			
  		var addNewGoal =   action("use prolog to help an agent to untrapped").do1(
  				(BeliefStateExtended belief)-> {
  					System.out.println("is it in prolog");
  					var isLocked = belief.prolog.isLockedInCurrentRoom();
  					System.out.println("is locked: " + isLocked + belief.highLevelGragh.currentSelectedEntity);
  					//if(!isLocked) return null;
  					String entityId = belief.highLevelGragh.currentBlockedEntity;	
  					var getEnablingButtons = belief.prolog.getEnablingButtons(entityId);
  					System.out.println("get Enabling Doors:size " + getEnablingButtons.size());
  					if(getEnablingButtons.size()== 0) return null;
  					float distance   = Float.valueOf(0);
  					//this should return the agent current position
  					Vec3 agentPosition = belief.worldmodel.position;
  					String selectedDoor = null;
  				    String selectedButon = null;
  					// if there is only one door select it otherwise select the nearest one to the agent position
  				    // if there is more than one set of door and button, select a set which the door
  					// is near to the agent position. suppose the agent was in its way and faced with 
  				    // the blocked door. So, it should select the one which is near to the agent position
  				    if(getEnablingButtons.size() == 1) {
  						var door = getEnablingButtons.keySet().toArray()[0];
  						System.out.println(door);
  						selectedDoor = door.toString();
  					}else {
	  					for(int i=0; i< getEnablingButtons.size(); i++) {	
	  						var door = getEnablingButtons.keySet().toArray()[i];
	  						var buttons = getEnablingButtons.values().toArray()[i];
	  						//map.get(map.keySet().toArray()[0]) to get the value of the first key
	  						var dist  = Vec3.dist(agentPosition, belief.worldmodel.getElement(door.toString()).position);
	  						System.out.print("get enabeling door and correspanding button: " + buttons + door );
	  						if(distance == 0) {
	  							distance = dist;
	  							selectedDoor = door.toString();
	  						}else {
	  							if(distance > dist) {
	  								distance = dist;
	  								selectedDoor = door.toString();
	  							}
	  						}
	  					} 					
  					}
  					
  					// get the set of buttons connected to the selected node
  					var setOFButtons = getEnablingButtons.get(selectedDoor);
  					System.out.print("setOFButtons" + setOFButtons + setOFButtons.size());
  					if(setOFButtons.size()> 1) {
  						//if there are more than one button selected to a door,
  						// select one which is near to the agent position. 
  						for(int j=0; j< setOFButtons.size(); j++) {
  							var button = setOFButtons.get(j);				
  							var dist = Vec3.dist(agentPosition, belief.worldmodel.getElement(button).position);
  						
	  						if(distance == 0) {
	  							distance = dist;
	  							selectedDoor = button;
	  						}else {
	  							if(distance > dist) {
	  								distance = dist;
	  								selectedDoor = button;
	  							}
	  						}
  						}
  					}else {
  						System.out.print("setOFButtons is set");
  						selectedButon = setOFButtons.get(0);
  					}
  					System.out.print("selected button and door" + selectedButon + selectedDoor);
  					// add a new goal to interact with the selected button, 
  					GoalStructure unblockedDoor = GoalLibExtended.interactWithAButtonAndCheckDoor(selectedButon,selectedDoor);
					agent.addAfter(unblockedDoor);
					
  					
					//the inserted goal dos'nt need to be removed after success because we remove the main 
				    //extra goal 
  				   //belief.goalsmap.put(selectedDoor,unblockedDoor);				
  					return belief;
  					}).lift();
		
  			return  addNewGoal;	
  	}	
    
    
	/**
	 * To unlock the agent we can consider the last interacted button as a cause of making this problem.
	 * Based on the information in the high-level graph, we know the last selected button. So, we can use
	 * prolog data set in order to get the connected door.   
	 * */
  	public static Tactic unlockAgentWithTheLastInteractedButton(BeliefState b, TestAgent agent) {			
  		var addingNewgoal =   action("use prolog to help an agent to untrapped").do1(
  				(BeliefStateExtended belief)-> {
  					System.out.println("is it in prolog");
  					var isLocked = belief.prolog.isLockedInCurrentRoom();
  					var currentNode = belief.highLevelGragh.currentSelectedEntity;
  					String buttonId = belief.highLevelGragh.entities.get(currentNode).id;
  					String selectedDoor = null;
  					System.out.println("is locked: " + isLocked + currentNode);
  					var getEnablingButtons = belief.prolog.getConnectedDoor(buttonId);
  					float distance   = Float.valueOf(0);
  					//this should return the agent current position
  					Vec3 agentPosition = belief.worldmodel.position;
  					
  					if(getEnablingButtons.size() == 1) {
  						var door = getEnablingButtons.get(0);
  						System.out.println(door);
  						selectedDoor = door.toString();
  					}else {
	  					for(int i=0; i< getEnablingButtons.size(); i++) {	
	  						var door = getEnablingButtons.get(i);	  				
	  						//map.get(map.keySet().toArray()[0]) to get the value of the first key
	  						var dist  = Vec3.dist(agentPosition, belief.worldmodel.getElement(door).position);
	  						System.out.print("get enabeling door " +  door );
	  						if(distance == 0) {
	  							distance = dist;
	  							selectedDoor = door;
	  						}else {
	  							if(distance > dist) {
	  								distance = dist;
	  								selectedDoor = door;
	  							}
	  						}
	  					} 					
  					}
  					
  					System.out.print("selected button and door" + buttonId + selectedDoor);
  					// add a new goal to interact with the selected button, 
  					GoalStructure unblockedDoor = GoalLibExtended.interactWithAButtonAndCheckDoor(buttonId,selectedDoor);
					agent.addAfter(unblockedDoor);
					
  					return belief;
  				}).lift();
  				return addingNewgoal;
  	}
  	
	/**
	 * Navigate to a navigation node closest to the given entity, and is moreover
	 * reachable by the agent. We consider the agent visibility range for selecting closest nodes.
	 */
	static Tactic navigateToClosestReachableNode(String id) {

		MiniMemory memory = new MiniMemory("S0") ;
		System.out.println("navigateToClosestReachableNode");
		Action move =
				unguardedNavigateTo("Navigate to a navigation vertex nearby " + id)

				. on((BeliefState belief) -> {

					LabEntity e = belief.worldmodel.getElement(id) ;
    			    if (e==null) return null ;

					Vec3 nodeLocation = null ;
					if (!memory.memorized.isEmpty()) {
						nodeLocation = (Vec3) memory.memorized.get(0) ;
					}
					Vec3 currentGoalLocation = belief.getGoalLocation() ;

					if (nodeLocation == null
					    || currentGoalLocation == null
					    || Vec3.dist(nodeLocation,currentGoalLocation) >= 0.05) {
						// in all these cases we need to calculate the node to go

    			        var entity_location = e.getFloorPosition() ;
	    			    List<Pair<Vec3,Float>> candidates = new LinkedList<>() ;
	    			    int k=0 ;
	    			    for (Vec3 v : belief.pathfinder.vertices) {
	    			    	if (belief.pathfinder.seenVertices.get(k) && (Vec3.dist(v, entity_location)) <= belief.viewDistance) {
	    			    		// v has been seen:
	    			    		candidates.add(new Pair(v, Vec3.dist(entity_location, v))) ;
	    			    	}
	    			    	k++ ;
	    			    }

		    		    if (candidates.isEmpty()) return null ;
		    		    // sort the candidates according to how close they are to the entity e (closest first)
		    		    candidates.sort((c1,c2) -> c1.snd.compareTo(c2.snd));
		    		    // now find the first one that is reachable:
		    		    System.out.println(">>> #candidates closest reachable neighbor nodes = " + candidates.size()) ;
		    		    Pair<Vec3,List<Vec3>> result = null ;
		    		    for(var c : candidates) {
		    			    result = belief.findPathTo(c.fst,true) ;
		    			    if (result != null) {
		    			        // found a reachable candidate!
		    			        System.out.println(">>> a reachable nearby node found :" + c.fst + ", path: " + result.snd) ;
		    			        memory.memorized.clear();
		    			        memory.memorize(result.fst);
		    			    	return result ;
		    			    }
		    		    }
		    			System.out.println(">>> no reachable nearby nodes :|") ;
		    			// no reachable node can be found. We will clear the memory, and declare the tactic as disabled
		    			memory.memorized.clear() ;
		    			return null ;
					}
					else {
						// else the memorized location and the current goal-location coincide. No need to
						// recalculate the path, so we will just return the pair (memorized-loc,null)
						return new Pair (nodeLocation,null) ;
					}
				}) ;

		return FIRSTof(
				 forceReplanPath(),
				 tryToUnstuck(),
				 move.lift()
			   )  ;
	}

	
	public static Tactic dynamiclyAddGoalToFindButton(TestAgent agent) {
		
		return  action("dynamicly add a goal to find a correspanding button to open the selected door").do1(
				(BeliefStateExtended belief)-> {
					System.out.println("damicly add a new goal to open the blocked door");
					var selectedNode = belief.highLevelGragh.currentSelectedEntity;
					var entity = belief.highLevelGragh.entities.get(selectedNode);
					
					belief.highLevelGragh.currentBlockedEntity = entity.id;
					//belief.highLevelGragh.visitedNodes.clear();
					//when we clean the all visited nodes to force agent to check the previous visited nodes
					// the agent won't add the current position which is the blocked node to the visited node
					belief.highLevelGragh.visitedNodes.add(belief.highLevelGragh.getIndexById(belief.highLevelGragh.currentBlockedEntity));
					
					GoalStructure unblockedDoor = GoalLibExtended.findCorrespondingButton(belief,agent);
					agent.addAfter(unblockedDoor);	
					System.out.println(" a new goal added");
					belief.goalsmap.put(entity.id,unblockedDoor);
					List<String> list = new ArrayList<>();
					belief.buttonDoorConnection.put(entity.id, list);	
					return new Pair(selectedNode,belief);						
					}).lift();
				
	}
	
	public static Tactic lookForAbutton(TestAgent agent) {
		
  		return action("checkig the prolog data set to find the corresponding button").do1(
  				(BeliefStateExtended belief)-> {
  					var currentNode = belief.highLevelGragh.currentSelectedEntity;
  					String entityId = belief.highLevelGragh.entities.get(currentNode).id;
  					belief.highLevelGragh.currentBlockedEntity = entityId;
  					String selectedButton = null;
  					System.out.println("current blocked selected node: " + entityId);
  					var getEnablingButtons = belief.prolog.getConnectedButton(entityId);
  					if(getEnablingButtons == null || getEnablingButtons.isEmpty()) return new Pair(false, belief);;
  					float distance   = Float.valueOf(0);
  					//this should return the agent current position
  					Vec3 agentPosition = belief.worldmodel.position;
  					
  					if(getEnablingButtons.size() == 1) {
  						selectedButton = getEnablingButtons.get(0);	
  					
  					}else {
	  					for(int i=0; i< getEnablingButtons.size(); i++) {	
	  						var button = getEnablingButtons.get(i);	  				
	  						//map.get(map.keySet().toArray()[0]) to get the value of the first key
	  						var dist  = Vec3.dist(agentPosition, belief.worldmodel.getElement(button).position);
	  						System.out.print("get a corresponding button " +  button );
	  						if(distance == 0) {
	  							distance = dist;
	  							selectedButton = button.toString();
	  						}else {
	  							if(distance > dist) {
	  								distance = dist;
	  								selectedButton = button.toString();
	  							}
	  						}
	  					} 					
  					}
  					
  					System.out.print("selected button" + selectedButton );
  					// add a new goal to interact with the selected button, 
  					GoalStructure unblockedDoor = GoalLibExtended.interactWithAButtonAndCheckDoor(selectedButton,entityId);
					agent.addAfter(unblockedDoor);
					belief.goalsmap.put(entityId,unblockedDoor);
					List<String> list = new ArrayList<>();
					belief.buttonDoorConnection.put(entityId, list);
  					return new Pair(true, belief);
  				}).lift();
	}
	

    //------------------ these two are just for testing **** should be removed------------------
    public static Tactic guidedExplore() {

    	var memo = new MiniMemory("S0") ;
    	// three states:
    	//  S0 ; initial exploration state, a new exploration target must be set
    	//  inTransit: when the agent is traveling to the set exploration target
    	//  exhausted: there is no more exploration target left
    	//
    	
    	var explore_ =
    			unguardedNavigateTo("Explore: traveling to an exploration target")
    			. on((BeliefStateExtended belief) -> {  
    				System.out.println("guided explore" + memo.stateIs("S0") + memo.stateIs("inTransit")) ;
    				Vec3 destination = belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).position;
    				 
    				if(!memo.stateIs("inTransit")) {
    					 // in this state we must decide a new exploration target:
    					 System.out.println("if ### Explore: explore" ) ;
                         //get the location of the closest unexplored node
        				 var position = belief.worldmodel.getFloorPosition() ;        				 
        				 //System.out.println(">>> #explored nodes:" + belief.pathfinder.numberOfSeen()) ;
        				 var path = belief.pathfinder.explore(position,destination,BeliefState.DIST_TO_FACE_THRESHOLD, belief.viewDistance) ;   				 
        				 if (path==null || path.isEmpty()) {
        					memo.moveState("exhausted") ;
                            System.out.println("###*** no new and reachable navigation point found; agent is @" + belief.worldmodel.position) ;
                            return null ;
        				 }
        				 List<Vec3> explorationPath = path.stream()
        						            .map(v -> belief.pathfinder.vertices.get(v))
        						            .collect(Collectors.toList()) ;
        				         				 				         				 
        				 var target = explorationPath.get(explorationPath.size() - 1) ;        	
        				 System.out.println("###***** original : " + destination) ;
        				 System.out.println("###***** setting a new exploration target: " + target) ;
                         System.out.println("### abspath to exploration target: " + path) ;
                         System.out.println("### path to exploration target: " + explorationPath) ;
                         memo.memorized.clear();
                         memo.memorize(target);
                         memo.moveState("inTransit") ; // move the exploration state to inTransit...
                         return new Pair(target, explorationPath);//return the path finding information
    				}else {
                         
    					 System.out.println("inTransit" ) ;
    					 Vec3 exploration_target = (Vec3) memo.memorized.get(0) ;
                         // note that exploration_target won't be null because we are in the state
                         // in-Transit
                         Vec3 agentLocation = belief.worldmodel.getFloorPosition() ;
                         Vec3 currentDestination = belief.getGoalLocation() ;
                         var distToExplorationTarget = Vec3.dist(agentLocation,exploration_target) ;
                         if (distToExplorationTarget <= EXPLORATION_TARGET_DIST_THRESHOLD // current exploration target is reached
                             || currentDestination==null
                             || Vec3.dist(currentDestination,exploration_target) > 0.3) {
                        	 System.out.println("inTransit if part" ) ;
                        	 // in all these cases we need to select a new exploration target.
                        	 // This is done by moving back the exploration state to S0.
                        	 memo.moveState("S0");
                         }
                         if (distToExplorationTarget<=EXPLORATION_TARGET_DIST_THRESHOLD) {
                        	 System.out.println("### dist to explroration target " + distToExplorationTarget) ;
                         }
                         //System.out.println("### inTransit: " + distToExplorationTarget) ;
                         // System.out.println(">>> explore in-transit: " + memo.stateIs("inTransit")) ;
                         // System.out.println(">>> exploration target: " + exploration_target) ;
                         // We should not need to re-calculate the path. If we are "inTransit" the path is
                         // already in the agent's memory
                         // return new Tuple(g, belief.findPathTo(g));
                         System.out.println("inTransit"  + exploration_target) ;
                         return new Pair(exploration_target,null);
    				}
                     // in all other cases, the guard is not enabled:
    				// return null ;
                 })
               .lift();


        return FIRSTof(
        		 forceReplanPath(),
				 tryToUnstuck(),
				 explore_) ;
    }
    
	/**
	 * Navigate to a navigation node closest to the given entity, and is moreover
	 * reachable by the agent.
	 */
	static Tactic navigateToClosestReachableNode() {

		MiniMemory memory = new MiniMemory("S0") ;
		
		Action move =
				unguardedNavigateTo("Navigate to a navigation vertex nearby ")

				. on((BeliefStateExtended belief) -> {
					
					//LabEntity e = belief.worldmodel.getElement(id) ;
					var entityId = belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id;
	           		   
	         		var e = (LabEntity) belief.worldmodel.getElement(entityId) ;
//					System.out.println("navigate to close reachable:*** goal location: " + belief.getGoalLocation()
//					+ "***door1 location: "   + "***agent position"+ belief.worldmodel.getFloorPosition()
//					+ "***path to entity position"
//					);
					
				//	if(belief.findPathTo(e.position,true) ==null) { return null;}
    			    
					if (e==null) return null ;
					
					Vec3 nodeLocation = null ;
					if (!memory.memorized.isEmpty()) {
						nodeLocation = (Vec3) memory.memorized.get(0) ;
					}
					
					Vec3 currentGoalLocation = belief.getGoalLocation() ;
					
					
					if(nodeLocation != null && currentGoalLocation != null)
					System.out.println("navigate to close reachable move:" + 
					(Vec3.dist(nodeLocation,currentGoalLocation)) + 
					 "selected node" + nodeLocation + " current goal location "+ currentGoalLocation);
					
					if (nodeLocation == null
					    || currentGoalLocation == null
					    || Vec3.dist(nodeLocation,currentGoalLocation) >= 0.05
					    ) {
						// in all these cases we need to calculate the node to go
						
    			        var entity_location = e.getFloorPosition() ;
    			        
	    			    List<Pair<Vec3,Float>> candidates = new LinkedList<>() ;
	    			    int k=0 ;
	    			    
	    			    
	    			    for (Vec3 v : belief.pathfinder.vertices) {
	    			    	if (belief.pathfinder.seenVertices.get(k)) {
	    			    		// v has been seen:
	    			    		candidates.add(new Pair(v, Vec3.dist(entity_location, v))) ;	    			    		
	    			    	}
	    			    	k++ ;
	    			    }
	    			    	    			   
	    			    
		    		    if (candidates.isEmpty()) return null ;
		    		    // sort the candidates according to how close they are to the entity e (closest first)
		    		    candidates.sort((c1,c2) -> c1.snd.compareTo(c2.snd));
		    		    // now find the first one that is reachable:
		    		    System.out.println(">>> #candidates closest reachable neighbor nodes = " + candidates.size()) ;
		    		    Pair<Vec3,List<Vec3>> result = null ;
		    		    for(var c : candidates) {
		    			    result = belief.findPathTo(c.fst,true) ;
		    			    if (result != null) {
		    			        // found a reachable candidate!
		    			        System.out.println(">>> a reachable nearby node found :" + c.fst + ", path: " + result.snd) ;
		    			        memory.memorized.clear();
		    			        memory.memorize(result.fst);
		    			    	return result ;
		    			    }
		    		    }
		    			System.out.println(">>> no reachable nearby nodes :|") ;
		    			// no reachable node can be found. We will clear the memory, and declare the tactic as disabled
		    			memory.memorized.clear() ;
		    			return null ;
					}				
					else {
						// else the memorized location and the current goal-location coincide. No need to
						// recalculate the path, so we will just return the pair (memorized-loc,null)
						System.out.println("navigate to close reachable: else part: " +  e.timestamp +"++"+ belief.age(entityId) );
						if(belief.age(entityId) != 0) {
					//		memory.memorized.clear() ;   
							return null;
							}
						return new Pair (nodeLocation,null) ;
						
					}
				}) ;

		return FIRSTof(
				 forceReplanPath(),
				 tryToUnstuck(),
				 move.lift()
			   )  ;
	}
	
	
}
