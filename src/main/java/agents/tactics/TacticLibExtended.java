package agents.tactics;

import static nl.uu.cs.aplib.AplibEDSL.FIRSTof;
import static nl.uu.cs.aplib.AplibEDSL.action;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import alice.tuprolog.InvalidTheoryException;
import eu.iv4xr.framework.extensions.pathfinding.AStar;
import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import eu.iv4xr.framework.mainConcepts.ObservationEvent.TimeStampedObservationEvent;
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
	
	/**
	 * It is the same as the original tactic but the id of the entity is not given. 
	 */
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
		            	  System.out.println("get door status in guided explore:" + belief.isOpen("door3") + belief.isOpen("door34") + belief.isOpen("door0") +belief.isOpen("door1") +belief.isOpen("door17")+belief.isOpen("door30") +belief.isOpen("door2") +belief.isOpen("door10"));
		    			    System.out.print("***row navigate to position " + p +"id "+ id +" , path "+ belief.findPathTo(p,false));
		    			    // find path to p, but don't force re-calculation
		    			    return belief.findPathTo(p,false) ;		    			    
		                }) ;
		
		return move.lift() ;
	    }
	  
	  /**
	   * This method is the same as the original tactic using BeliefStateExtended
	   */
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
	                		belief.env().moveToward(belief.id,belief.worldmodel().getFloorPosition(),belief.getCurrentWayPoint());	
	                		return belief ;
	                	}
	                	else return null ;
	                    })
	                // a dummy guard; override this when using this action:
	                .on((BeliefStateExtended belief) -> new Pair(null,null)) ;
	    	return move ;
	    }

	/**
	 *  Update the graph of objects/high-level graph after visiting new entities
	 **/
	public static Tactic updateGragh(TestAgent agent) {
		
		return action("update high level gragh with new visited neighbors/nodes")
                . do1((BeliefStateExtended belief)-> {
                    //State update   
                	//printing entities in the same time stamp
                	belief.newlyObservedEntities().forEach (	
                			e -> {System.out.println("Seen in the same time stamp " + e.id + " e timestam: " +  e.timestamp + " curenttimestamp : " 
                    + belief.worldmodel.timestamp + " id :" + e.id); 
                	}); 
                	
                	//get new observation
                	var newObserved = belief.newlyObservedEntities();
                	if(newObserved == null) {System.out.println("there is no entity in the new observation!! "); return new Pair(null,belief);}
               
                	//if there are new entity in the recent observation, merge them to the high-level graph
                	var mergeObserve = belief.mergeNewlyObservedEntities(belief.newlyObservedEntities());
                	if( mergeObserve == false) { System.out.println("there is not new entity to Merge!!"); return new Pair(null,belief); } 	
                	
                	return new Pair(true,belief);
                }).lift();
   }
	
	/**
	 *  Apply AStar algorithm on the current graph
	 *  */
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

	/**
	 *  Select the nearest node to the current agent position
	 *  */
	public static Tactic selectNearestNode(String goalId) {
		
		Tactic nearestNode =  action("find the nearest neighbor").do1(
				(BeliefStateExtended belief)-> {
					System.out.println("apply different polices to select a node"); 
					var currentNode  = belief.highLevelGragh.currentSelectedEntity;
					System.out.println(">>>>>>current Node: " + currentNode);
					float distance   = Float.valueOf(0); 
					ArrayList<Float> distances = new ArrayList<Float>();
					List<Integer> doors = new ArrayList<Integer>();
					Integer selectedNode = null; 
					var goalPosition = belief.highLevelGragh.goalPosition;
					/* this is the first step at the beginning*/
					if(currentNode == null) {						
						// if the goal is in the list of entities, it will be selected as the next node
						if(belief.highLevelGragh.getIndexById(goalId) != -1)
						{
							System.out.println(">>>>>>The goal has seen: " + goalId);
							selectedNode = belief.highLevelGragh.getIndexById(goalId);
							belief.highLevelGragh.currentSelectedEntity = selectedNode;
							return new Pair(selectedNode,belief);	 
						}
						
						var agentLocation = belief.worldmodel.position;
						/* because it is the beginning of the game, all vertices are the first set of
						 * vertices that agent has seen. we choose the nearest one. 
						 * */
						for(int i = 0 ; i<belief.highLevelGragh.entities.size(); i++) {
							if(belief.highLevelGragh.entities.get(i).id.contains("door")) {doors.add(i);}
							System.out.println(">>>>>>which entity is selected to compre the distance to the goal: " + belief.highLevelGragh.vertices.get(i) );
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
									System.out.println(">>>>>>goal distance: " + goalId + "previous dist : " + distances.get(1) + "new dist: " + goalDistance);
									if((goalDistance < distances.get(1))  ) {		
									distances.set(0, y); distances.set(1, goalDistance);  selectedNode = i; 
									}	}							
							}
						}					
					} 
					else {
						
						// if the goal is in the list of entities, it will be selected as the next node.
						//but if the current node is the goal and the agent could not find a way to open it,
						//the next time we should give the opportunity to other doors.
						if(belief.highLevelGragh.getIndexById(goalId) != -1 && !goalId.equals(belief.highLevelGragh.entities.get(currentNode).id))
						{
							System.out.println(">>>>>>The goal has seen: " + goalId);
							selectedNode = belief.highLevelGragh.getIndexById(goalId);
							belief.highLevelGragh.currentSelectedEntity = selectedNode;
							return new Pair(selectedNode,belief);	 
						}
						//agent has seen some nodes before, in order to avoid loop, we need to select the
						// new node which is not visited before. 
						// get the neighbors of the current agent position(current node)
						var neighbors = belief.highLevelGragh.neighboursNew(currentNode);
						System.out.println(" select Nearest Node when it is not the first time: " + neighbors + "currrentnode: " + currentNode);
						
						// print some extra information.
						for(Integer element : belief.highLevelGragh.visitedNodes) {
							System.out.println("visited nodes: " + element + " : "+ belief.highLevelGragh.entities.get(element).id);
							
						}
						for(Integer element : neighbors) {
							System.out.println(" neighbor of the current position: " + element +" : "+ belief.highLevelGragh.entities.get(element).id);
							//check if the node is not selected before: unvisitedNode
							if(!belief.highLevelGragh.visitedNodes.contains(element)) {
								// create a list of unvisited doors
								if(belief.highLevelGragh.entities.get(element).id.contains("door")) {doors.add(element);}
								
								var goalDistance  = goalPosition != null ? Vec3.dist(belief.highLevelGragh.vertices.get(element), goalPosition) : null;
								System.out.println(">>>>>>which entity is selected to compre the distance to the goal: " + belief.highLevelGragh.vertices.get(element) );
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
										System.out.println(">>>>>>goal distance: " + goalId + "previous dist : " + distances.get(1) + "new dist: " + goalDistance);
										if(goalDistance < distances.get(1) ) {
											distances.set(0, y); distances.set(1, goalDistance);  selectedNode = element; 
											}
										}
								}
							}	
						}	

					}
					var tempSelectedNode  = selectedNode;
					distances.clear();
					System.out.println("selected an entity in neighborhood: " + selectedNode);
					
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
						
						if(belief.highLevelGragh.getIndexById(goalId) != -1  && !goalId.equals(belief.highLevelGragh.entities.get(currentNode).id)) {
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
							tempSelectedNode = selectedNode;
						}
						System.out.println("Select one unchecked entity" + selectedNode); 
					}
					
					belief.highLevelGragh.entities.forEach(e -> System.out.println("entites id : " + e.id + " statuse: "+belief.isOpen(e.id)));
					
					
					
					
					//if there are some unvisited doors in the neighbors, we give the priority to select between them.
					System.out.println("There is more than one door in nighberhood : " +doors.size() );
					if(!doors.isEmpty() && !belief.highLevelGragh.entities.get(tempSelectedNode).id.contains("door")) {							
						if(doors.size()>1) {
							
							distance = 0;	
							for(Integer element : doors) {
								var goalDistance  = goalPosition != null ? Vec3.dist(belief.highLevelGragh.vertices.get(element), goalPosition) : null;
								System.out.println("doors in neighborhood: " + element + belief.highLevelGragh.entities.get(element).id);
								var y  = Vec3.dist(belief.highLevelGragh.vertices.get(element), belief.highLevelGragh.vertices.get(currentNode));
								System.out.println("distance to current node: " +belief.highLevelGragh.entities.get(currentNode).id +" : "+ y);
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
							System.out.println("There is only one door : " + doors.get(0) );
							selectedNode = doors.get(0);
						}
					}
					System.out.println("selected a door in neighborhood: " + selectedNode );
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
							if(goalDistanceTo1 < goalDistanceTo2 && disBetweenToEntity > belief.getViewDistance()) {
								selectedNode = tempSelectedNode;
							System.out.println("select a button which is near to the goal position rather than a door");
							}
						}
					}
					if(selectedNode == null) return new Pair(null,belief);
					belief.highLevelGragh.currentSelectedEntity = selectedNode;
					System.out.println(">>>>>>>>>>>********selected node: " + belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id + 
					belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).position);
					return new Pair(selectedNode,belief);				
					}).lift();
		
		return nearestNode;
	}
	
	
	/**
	 *  This tactic checks that selected node should be unvisited to avoid being stock in the loop 
	 **/
	
	public static Tactic unvisitedNode() {
			
		return  action("Check the node is not visited").do1(
				(BeliefStateExtended belief)-> {
					var selectedNode = belief.highLevelGragh.currentSelectedEntity;
					if(selectedNode != null) {	
						// the node is selected at the nearest selected tactic 
						System.out.println("unvisitedNode function: " + selectedNode);
							belief.highLevelGragh.visitedNodes.add(selectedNode);
							return new Pair(selectedNode,belief);
					}
											
					return null;
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
                	  var obs = belief.env().interact(belief.id, e.id, LabWorldModel.INTERACT)  ;   
                	//WP change: adding this wait; ... not an ideal solution as it ignores thread interrupt:
                	  try {
                		  // LR has 0.5s timeout before a button can be interacted again, so we need to wait: 
                		  Thread.sleep(650) ;
                	  }
                	  catch(Exception exc) {
                		  // swallowing thread exception...
                	  }
                	  return belief;
                    })
               . on((BeliefStateExtended belief) -> {
            	   
            	    var selectedNode = belief.highLevelGragh.currentSelectedEntity;
            	    if(selectedNode == null ) return null;
            	    var objectID = belief.highLevelGragh.entities.get(selectedNode).id;
                	var e = belief.worldmodel().getElement(objectID) ;
                	//System.out.println(">>>> " + objectID + ": " + e) ;
                	if (e==null) return null ;
                	System.out.println(">>>>>>>>>>>>>********** interact dynamicly " + e.id  + belief.canInteract( e.id)) ;
                	if (belief.canInteract(e.id)) {
                		return e ;
                	}
                	System.out.println(">>> cannot interact with " + e.id) ;
            		System.out.println("    Agent pos: " + belief.worldmodel().getFloorPosition()) ;
            		System.out.println("    Entity pos:" + e.getFloorPosition()) ;
            		System.out.println("    Entity extent:" + e.extent) ;
            		var dist = Vec3.dist(belief.worldmodel().getFloorPosition(), e.getFloorPosition()) ;
            		System.out.println("    Dist: " + dist) ;
            		
                	return null ;
                    })
               . lift();
        return interact;
    }

    
    /**
     *  This method checks current blocked entity status 
     *  */
	public static Tactic checkBlockedEntityStatus(BeliefState b, TestAgent agent) {			
		var checkingState =   action("back to the current blocked entity to check the status").do1(
				(BeliefStateExtended belief)-> {
					var blockedNode = belief.highLevelGragh.currentBlockedEntity;
					System.out.println(">>>>>>>>>>Check Blocked Entity Status " + blockedNode + belief.isOpen(blockedNode));
					return belief;
					}).lift();
		
		
			return  checkingState;	
	}	
    
	/**
	 *  This method will return a node in neighbors which does not have the direct edge with the current node.
	 *  The node should not be tried before. 
	 *  If there are more than one button, the distance to the agent position is considered
	 *  */
	public static Tactic indirectNeighbors() {	
		var checkingState =   action("find a button which is not a direct neighbor").do1(
				(BeliefStateExtended belief)-> {
					float distance   = Float.valueOf(0);
					ArrayList<Float> distances = new ArrayList<Float>();
					Integer selectedNode = null; 
					//var agentLocation = belief.highLevelGragh.currentSelectedEntity;
					// we always call this tactic when there we are looking for a button to open a blocked door
					// so, it should always select an entity based on this blocked entity
					var agentLocation  = belief.highLevelGragh.getIndexById(belief.highLevelGragh.currentBlockedEntity);
					var currentNodeId = belief.highLevelGragh.entities.get(agentLocation).id;
					var neighbor = belief.highLevelGragh.neighboursNew(agentLocation);			
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
								&&!(belief.buttonDoorConnection.get(currentNodeId).contains(entities.get(i).id))
								&& !entities.get(i).id.contains("door")) {		
							var y  = Vec3.dist(belief.highLevelGragh.vertices.get(i), belief.highLevelGragh.vertices.get(agentLocation));
								if(distance == 0) {
									distance = y; 
									selectedNode = i;
									}
								else {						
									if(y<distance) { distance = y; selectedNode = i; }
								}	
							}							
					}
					
					if(selectedNode != null) {
						System.out.println("indirect inactive button " + selectedNode + entities.get(selectedNode).id);
						belief.highLevelGragh.currentSelectedEntity = selectedNode;
						System.out.println("indirect neighbors function: " + selectedNode);
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
	/**
	 *  This tactic is called when agent is looking for a button to open a blocked door
	 *  Select the nearest inactive node to the current agent position.
	 *  If there are more than one button, the distance to the agent position is considered
	 *  */
	public static Tactic selectInactiveButton() {
		
		Tactic nearestNode =  action("find the nearest inactive button").do1(
				(BeliefStateExtended belief)-> {	 
					// So, it should always select an entity based on this blocked entity
					var currentNode  = belief.highLevelGragh.getIndexById(belief.highLevelGragh.currentBlockedEntity);
					float distance   = Float.valueOf(0); 		
					Integer selectedNode = null; 
					// agent has interacted with some nodes before, in order to avoid loop, we need to select the
					// new node which is not interacted before. 
					var agentLocation = currentNode;
					String currentNodeId = belief.highLevelGragh.entities.get(agentLocation).id;
					// get the neighbors of the current agent position(current node)
					var neighbors = belief.highLevelGragh.neighboursNew(agentLocation);
					System.out.println(" select nearest inactive button: the neighbors of the current position " + neighbors + "currrentnode: " + agentLocation);						
					System.out.println("to test the connections" + belief.buttonDoorConnection.get(currentNodeId ).contains("button2"));
					
					for(Integer element : neighbors) {
						var entity  = belief.highLevelGragh.entities.get(element);
						//check if the node is not interacted before and it is not a door nor any other entity
						if(entity.type.equals(LabEntity.SWITCH)						
								&& !(belief.buttonDoorConnection.get(currentNodeId ).contains(entity.id))
								&& !entity.id.contains("door")				
								) {								
							System.out.println(" neighbor of the current position " + element + entity.id +currentNodeId);
							//System.out.println(" is it set" + (belief.buttonDoorConnection.get(currentNodeId).contains(entity.id)));
							var y  = Vec3.dist(belief.highLevelGragh.vertices.get(element), belief.highLevelGragh.vertices.get(agentLocation));							;						
							if(distance == 0) {
								distance = y; 
								selectedNode = element;
								}
							else {
								if(y<distance) { 
									distance = y; selectedNode = element; 	
									}
							}
						}	
					}				
					
					if(selectedNode == null) {System.out.println("there is no inirect button in the neighbourhood to interact" ); return new Pair(null,belief);}
					belief.highLevelGragh.currentSelectedEntity = selectedNode;					
					belief.buttonDoorConnection.get(currentNodeId).add(belief.highLevelGragh.entities.get(selectedNode).id);						
					
					System.out.println("selected button with the position: " + belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id + 
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
    				System.out.println("get door status in guided explore:" + belief.isOpen("door1") );
    				System.out.println("guided explore with parametes: " + memo.stateIs("S0") + memo.stateIs("inTransit")) ;
    				
    				if(!memo.stateIs("inTransit")) {
    					 // in this state we must decide a new exploration target:
    					 System.out.println("if ### Explore: explore" ) ;
                         //get the location of the closest unexplored node
        				 var position = belief.worldmodel().getFloorPosition() ;        				 
        				 //System.out.println(">>> #explored nodes:" + belief.pathfinder.numberOfSeen()) ;
        				 var path = belief.pathfinder().explore(position,destination,BeliefState.DIST_TO_FACE_THRESHOLD,belief.getViewDistance(), (List<Vec3>) memo.memorized) ;   				 
        				 if (path==null || path.isEmpty()) {
        					memo.moveState("exhausted") ;
                            System.out.println("###*** no new and reachable navigation point found; agent is @" + belief.worldmodel.position) ;
                            return null ;
        				 }
        				 List<Vec3> explorationPath = path.stream()
        						            .map(v -> belief.pathfinder().vertices.get(v))
        						            .collect(Collectors.toList()) ;
        				         				 				         				 
        				 var target = explorationPath.get(explorationPath.size() - 1) ;        	
        				 System.out.println("###***** original : " + destination) ;
        				 System.out.println("###***** setting a new exploration target: " + target) ;
                         System.out.println("### abspath to exploration target: " + path) ;
                         System.out.println("### path to exploration target: " + explorationPath) ;
                         //memo.memorized.clear();
                         memo.memorize(target);
                         memo.moveState("inTransit") ; // move the exploration state to inTransit...
                         return new Pair(target, explorationPath);//return the path finding information
    				}else {
                         
    					 System.out.println("***inTransit***" ) ;
    					 Vec3 exploration_target = (Vec3) memo.memorized.get(memo.memorized.size()-1) ;
                         // note that exploration_target won't be null because we are in the state
                         // in-Transit
                         Vec3 agentLocation = belief.worldmodel().getFloorPosition() ;
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
	
    
    
    /** The agent is trapped in some room, while it is moving from entity A to B
     * It Checks the prolog data set to know is there any door connected to a button that
     * can open a path to B. If there are more than one doors or a door is connected to multi 
     * buttons, it selects a door based on some policies.
     *  */
  	public static Tactic unlockAgent(BeliefState b, TestAgent agent, String s) {			
  		var addNewGoal =   action("use prolog to help an agent to untrapped").do1(
  				(BeliefStateExtended belief)-> {
  					var isLocked = belief.prolog.isLockedInCurrentRoom();
  					String entityId = "";
  					System.out.println("what is s:" + s);
  					if(s.contains("button")) {
  						System.out.println("in the buttons: " + belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id);
  						entityId = belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id;
  					}else {
  						System.out.println("in the door: " +  belief.highLevelGragh.currentBlockedEntity);
  						entityId = belief.highLevelGragh.currentBlockedEntity;
  					}
  					
  					
  					System.out.println("is locked in the room: " + isLocked +" id: "+ entityId); 									
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
  					System.out.print("set OF Buttons" + setOFButtons + setOFButtons.size());
  					if(setOFButtons.size()> 1) {
  						//if there are more than one button connected to a door,
  						// select one which is near to the agent position. 
  						for(int j=0; j< setOFButtons.size(); j++) {
  							var button = setOFButtons.get(j);				
  							var dist = Vec3.dist(agentPosition, belief.worldmodel.getElement(button).position);
  						
	  						if(distance == 0) {
	  							distance = dist;
	  							selectedButon = button;
	  						}else {
	  							if(distance > dist) {
	  								distance = dist;
	  								selectedButon = button;
	  							}
	  						}
  						}
  					}else {
  						selectedButon = setOFButtons.get(0);
  					}
  					System.out.println(">>> **** Selected button and door" + selectedButon + selectedDoor);
  					
  					// add a new goal to interact with the selected button, 
  					GoalStructure unblockedDoor = GoalLibExtended.interactWithAButtonAndCheckDoor(selectedButon,selectedDoor);
					agent.addAfter(unblockedDoor);
					
					//register the blocked doors name in the data collector
					agent.registerEvent(new TimeStampedObservationEvent(selectedDoor));
					
					//the inserted goal dos'nt need to be removed after success because we remove the main 
				    //extra goal 	
					
					System.out.println(">>**** A new goal to open the blocked door added " + selectedDoor);
					belief.goalsmap.put("temporaryDoor",unblockedDoor);
					
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
  					var isLocked = belief.prolog.isLockedInCurrentRoom();
  					var currentNode = belief.highLevelGragh.currentSelectedEntity;
  					String buttonId = belief.highLevelGragh.entities.get(currentNode).id;
  					String selectedDoor = null;
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

					LabEntity e = belief.worldmodel().getElement(id) ;
    			    if (e==null) return null ;

					Vec3 nodeLocation = null ;
					if (!memory.memorized.isEmpty()) {
						nodeLocation = (Vec3) memory.memorized.get(0) ;
					}
					Vec3 currentGoalLocation = belief.getGoalLocation() ;

					if (nodeLocation == null
					    || currentGoalLocation == null
					    || Vec3.dist(nodeLocation,currentGoalLocation) >= 0.05
					    || !memory.stateIs(e.id)
							) {
						// in all these cases we need to calculate the node to go

    			        var entity_location = e.getFloorPosition() ;
	    			    List<Pair<Vec3,Float>> candidates = new LinkedList<>() ;
	    			    int k=0 ;
	    			    for (Vec3 v : belief.pathfinder().vertices) {
	    			    	if (belief.pathfinder().seenVertices.get(k) && (Vec3.dist(v, entity_location)) <= belief.getViewDistance()) {
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
		    			        memory.moveState(e.id);
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

	
	/**
	 * Navigate to a location, nearby the given entity, if the location is reachable.
	 * Locations east/west/south/north of the entity of distance 0.7 will be tried.
	 * 
	 * This is the same as original tactic, but the id of the entity is not given.
	 */
	static Tactic navigateToCloseByPosition() {

		MiniMemory memory = new MiniMemory("S0") ;

		Action move =
				unguardedNavigateTo("Navigate to a position nearby ")

				. on((BeliefStateExtended belief) -> {

					var entityId = belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id;
                    
					LabEntity e = belief.worldmodel().getElement(entityId) ;
    			    if (e==null) return null ;

					Vec3 closeByLocation = null ;
					if (!memory.memorized.isEmpty()) {
						// if the position has been calculated before, retrieve it from memory:
						closeByLocation = (Vec3) memory.memorized.get(0) ;
					}
					Vec3 currentGoalLocation = belief.getGoalLocation() ;

					if (closeByLocation == null
					    || currentGoalLocation == null
					    || Vec3.dist(closeByLocation,currentGoalLocation) >= 0.05
					    || ! memory.stateIs(e.id)
					    || belief.getMemorizedPath() == null) {
						// in all these cases we need to calculate the location to go

						//var agent_location = belief.worldmodel.getFloorPosition() ;
	    			    var entity_location = e.getFloorPosition() ;
	    			    // Calculate the center of the square on which the target entity is located.
	    			    // Note: the bottom-left position of the bottom-left corner is (0.5,-,0.5) so this need to be taken into
	    			    // account.
	    			    // First, substract 0.5 from (x,z) ... then round it down. Add 0.5 to get the center position.
	    			    // Then add another 0.5 to compensate the 0.5 that we substracted earlier.
	    			    var entity_sqcenter = new Vec3((float) Math.floor((double) entity_location.x - 0.5f) + 1f,
	    			    		entity_location.y,
	    			    		(float) Math.floor((double) entity_location.z - 0.5f) + 1f) ;
	    			    System.out.println("entity location " + entity_location + " entity_sqcenter " + entity_sqcenter);
	    			    List<Vec3> candidates = new LinkedList<>() ;
	    			    float delta = 0.5f ;
	    			    // adding North and south candidates
	    			    candidates.add(Vec3.add(entity_sqcenter, new Vec3(0,0,delta))) ;
	    			    candidates.add(Vec3.add(entity_sqcenter, new Vec3(0,0,-delta))) ;
	    			    // adding east and west candidates:
	    			    candidates.add(Vec3.add(entity_sqcenter, new Vec3(delta,0,0))) ;
	    			    candidates.add(Vec3.add(entity_sqcenter, new Vec3(-delta,0,0))) ;

	    			    // iterate over the candidates, if one would be reachable:
	    			    for (var c : candidates) {
	    			    	// if c (a candidate point near the entity) is on the navigable,
	    			    	// we should ignore it:
	    			    	if (getCoveringFaces(belief,c) == null) continue ;
	    			    	var result = belief.findPathTo(c, true) ; 
	    			    	System.out.println("c " + c);
	    			    	if (result != null) {
	    			    		// found our target
	    			    		System.out.println(">>> a reachable closeby position found :" + c + ", path: " + result.snd) ;
	    			    		memory.memorized.clear();
	    			    		memory.memorize(c);
	    			    		memory.moveState(e.id);
	    			    		return result ;
	    			    	}
	    			    }
	    			    System.out.println(">>> i tried few nearby locations, but none are reachable :|") ;
	    			    // no reachable node can be found. We will clear the memory, and declare the tactic as disabled
	    			    memory.memorized.clear() ;
	    			    return null ;
					}
					else {
						// else the memorized location and the current goal-location coincide. No need to
						// recalculate the path, so we will just return the pair (memorized-loc,null)
						return new Pair (closeByLocation,null) ;
					}
				}) ;

		return  FIRSTof(
				 forceReplanPath(),
				 tryToUnstuck(),
				 move.lift()
			   ) ;
	}
	
	/**
	 * If the selected node is a blocked entity, a new goal to open this blocked entity
	 * will add as a sub-goal. 
	 * A list of which buttons are tried to open this door is created. 
	 * In order to remove the goal after success, we put the information in goalsmap.
	 * 
	 * */
	/**
	 *  This method checks entity status in order to add a new subgoal if the status is blocked
	 *  The variable currentBlockedEntity is set and a new subgoal to unblock the blocked entity is added. 
	 *  The dynamic subgoal info is recorded in goalsmap.
	 *  The id of the blocked entity is added to the list of buttonDoorConnection to check which buttons are tried to open it. 
	 *  */
	public static Tactic dynamicallyAddGoalToFindButton(TestAgent agent) {
		
		return  action("dynamicly add a goal to find a correspanding button to open the selected door").do1(
				(BeliefStateExtended belief)-> {
					System.out.println("damicly add a new goal to open the blocked door");
					var selectedNode = belief.highLevelGragh.currentSelectedEntity;
					var entity = belief.highLevelGragh.entities.get(selectedNode);
					
					belief.highLevelGragh.currentBlockedEntity = entity.id;
					// the agent won't add the current position which is the blocked node to the visited node
					System.out.println("dynamicly add a goal to find function: " + selectedNode);
					belief.highLevelGragh.visitedNodes.add(belief.highLevelGragh.getIndexById(belief.highLevelGragh.currentBlockedEntity));
					//register the blocked doors name in the data collector
					agent.registerEvent(new TimeStampedObservationEvent(belief.highLevelGragh.currentBlockedEntity));
					
					GoalStructure unblockedDoor = GoalLibExtended.findCorrespondingButton(belief,agent);
					agent.addAfter(unblockedDoor);	
					System.out.println(">>**** A new goal to open the blocked door added " + belief.highLevelGragh.currentBlockedEntity);
					belief.goalsmap.put(entity.id,unblockedDoor);
					
					
					List<String> list = new ArrayList<>();
					if(!belief.buttonDoorConnection.containsKey(entity.id))
					belief.buttonDoorConnection.put(entity.id, list);	
					return new Pair(selectedNode,belief);						
					}).lift();
				
	}

	
	
	/**
	 * Checking the prolog data set to know is there any button connected to current
	 * blocked entity. We also apply a policy to select one button, if there are more. 
	 * the policy is based on the distance, the nearest button to the agent position 
	 *  will select. 
	 *  A dynamic goal will be added if there is a button. the goal is to interact 
	 *  with the button and check the blocked door status.
	 * */
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
					if(!belief.buttonDoorConnection.containsKey(entityId))
					belief.buttonDoorConnection.put(entityId, list);
  					return new Pair(true, belief);
  				}).lift();
	}
	

	/**
	 * This method use the original explore tactic but in the specific direction.
	 * The explore is limited to the area of agent position and the target position. 
	 * The list of the position that agent selects is recorded.
	 * @return
	 */
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
    					System.out.println("get door status in guided explore:" + belief.isOpen("door1"));
    					System.out.println("Guided exlore! " ) ;
                         //get the location of the closest unexplored node
        				 var position = belief.worldmodel().getFloorPosition() ;        				 
        				 //System.out.println(">>> #explored nodes:" + belief.pathfinder.numberOfSeen()) ;
        				 //System.out.println("agent position**********: " + position + "*******target list " + memo.memorized);
        				 		
        				 System.out.println("***********************view distance " + belief.getViewDistance());
        				 // explore method is different parameters to find the target in the shortest time instead of exploring in the wrong direction.
        				 var path = belief.pathfinder().explore(position,destination,BeliefState.DIST_TO_FACE_THRESHOLD, belief.getViewDistance(),(List<Vec3>) memo.memorized) ;   				 
        				 if (path==null || path.isEmpty()) {
        					memo.moveState("exhausted") ;
                            System.out.println("###*** no new and reachable navigation point found; agent is @" + belief.worldmodel.position) ;
                            return null ;
        				 }
        				 List<Vec3> explorationPath = path.stream()
        						            .map(v -> belief.pathfinder().vertices.get(v))
        						            .collect(Collectors.toList()) ;
        				         				 				         				 
        				 var target = explorationPath.get(explorationPath.size() - 1) ;        	
        				 System.out.println("###***** original : " + destination) ;
        				 System.out.println("###***** setting a new exploration target: " + target) ;
                         System.out.println("### abspath to exploration target: " + path) ;
                         System.out.println("### path to exploration target: " + explorationPath) ;
                         //memo.memorized.clear();
                         memo.memorize(target);
                         memo.moveState("inTransit") ; // move the exploration state to inTransit...
                         return new Pair(target, explorationPath);//return the path finding information
    				}
    				else {
    
    					 System.out.println("inTransit" +  (memo.memorized.size()-1) ) ;
    					 Vec3 exploration_target = (Vec3) memo.memorized.get(memo.memorized.size()-1) ;
    					 System.out.println("exploration_target" + exploration_target ) ;
    					 // note that exploration_target won't be null because we are in the state
                         // in-Transit
                         Vec3 agentLocation = belief.worldmodel().getFloorPosition() ;
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
                      //in all other cases, the guard is not enabled:
    				 //return null ;
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
		System.out.println("navigate To Closest Reachable Node without id");
		Action move =
				unguardedNavigateTo("Navigate to a navigation vertex nearby ")

				. on((BeliefStateExtended belief) -> {

					 var entityId = belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id;
			                             
					LabEntity e = belief.worldmodel().getElement(entityId) ;
    			    if (e==null) return null ;

					Vec3 nodeLocation = null ;
					if (!memory.memorized.isEmpty()) {
						nodeLocation = (Vec3) memory.memorized.get(0) ;
					}
					Vec3 currentGoalLocation = belief.getGoalLocation() ;

					if (nodeLocation == null
					    || currentGoalLocation == null
					    || Vec3.dist(nodeLocation,currentGoalLocation) >= 0.05
					    || !memory.stateIs(e.id)
							) {
						// in all these cases we need to calculate the node to go

    			        var entity_location = e.getFloorPosition() ;
	    			    List<Pair<Vec3,Float>> candidates = new LinkedList<>() ;
	    			    int k=0 ;
	    			    for (Vec3 v : belief.pathfinder().vertices) {
	    			    	if (belief.pathfinder().seenVertices.get(k) && (Vec3.dist(v, entity_location)) <= belief.getViewDistance()) {
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
		    			        memory.moveState(e.id);
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
	
	
	   /**
	 * This method is the same as the original explore but I removed the exhausted part.
     * This method will construct a tactic in which the agent will "explore" the world.
     * The tactic will locate the nearest reachable navigation node which the agent
     * has not discovered yet, and drive the agent to go there.
     
     */
    public static Tactic newExplore() {

    	var memo = new MiniMemory("S0") ;
    	// three states:
    	//  S0 ; initial exploration state, a new exploration target must be set
    	//  inTransit: when the agent is traveling to the set exploration target
    	//  exhausted: there is no morsetting a new exploration targete exploration target left
    	//

    	var explore_ =
    			unguardedNavigateTo("Explore: traveling to an exploration target")

    			. on((BeliefState belief) -> {
    				 if(memo.stateIs("S0")) {
    					 // in this state we must decide a new exploration target:

                         //get the location of the closest unexplored node
        				 var position = belief.worldmodel().getFloorPosition() ;
        				 //System.out.println(">>> #explored nodes:" + belief.pathfinder.numberOfSeen()) ;
        				 var path = belief.pathfinder().explore(position,BeliefState.DIST_TO_FACE_THRESHOLD) ;

        				 if (path==null || path.isEmpty()) {
        					memo.moveState("exhausted") ;
                            System.out.println("### no new and reachable navigation point found; agent is @" + belief.worldmodel.position) ;
                            return null ;
        				 }
        				 List<Vec3> explorationPath = path.stream()
        						            .map(v -> belief.pathfinder().vertices.get(v))
        						            .collect(Collectors.toList()) ;

        				 var target = explorationPath.get(explorationPath.size() - 1) ;
        				 System.out.println("### setting a new exploration target: " + target) ;
                         System.out.println("### abspath to exploration target: " + path) ;
                         System.out.println("### path to exploration target: " + explorationPath) ;
                         memo.memorized.clear();
                         memo.memorize(target);
                         memo.moveState("inTransit") ; // move the exploration state to inTransit...
                         return new Pair(target, explorationPath);//return the path finding information
    				 }
    				 else if (memo.stateIs("inTransit")) {
    					 Vec3 exploration_target = (Vec3) memo.memorized.get(0) ;
                         // note that exploration_target won't be null because we are in the state
                         // in-Transit
                         Vec3 agentLocation = belief.worldmodel().getFloorPosition() ;
                         Vec3 currentDestination = belief.getGoalLocation() ;
                         var distToExplorationTarget = Vec3.dist(agentLocation,exploration_target) ;
                         if (distToExplorationTarget <= EXPLORATION_TARGET_DIST_THRESHOLD // current exploration target is reached
                             || currentDestination==null
                             || Vec3.dist(currentDestination,exploration_target) > 0.3) {
                        	 // in all these cases we need to select a new exploration target.
                        	 // This is done by moving back the exploration state to S0.
                        	 memo.moveState("S0");
                         }
                         if (distToExplorationTarget<=EXPLORATION_TARGET_DIST_THRESHOLD) {
                        	 System.out.println("### dist to explroration target " + distToExplorationTarget) ;
                         }
                         // System.out.println(">>> explore in-transit: " + memo.stateIs("inTransit")) ;
                         // System.out.println(">>> exploration target: " + exploration_target) ;
                         // We should not need to re-calculate the path. If we are "inTransit" the path is
                         // already in the agent's memory
                         // return new Tuple(g, belief.findPathTo(g));
                         return new Pair(exploration_target,null);
    				 }
                     // in all other cases, the guard is not enabled:
    				 return null ;
                 })
               . lift();


        return FIRSTof(
        		 forceReplanPath(),
				 tryToUnstuck(),
				 explore_) ;
    }

/****************************************************************************************************/
 //This part is not completed. The functions are related to the dynamicgoal goal structure. They are mainly 
 // the same functions as we already have just small change to make it more dynamic
public static Tactic lookForAbutton2(TestAgent agent) {
		
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
					/*
					 * GoalStructure unblockedDoor =
					 * GoalLibExtended.interactWithAButtonAndCheckDoor(selectedButton,entityId);
					 * agent.addAfter(unblockedDoor); belief.goalsmap.put(entityId,unblockedDoor);
					 */
					
					
					List<String> list = new ArrayList<>();
					if(!belief.buttonDoorConnection.containsKey(entityId))
					belief.buttonDoorConnection.put(entityId, list);
  					return new Pair(true, belief);
  				}).lift();
	}

public static Tactic dynamicallyAddGoalToFindButton2(TestAgent agent) {
	
	return  action("dynamicly add a goal to find a correspanding button to open the selected door").do1(
			(BeliefStateExtended belief)-> {
				System.out.println("damicly add a new goal to open the blocked door");
				var selectedNode = belief.highLevelGragh.currentSelectedEntity;
				var entity = belief.highLevelGragh.entities.get(selectedNode);
				
				belief.highLevelGragh.currentBlockedEntity = entity.id;
				// the agent won't add the current position which is the blocked node to the visited node
				belief.highLevelGragh.visitedNodes.add(belief.highLevelGragh.getIndexById(belief.highLevelGragh.currentBlockedEntity));
				//register the blocked doors name in the data collector
				agent.registerEvent(new TimeStampedObservationEvent(belief.highLevelGragh.currentBlockedEntity));
				
				/*
				 * GoalStructure unblockedDoor =
				 * GoalLibExtended.findCorrespondingButton(belief,agent);
				 * agent.addAfter(unblockedDoor);
				 * System.out.println(">>**** A new goal to open the blocked door added " +
				 * belief.highLevelGragh.currentBlockedEntity);
				 * belief.goalsmap.put(entity.id,unblockedDoor);
				 */
				
				
				List<String> list = new ArrayList<>();
				if(!belief.buttonDoorConnection.containsKey(entity.id))
				belief.buttonDoorConnection.put(entity.id, list);	
				return new Pair(selectedNode,belief);						
				}).lift();
			
}

static Tactic navigateToBlockedNode() {

	MiniMemory memory = new MiniMemory("S0") ;
	System.out.println("navigate To Closest Reachable Node without id");
	Action move =
			unguardedNavigateTo("Navigate to a navigation vertex nearby ")

			. on((BeliefStateExtended belief) -> {

				 var entityId = belief.highLevelGragh.currentBlockedEntity;
		                             
				LabEntity e = belief.worldmodel().getElement(entityId) ;
			    if (e==null) return null ;

				Vec3 nodeLocation = null ;
				if (!memory.memorized.isEmpty()) {
					nodeLocation = (Vec3) memory.memorized.get(0) ;
				}
				Vec3 currentGoalLocation = belief.getGoalLocation() ;

				if (nodeLocation == null
				    || currentGoalLocation == null
				    || Vec3.dist(nodeLocation,currentGoalLocation) >= 0.05
				    || !memory.stateIs(e.id)
						) {
					// in all these cases we need to calculate the node to go

			        var entity_location = e.getFloorPosition() ;
    			    List<Pair<Vec3,Float>> candidates = new LinkedList<>() ;
    			    int k=0 ;
    			    for (Vec3 v : belief.pathfinder().vertices) {
    			    	if (belief.pathfinder().seenVertices.get(k) && (Vec3.dist(v, entity_location)) <= belief.getViewDistance()) {
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
	    			        memory.moveState(e.id);
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


}
