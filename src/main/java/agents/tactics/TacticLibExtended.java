package agents.tactics;

import static nl.uu.cs.aplib.AplibEDSL.FIRSTof;
import static nl.uu.cs.aplib.AplibEDSL.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import alice.tuprolog.InvalidTheoryException;
import eu.iv4xr.framework.extensions.pathfinding.AStar;
import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import eu.iv4xr.framework.mainConcepts.ObservationEvent.TimeStampedObservationEvent;
import eu.iv4xr.framework.spatial.Vec3;
import gameTestingContest.TestingTaskStack;
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
	
	public static Tactic navigateTo(TestingTaskStack tasks) {	
		return FIRSTof(
				 TacticLib.forceReplanPath(),
				 TacticLib.tryToUnstuck(),
				 rawNavigateTo()
			   )  ;
	}
	
	public static Tactic navigateTo(BeliefStateExtended b) {	
		return FIRSTof(
				 TacticLib.forceReplanPath(),
				 TacticLib.tryToUnstuck(),
				 rawNavigateTo(b)
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
	          	  
	            	  if(element == null) return null;
        	  
	            	  System.out.println("Navigate to in belif :::::" + belief.highLevelGragh.currentSelectedEntityId);
	            	  String id ;
//	            	  if(belief.highLevelGragh.currentSelectedEntityId != null) {
//	            		  id = belief.highLevelGragh.currentSelectedEntityId;
//	            		  System.out.println("Navigate to in belif if" + id);
//	            	  }else {
//	            		  id = belief.highLevelGragh.entities.get(element).id;
//	            		  System.out.println("Navigate to in belif else" + id);
//	            	  }
	            		   
	            	  id = belief.highLevelGragh.entities.get(element).id;
            		  System.out.println("Navigate to in rawNavigateTo" + id);
            		  
	            	  var e = (LabEntity) belief.worldmodel.getElement(id) ;
	            	  if(e ==null) return null;
	            		 
	            	  var p = e.getFloorPosition() ;	

	    			    System.out.println("***row navigate to position " + p +" id "+ id +" , path "+ belief.findPathTo(p,false));
	    			    // find path to p, but don't force re-calculation
	    			    return belief.findPathTo(p,false) ;		    			    
	                }) ;
	
	return move.lift() ;
    }
	
	
	public static Tactic rawNavigateTo(BeliefStateExtended b) {
    	Action move = unguardedNavigateTo("Navigate to " )
			      // replacing its guard with this new one:
	              . on((BeliefStateExtended belief) -> {
	            	  //set the element you wan to navigate there while it is in the belief. 
	            	  var element  = belief.highLevelGragh.currentSelectedEntity;
	            	  String  elementId= belief.highLevelGragh.currentSelectedEntityId;
	            	  System.out.println("row navigate to with different belief agent" + element);
	            	  if(element == null) return null;
        	  
	            	  String id;
	            	  LabEntity e;
					  Vec3 p;
					  
					  
					var itemExist = belief.highLevelGragh.getIndexById(elementId);
					
					if(itemExist == -1) {	   
						  System.out.println("Selected item does not exist in the belief");						 
	            		  e = (LabEntity) b.worldmodel.getElement(elementId) ;
		            	  p = e.getFloorPosition() ;
	            	  }else {	            		  
	            		  e = (LabEntity) belief.worldmodel.getElement(elementId) ;
		            	  
		            	  p = e.getFloorPosition() ;
	            	  }
	    			    System.out.print("***row navigate to position " + p +"id "+ elementId +" , path "+ belief.findPathTo(p,false));
	    			    // find path to p, but don't force re-calculation
	    			    return belief.findPathTo(p,false) ;		    			    
	                }) ;
	
	return move.lift() ;
    }
	
	
	public static Tactic rawNavigateTo(TestingTaskStack tasks) {
	    	Action move = unguardedNavigateTo("Navigate to tasks" )
				      // replacing its guard with this new one:
		              . on((BeliefStateExtended belief) -> {
		            	  //set the element you wan to navigate there while it is in the belief. 
		            	  var element  = belief.highLevelGragh.currentSelectedEntity;
		            	  System.out.println(">>>> row navigate to tactic" + element);
		            	  if(element == null) return null;
	        	  
		            	  
		            	  var id = belief.highLevelGragh.entities.get(element).id;
		            	  
		            	  
		            	  var e = (LabEntity) belief.worldmodel.getElement(id) ;
		            	  
		            	  var p = e.getFloorPosition() ;	

		    			    System.out.println("***row navigate to position " + p +"id "+ id +" , path "+ belief.findPathTo(p,false));
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
                			e -> {System.out.println("Seen in the same time stamp update Gragh" + e.id + " e timestam: " +  e.timestamp + " curenttimestamp : " 
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
	
	
	// the only difference between this and the next one is what they return
	public static Tactic updateGraghAndTasks(TestAgent agent, TestingTaskStack goals) {
		
		return action("update high level gragh with new visited neighbors/nodes")
                . do1((BeliefStateExtended belief)-> {
                    //State update   
                	//printing entities in the same time stamp
                	
                	
                	belief.newlyObservedEntities().forEach (	
                			e -> {System.out.println("Seen in the same time stamp update Gragh And Tasks " + e.id + " e timestam: " +  e.timestamp + " curenttimestamp : " 
                    + belief.worldmodel.timestamp + " id :" + e.id); 
                	}); 
                	
                	//get new observation
                	var newObserved = belief.newlyObservedEntities();
                	System.out.println("updateGraghAndTasks!" + newObserved.size() + newObserved.isEmpty());
                	if(newObserved == null || newObserved.isEmpty()) {System.out.println("there is no entity in the new observation!! "); return new Pair(null,belief);}
               
                	//if there are new entity in the recent observation, merge them to the high-level graph
                	var mergeObserve = belief.mergeNewlyObservedEntities(belief.newlyObservedEntities());
                	// Update goals in testing task stack  	
                	for(WorldEntity e :belief.newlyObservedEntities()) {
                		System.out.println("does it update the tasks!");
                		if(e.id.contains("door")  && !belief.isOpen(e.id)&& !goals.itemExcist(e.id) && !goals.itemTested2(e.id)) {
                			System.out.println("does it update the tasks!" + e.id + "updated agent" + belief.worldmodel.agentId);// this is temporary 
                			
	                    	goals.tasksList.add(new TestingTaskStack(e.id, false,e.position, null, belief.worldmodel.agentId));	
                		}
                	}	
                	
                	if( mergeObserve == false) { System.out.println("there is not new entity to Merge!!"); return new Pair(null,belief); } 	
                	
                	return new Pair(true,belief);
                }).lift();
   }
	
	
public static Tactic updateGraghAndTasksSecond(TestAgent agent, TestingTaskStack goals) {
		
		return action("update high level gragh with new visited neighbors/nodes")
                . do1((BeliefStateExtended belief)-> {
                    //State update   
                	//printing entities in the same time stamp
                	belief.newlyObservedEntities().forEach (	
                			e -> {System.out.println("Seen in the same time stamp second" + e.id + " e timestam: " +  e.timestamp + " curenttimestamp : " 
                    + belief.worldmodel.timestamp + " id :" + e.id); 
                	}); 
                	
                	//get new observation
                	var newObserved = belief.newlyObservedEntities();
                	if(newObserved == null) {System.out.println("there is no entity in the new observation!! "); return null;}
               
                	//if there are new entity in the recent observation, merge them to the high-level graph
                	var mergeObserve = belief.mergeNewlyObservedEntities(belief.newlyObservedEntities());          
                	// Update goals in testing task stack  	
                	
                	for(WorldEntity e :belief.newlyObservedEntities()) {
                		System.out.println("befor adding to tasks: " + belief.isOpen(e.id) + "..." );
                		if(e.id.contains("door") && !belief.isOpen(e.id) && !goals.itemExcist(e.id) && !goals.itemTested2(e.id)) {  // this is temporary 	                		
                			System.out.println("add to list of task");
                			goals.tasksList.add(new TestingTaskStack(e.id, false,e.position, null, belief.worldmodel.agentId));	
                		}
                	}	
                	if( mergeObserve == false) { System.out.println("there is not new entity to Merge!!"); return null; } 	
                	return belief;
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
					else {  //it is not the first time.
						
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
	 *  Select the nearest node to the current agent position
	 *  */
	public static Tactic selectNearestNodeByTask(TestingTaskStack testingGoals, int threshold, TestAgent agent, String type, int dist) {
		
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
					var agentLocation = belief.worldmodel.position;
					TestingTaskStack  tempTask = null;
					TestingTaskStack  candidates =  new TestingTaskStack() ;
					//sort the candidates based on the distance to the agent position
					Comparator<TestingTaskStack> distanceComparator = (p1, p2) -> {
						    double distance1 = Vec3.dist( p1.getposition(),agentLocation);
				            double distance2 = Vec3.dist( p2.getposition(),agentLocation);
				            int distanceComparison = Double.compare(distance1, distance2);            
			                return distanceComparison;
				        };
				        
				  // sort the candidates based on the tried number   
			      Comparator<TestingTaskStack> triedNumberComparator = distanceComparator.thenComparing((o1, o2) -> {
			    	  return Integer.compare(o1.gettriedNumber(), o2.gettriedNumber());
			       });
					
					/* this is the first step at the beginning*/
					if(currentNode == null) {						
						// we first check if there is a task in the taskd list which is close enough. if there is we select
						// if not we select the nearest entity seen so far to navigate to. 
	
						// if the goal is in the list of entities, it will be selected as the next node	
						for(TestingTaskStack goals : testingGoals.tasksList) {		
							if(type.equals("higher") && goals.getweight() > threshold &&  !goals.getstatus()) {									
								//create a list of candidates:
								candidates.tasksList.add(goals);	
							}
							if(type.equals("lower") && goals.getweight() < threshold  && !goals.getstatus()) {							
								//create a list of candidates:
								candidates.tasksList.add(goals);							
							}		
						}
						
						if(type.equals("random") ) {
							if(!testingGoals.tasksList.isEmpty()) {
							Random random = new Random();
							while(tempTask == null) {
								int randomIndex = random.nextInt(testingGoals.tasksList.size());
								TestingTaskStack randomSelected = testingGoals.tasksList.get(randomIndex);
								if(!randomSelected.gettestedBy().contains(agent.getId()) && !randomSelected.getstatus()) {
									tempTask = randomSelected;
								}
							}	
						}}
						
					    
						
						if(candidates != null) {
							var sortedCandidates  =  candidates.tasksList.stream().sorted( 
									distanceComparator.thenComparing(triedNumberComparator)
									).collect(Collectors.toList());
							 //this should be the first item which is close and less tried before
							sortedCandidates.forEach(e-> System.out.println(">>>>>>>> sorted candodates: " + e.getitemId() +  " tried number: " + e.gettriedNumber() +" position: "+ e.getposition()  +" distance: "+ Vec3.sub(belief.worldmodel().getFloorPosition(), e.getposition()).lengthSq()));
							if(!sortedCandidates.isEmpty()) tempTask = sortedCandidates.get(0);
						}
									   
			
						if(tempTask != null) {
							selectedNode = belief.highLevelGragh.getIndexById(tempTask.getitemId());
							if(selectedNode == -1) {
								//need to filter based on the distance. 
								System.out.println("It is not seen buy the agent itself: " + Vec3.sub(belief.worldmodel().getFloorPosition(), tempTask.getposition()).length());
								if( Vec3.dist(belief.worldmodel().getFloorPosition(), tempTask.getposition()) <= dist) {				
									// add it to the belief 								
									belief.highLevelGragh.currentSelectedEntityId = tempTask.getitemId();
									selectedNode = 100; // just not to be null
									System.out.println(">>>>>>The goal is selected based on the stack: higher" + tempTask.getitemId());
									// change the status of the selected item in the stack, to avoid picking up by another agent
									tempTask.setStatus(true);	
									tempTask.settestedBy(agent.getId());
								}else {
									selectedNode = null;
								}									
							}else {
								belief.highLevelGragh.currentSelectedEntity = selectedNode;				
								System.out.println(">>>>>>The goal has seen: higher" + tempTask.getitemId() + selectedNode);
								// change the status of the selected item in the stack, to avoid picking up by another agent
								tempTask.setStatus(true);	
								tempTask.settestedBy(agent.getId());
							}	
						}
						
			
						if(selectedNode != null) {

							belief.highLevelGragh.currentSelectedEntity = selectedNode;							
							return new Pair(selectedNode,belief);
						}
						
						
						
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
								if((goalDistance < distances.get(1))  ) {		
								distances.set(0, y); distances.set(1, goalDistance);  selectedNode = i; 
								}}							
							}
						}					
					} 
					else {
						
						// if the goal is in the list of entities, it will be selected as the next node.
						//but if the current node is the goal and the agent could not find a way to open it,
						//the next time we should give the opportunity to other doors.
						
						//we first check the list then the neighbors to navigate
						for(TestingTaskStack goals : testingGoals.tasksList) {
							System.out.println("list of tasks: " + goals.getitemId() + goals.getweight() + threshold + goals.gettestedBy().contains(agent.getId()) + goals.getstatus() + goals.getitemId().equals(belief.highLevelGragh.entities.get(currentNode).id) + type.equals("lower") + type.equals("higher") + type);
							if(type.equals("higher") && goals.getweight() > threshold  && !goals.getstatus() && !goals.getitemId().equals(belief.highLevelGragh.entities.get(currentNode).id)) 	
							{
								candidates.tasksList.add(goals);
							}
							if(type.equals("lower") && goals.getweight() < threshold &&  !goals.getstatus() && !goals.getitemId().equals(belief.highLevelGragh.entities.get(currentNode).id)) 	
							{
								candidates.tasksList.add(goals);
							}							
						}
						
						
						if(candidates != null) {
							var sortedCandidates  =  candidates.tasksList.stream().sorted( 
									distanceComparator.thenComparing(triedNumberComparator)
									).collect(Collectors.toList());
							 //this should be the first item which is close and less tried before
							sortedCandidates.forEach(e-> System.out.println(">>>>>>> sorted candodates: " + e.getitemId() +  " tried number: " + e.gettriedNumber() +" position: "+ e.getposition()  +" distance: "+ Vec3.sub(belief.worldmodel().getFloorPosition(), e.getposition()).lengthSq()));
							if(!sortedCandidates.isEmpty()) tempTask = sortedCandidates.get(0);
						}
						
						
						if(type.equals("random") ) {
							if(!testingGoals.tasksList.isEmpty()) {
							Random random = new Random();
							while(tempTask == null) {
								int randomIndex = random.nextInt(testingGoals.tasksList.size());
								TestingTaskStack randomSelected = testingGoals.tasksList.get(randomIndex);
								if( !randomSelected.getstatus()) { //!randomSelected.gettestedBy().contains(agent.getId()) &&
									tempTask = randomSelected;
								}
							}	
						}}
						System.out.println("print temtTask" + tempTask );
						if(tempTask != null) {
							selectedNode = belief.highLevelGragh.getIndexById(tempTask.getitemId());
							if(selectedNode == -1) {
								//need to filter based on the distance. 
								if( Vec3.dist(belief.worldmodel().getFloorPosition(), tempTask.getposition()) <= dist) {				
									// add it to the belief 								
									belief.highLevelGragh.currentSelectedEntityId = tempTask.getitemId();
									selectedNode = 100; // just not to be null
									System.out.println(">>>>>>The goal is selected based on the stack: higher" + tempTask.getitemId());
									// change the status of the selected item in the stack, to avoid picking up by another agent
									tempTask.setStatus(true);	
									tempTask.settestedBy(agent.getId());
								}else {
									selectedNode = null;
								}									
							}else {
								belief.highLevelGragh.currentSelectedEntity = selectedNode;				
								System.out.println(">>>>>>The goal has seen: higher" + tempTask.getitemId() + selectedNode);
								// change the status of the selected item in the stack, to avoid picking up by another agent
								tempTask.setStatus(true);
								tempTask.settestedBy(agent.getId());
							}	
						}
							
						if(selectedNode != null) {
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
							System.out.println("visited nodes: " + element);
							System.out.println("visited nodes: " + element + " : "+ belief.highLevelGragh.entities.get(element).id);
							
						}						
						for(Integer element : neighbors) {
							System.out.println(" neighbor of the current position: " + element +" : "+ belief.highLevelGragh.entities.get(element).id);
							//check if the node is not selected before: unvisitedNode
//							var task  = testingGoals.itemByID(belief.highLevelGragh.entities.get(element).id);
//							System.out.println(" is it in the neighbors:"+ task );
//							if(task != null && task.getstatus()) {
//								System.out.println(" does it come here"+ task + task.getstatus() + element );
//									break;
//							}
															
							if(!belief.highLevelGragh.visitedNodes.contains(element) ) {
								System.out.println(" waht about here" );
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
											
						for(int j = 0 ; j<belief.highLevelGragh.entities.size(); j++) {
//							var task  = testingGoals.itemByID(belief.highLevelGragh.entities.get(j).id);
//							System.out.println(" is it in the neighbors:"+ task );
//							if(task != null && task.getstatus()) {
//								System.out.println(" does it come here second"+ task + task.getstatus() + j );
//									break;
//							}
							if(!belief.highLevelGragh.visitedNodes.contains(j)) {
								if(belief.highLevelGragh.entities.get(j).id.contains("door")) {doors.add(j);}
								var goalDistance  = goalPosition != null ? Vec3.dist(belief.highLevelGragh.vertices.get(j), goalPosition) : null;
								var y = Vec3.dist(belief.highLevelGragh.vertices.get(j), agentLocation);
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
						
						tempSelectedNode = selectedNode;
						}
						System.out.println("Select one unchecked entity" + selectedNode); 
					}
					
					belief.highLevelGragh.entities.forEach(e -> System.out.println("entites id : " + e.id + " statuse: "+belief.isOpen(e.id)));

					
					if(selectedNode == null) return new Pair(null,belief);
					belief.highLevelGragh.currentSelectedEntity = selectedNode;
					System.out.println(">>>>>>>>>>>********selected node: " + selectedNode);
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
					System.out.println("unvisitedNode function: " + selectedNode);
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
	
	
	
	public static Tactic indirectNeighbors(TestingTaskStack task) {	
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
								&& !(belief.buttonDoorConnection.get(currentNodeId).contains(entities.get(i).id))
								&& !entities.get(i).id.contains("door")
								&& !task.checkLuckedItems(entities.get(i).id)  //it should not be in lucked list
								) {		
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
						task.setluckedItems(belief.highLevelGragh.entities.get(selectedNode).id); System.out.println("set the button in lucked list: " + belief.highLevelGragh.entities.get(selectedNode).id);		
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
					System.out.println("find the nearest inactive button " + currentNode + belief.highLevelGragh.currentBlockedEntity);
					float distance   = Float.valueOf(0); 		
					Integer selectedNode = null; 
					// agent has interacted with some nodes before, in order to avoid loop, we need to select the
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
					if(!belief.highLevelGragh.visitedNodes.contains(selectedNode)) belief.highLevelGragh.visitedNodes.add(selectedNode); 		
					System.out.println("selected button with the position: " + belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id + 
					belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).position);
					return new Pair(selectedNode,belief);				
					}
				
				).lift();
		
		return nearestNode;
	}
	
	public static Tactic selectInactiveButton(TestingTaskStack task) {
			
			Tactic nearestNode =  action("find the nearest inactive button").do1(
					(BeliefStateExtended belief)-> {	 
						// So, it should always select an entity based on this blocked entity
						var currentNode  = belief.highLevelGragh.getIndexById(belief.highLevelGragh.currentBlockedEntity);
						System.out.println("find the nearest inactive button " + currentNode + belief.highLevelGragh.currentBlockedEntity);
						float distance   = Float.valueOf(0); 		
						Integer selectedNode = null; 
						// agent has interacted with some nodes before, in order to avoid loop, we need to select the
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
									&& !(belief.buttonDoorConnection.get(currentNodeId ).contains(entity.id))
									&& !entity.id.contains("door")	
									&& ! task.checkLuckedItems(entity.id)  //it should not be in lucked list
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
						if(!belief.highLevelGragh.visitedNodes.contains(selectedNode)) belief.highLevelGragh.visitedNodes.add(selectedNode); 		
						//Luck the selected button
		            	 task.setluckedItems(belief.highLevelGragh.entities.get(selectedNode).id); System.out.println("set luck items in row navigate to: " + belief.highLevelGragh.entities.get(selectedNode).id);
		            	  
						System.out.println("selected button with the position: " + belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id + 
						belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).position);
						return new Pair(selectedNode,belief);				
						}
					
					).lift();
			
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
  					if(s.contains("button")) {  						
  						entityId = belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id;
  					}else {  						
  						entityId = belief.highLevelGragh.currentBlockedEntity;
  					}
  					
  					// the agent is not locked in the current room
  					// if(! isLocked ) { System.out.println("The agent is not locked in the room"); return null;}
  					
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
    
    
  	
  	
  	public static Tactic unlockAgent2(BeliefState b, TestAgent agent, String s) {			
  		var addNewGoal =   action("use prolog to help an agent to untrapped").do1(
  				(BeliefStateExtended belief)-> {
  					var isLocked = belief.prolog.isLockedInCurrentRoom();
  					String entityId = "";
  					if(s.contains("button")) {  						
  						entityId = belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id;
  					}else {  						
  						entityId = belief.highLevelGragh.currentBlockedEntity;
  					}
  					
  				//	belief.prolog.getConnectedDoor(entityId);
  					// the agent is not locked in the current room
  				//	if(! isLocked ) { System.out.println("The agent is not locked in the room"); return null;}
  					
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
    
  	
  	
  	public static Tactic unlockAgent(BeliefStateExtended b, TestAgent agent, String s, BeliefStateExtended... b2) {			
  		var addNewGoal =   action("use prolog to help an agent to untrapped with two agents" ).do1(
  				(BeliefStateExtended belief)-> {			
						
  					String entityId = belief.highLevelGragh.currentSelectedEntityId;
  					
  					BeliefStateExtended bSlected = null;
  					for(BeliefStateExtended be:   b2) {			
  						if (be.worldmodel.getElement(entityId) != null) {					
  							 bSlected = be;
  						}
  					}
  					var getEnablingButtons = bSlected.prolog.getEnablingButtons2(entityId);
  					System.out.println("get Enabling Doors:size " + getEnablingButtons.size() + entityId);
	
  					
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
					String entityId ;
					if(selectedNode == 100) {
						entityId  = belief.highLevelGragh.currentSelectedEntityId;
					}else {
						var entity = belief.highLevelGragh.entities.get(selectedNode);
						entityId  = entity.id;
					}
					
					
					
					belief.highLevelGragh.currentBlockedEntity = entityId;
					// the agent won't add the current position which is the blocked node to the visited node
					System.out.println("dynamicly add a goal to find function: " + selectedNode);
					belief.highLevelGragh.visitedNodes.add(belief.highLevelGragh.getIndexById(belief.highLevelGragh.currentBlockedEntity));
					//register the blocked doors name in the data collector
					agent.registerEvent(new TimeStampedObservationEvent(belief.highLevelGragh.currentBlockedEntity));
					
					GoalStructure unblockedDoor = GoalLibExtended.findCorrespondingButton(belief,agent);
					agent.addAfter(unblockedDoor);	
					System.out.println(">>**** A new goal to open the blocked door added " + belief.highLevelGragh.currentBlockedEntity);
					belief.goalsmap.put(entityId,unblockedDoor);
					
					
					List<String> list = new ArrayList<>();
					if(!belief.buttonDoorConnection.containsKey(entityId))
					belief.buttonDoorConnection.put(entityId, list);	
					return new Pair(selectedNode,belief);						
					}).lift();
				
	}

	
	public static Tactic dynamicllyAddGoalToUnblock(TestAgent agent, BeliefStateExtended... b) {
			
			return  action("dynamicly add a goal to find a correspanding button to open the selected door").do1(
					(BeliefStateExtended belief)-> {
						System.out.println("damicly add a new goal to open the blocked door");
						var selectedNode = belief.highLevelGragh.currentSelectedEntity;
						String entityId ;
						if(selectedNode == 100) {
							entityId  = belief.highLevelGragh.currentSelectedEntityId;
						}else {
							var entity = belief.highLevelGragh.entities.get(selectedNode);
							entityId  = entity.id;
						}
						
						
						
						belief.highLevelGragh.currentBlockedEntity = entityId;
//						// the agent won't add the current position which is the blocked node to the visited node
//						System.out.println("dynamicly add a goal to open the blocker based on other agents information: " + selectedNode);
//						belief.highLevelGragh.visitedNodes.add(belief.highLevelGragh.getIndexById(belief.highLevelGragh.currentBlockedEntity));
						//register the blocked doors name in the data collector
					//	agent.registerEvent(new TimeStampedObservationEvent(belief.highLevelGragh.currentBlockedEntity));
						
						GoalStructure unblockedDoor = GoalLibExtended.findButtonFromNeighbors(belief,agent,b);
						agent.addAfter(unblockedDoor);	
						System.out.println(">>**** A new goal to open the blocked door added " + belief.highLevelGragh.currentBlockedEntity);
						belief.goalsmap.put(entityId,unblockedDoor);
						
											
						return new Pair(selectedNode,belief);						
						}).lift();
					
		}

	
	
	public static Tactic dynamicallyAddGoalToFindButtonByTask(TestAgent agent, TestingTaskStack tasks) {
		
		return  action("dynamicly add a goal to find a correspanding button to open the selected door: tasks" + "agent ID "+ agent.getId()).do1(
				(BeliefStateExtended belief)-> {
					System.out.println("damicly add a new goal to open the blocked door");
					var selectedNode = belief.highLevelGragh.currentSelectedEntity;
					String entityId ;
					if(selectedNode == 100) {
						entityId  = belief.highLevelGragh.currentSelectedEntityId;
					}else {
						var entity = belief.highLevelGragh.entities.get(selectedNode);
						entityId  = entity.id;
					}

					belief.highLevelGragh.currentBlockedEntity = entityId;
					// the agent won't add the current position which is the blocked node to the visited node
					System.out.println("dynamicly add a goal to find function: " + selectedNode + "entity Id: " + entityId);
					belief.highLevelGragh.visitedNodes.add(belief.highLevelGragh.getIndexById(belief.highLevelGragh.currentBlockedEntity));
					//register the blocked doors name in the data collector
					agent.registerEvent(new TimeStampedObservationEvent(belief.highLevelGragh.currentBlockedEntity));
					
					GoalStructure unblockedDoor = GoalLibExtended.findCorrespondingButtonByTask(belief,agent, tasks);
					agent.addAfter(unblockedDoor);	
					System.out.println(">>**** A new goal to open the blocked door added " + belief.highLevelGragh.currentBlockedEntity);
					belief.goalsmap.put(entityId,unblockedDoor);
					
					
					List<String> list = new ArrayList<>();
					if(!belief.buttonDoorConnection.containsKey(entityId))
					belief.buttonDoorConnection.put(entityId, list);	
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
  					String entityId;
  					if(currentNode == 100) {
  						entityId = belief.highLevelGragh.currentSelectedEntityId;
  					}else {
  						entityId= belief.highLevelGragh.entities.get(currentNode).id;
  					}
  					
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
	 * Checking the prolog data set to know is there any button connected to current
	 * blocked entity. We also apply a policy to select one button, if there are more. 
	 * the policy is based on the distance, the nearest button to the agent position 
	 *  will select. 
	 *  A dynamic goal will be added if there is a button. the goal is to interact 
	 *  with the button and check the blocked door status.
	 * */
	public static Tactic lookForAbutton(TestAgent agent, TestingTaskStack tasks) {
		
  		return action("checkig the prolog data set to find the corresponding button by task").do1(
  				(BeliefStateExtended belief)-> {
  					var currentNode = belief.highLevelGragh.currentSelectedEntity;
  					String entityId;
  					if(currentNode == 100) {
  						entityId = belief.highLevelGragh.currentSelectedEntityId;
  					}else {
  						entityId= belief.highLevelGragh.entities.get(currentNode).id;
  					}
  					
  					belief.highLevelGragh.currentBlockedEntity = entityId;
  					String selectedButton = null;
  					System.out.println("current blocked selected node: " + entityId);
  					var getEnablingButtons = belief.prolog.getConnectedButton(entityId);
  					if(getEnablingButtons == null || getEnablingButtons.isEmpty()) return new Pair(false, belief);
  					float distance   = Float.valueOf(0);
  					//this should return the agent current position
  					Vec3 agentPosition = belief.worldmodel.position;
  					
  					if(getEnablingButtons.size() == 1) {
  						if(!tasks.checkLuckedItems(getEnablingButtons.get(0)))  //check if it is not in the lucked items
  						selectedButton = getEnablingButtons.get(0);	
  					
  					}else {
	  					for(int i=0; i< getEnablingButtons.size(); i++) {	
	  						var button = getEnablingButtons.get(i);	  				
	  						//map.get(map.keySet().toArray()[0]) to get the value of the first key
	  					//check if the selected button is not in the lucked items:
	  	  					
	  	  					if(tasks.checkLuckedItems(button)) break;  //check if it is not in the lucked items
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
  					
  					if(selectedButton == null) return new Pair(false, belief);
  					
  					//LUCK the selected button
  					tasks.setluckedItems(selectedButton); System.out.println("set lucked item in look for a button prolog" + selectedButton);
  					
  					// add a new goal to interact with the selected button,   					
  					GoalStructure unblockedDoor = GoalLibExtended.interactWithAButtonAndCheckDoor(selectedButton,entityId);
					agent.addAfter(unblockedDoor);
					belief.goalsmap.put(entityId,unblockedDoor);
					
					belief.highLevelGragh.currentSelectedEntityId = entityId;
					
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
    
    
    public static Tactic guidedExplore(BeliefStateExtended b) {

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
    				
    				Vec3 destination ;
    				String elementId  = belief.highLevelGragh.currentSelectedEntityId;
    				var element = belief.highLevelGragh.currentSelectedEntity;
    				
    				var itemExist = belief.highLevelGragh.getIndexById(elementId);
					
					LabEntity e;
					String id;
					if(itemExist == -1) {	   
						  System.out.println("Selected item does not exist in the belief guided explore" + elementId);
						  id = b.highLevelGragh.entities.get(element).id;
	            		  e = (LabEntity) b.worldmodel.getElement(id) ;
	            		  destination = e.getFloorPosition() ;
	            	  }else {
	            		  id = belief.highLevelGragh.entities.get(element).id;
	            		  e = (LabEntity) belief.worldmodel.getElement(id) ;
		            	  
	            		  destination = e.getFloorPosition() ;
	            	  }
    				
    				
    				if(!memo.stateIs("inTransit")) {
    					 // in this state we must decide a new exploration target:    					
    					System.out.println("Guided exlore! " ) ;
                         //get the location of the closest unexplored node
        				 var position = belief.worldmodel().getFloorPosition() ;        				 
        				 //System.out.println(">>> #explored nodes:" + belief.pathfinder.numberOfSeen()) ;
        				 //System.out.println("agent position**********: " + position + "*******target list " + memo.memorized);
        				 		
        				 System.out.println("***********************view distance " + belief.getViewDistance());
        				 // explore method is different parameters to find the target in the shortest time instead of exploring in the wrong direction.
        				 List<Integer> path;
        				 path = belief.pathfinder().explore(position,destination,BeliefState.DIST_TO_FACE_THRESHOLD, belief.getViewDistance(),(List<Vec3>) memo.memorized) ;   				 
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
	 * This method use the original explore tactic but in the specific direction.
	 * The explore is limited to the area of agent position and the target position. 
	 * The list of the position that agent selects is recorded.
	 * @return
	 */
    public static Tactic guidedExplore(TestingTaskStack tasks) {

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
    				
    				Vec3 destination;
    				if(belief.highLevelGragh.currentSelectedEntity == 100) {
    					TestingTaskStack task = tasks.tasksList.stream().filter(e -> e.getitemId().equals(belief.highLevelGragh.currentSelectedEntityId)).findAny().get();
    					destination = task.getposition();
    				}else {
    				
    					destination = belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).position;
    				} 
    				if(!memo.stateIs("inTransit")) {
    					 // in this state we must decide a new exploration target:
    					System.out.println("get door status in guided explore:");
    					System.out.println("Guided exlore! with task" ) ;
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
     * This method will construct a tactic in which the agent will "explore" the world.
     * The tactic will locate the nearest reachable navigation node which the agent
     * has not discovered yet, and drive the agent to go there.
     */
    public static Tactic explore(TestingTaskStack tasks) {

    	var memo = new MiniMemory("S0") ;
    	// three states:
    	//  S0 ; initial exploration state, a new exploration target must be set
    	//  inTransit: when the agent is traveling to the set exploration target
    	//  exhausted: there is no morsetting a new exploration targete exploration target left
    	//
    	System.out.println("explore tactic with tasks");
    	var explore_ =
    			unguardedNavigateTo("Explore: traveling to an exploration target")

    			. on((BeliefStateExtended belief) -> {
    				 if(memo.stateIs("S0")) {
    					 // in this state we must decide a new exploration target:
    					 System.out.println("explore by tasks:: ");
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
    					 
    					 Vec3 destination;
    	    				if(belief.highLevelGragh.currentSelectedEntity == 100) {
    	    					TestingTaskStack task = tasks.tasksList.stream().filter(e -> e.getitemId().equals(belief.highLevelGragh.currentSelectedEntityId)).findAny().get();
    	    					destination = task.getposition();
    	    				}else {
    	    				
    	    					destination = belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).position;
    	    				} 
    	    				
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

					String entityId ;
					if(belief.highLevelGragh.currentSelectedEntity == 100) {
						entityId = belief.highLevelGragh.currentSelectedEntityId;
					} else {
						entityId = belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id;
					}                  
					LabEntity e = belief.worldmodel().getElement(entityId) ;
					 System.out.println("e is null" + e);
    			    if (e==null ) return null ;
    			   
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
						long startTime = System.currentTimeMillis();
    			        var entity_location = e.getFloorPosition() ;
    			      //  System.out.println("pathToEntity first: " + belief.findPathTo(e.getFloorPosition(),true));
    			        // fakely unblocking door to make it reachable via pathfinder

//    			        var object  = belief.pathfinder().obstacles.stream().filter(o -> o.obstacle.equals(e)).findAny().get();
//    			        System.out.println("object: " + object.isBlocking + "id: "+((LabEntity) object.obstacle).id);
    			        
    			        
    			    //    belief.prolog.fakelyUnblockDoor(entityId); 
    			       
//    			        if (belief.prolog.belief  == belief ) {
//    			        	System.out.println(" is th beilief the same:: ");
//    			        }
    			    //    System.out.println(" is it open:: " + belief.isOpen(entityId));
    			        
    			        // check the path now:
    			     //   var pathToEntity =  belief.findPathTo(e.getFloorPosition(),true) ;		        
    			        
//    			        boolean originalBlockingState = belief.pathfinder().getBlockingStatus(e) ;
//    			    	belief.pathfinder().setBlockingState(e,true) ; // should not be false?S
//    			    	var p = getDoorCenterPosition(e) ;
//    			    	//var p = e.getFloorPosition() ;
//    			    	// System.out.println(">>> door " + id + " center:" + p) ;
//    			    	Pair<Vec3,List<Vec3>> path = null ;
//    			    	path = calculatePathToDoor(belief,e,0.9f) ;
//    			    	System.out.println("path and status: " + path + originalBlockingState   );
//    			    	belief.pathfinder().setBlockingState(e,originalBlockingState) ;	 
    			        
    			        
    			        
    			        
    			      //  System.out.println("pathToEntity after changing: " + pathToEntity + belief.expensiveFindPathTo(e.position, true));
    			        
    			       // belief.prolog.restoreObstacleState(entityId, true);
    			        
    			      //  System.out.println("pathToEntity???: " + pathToEntity);
//    			        if(!belief.prolog.doorIsReachable(entityId) ) {  
//    			        	System.out.println("pathToEntity: " + pathToEntity + belief.prolog.doorIsReachable(entityId)); 
//    			        	long endTime = System.currentTimeMillis();
//			    			long totalTime = 0;
//			    			totalTime = endTime - startTime;
//			    			 System.out.println( "delay time when is null " + totalTime);
//    			        	return null;
//    			        	}
    			       
    			        System.out.println("entity location: " + entity_location);
	    			   
    			        
    			        
    			        List<Pair<Vec3,Float>> candidates = new LinkedList<>() ;
	    			    int k=0 ;
	    			    for (Vec3 v : belief.pathfinder().vertices) {
	    			    	if (belief.pathfinder().seenVertices.get(k) && (Vec3.dist(v, entity_location)) <= belief.getViewDistance()) {
	    			    		// v has been seen:
	    			    		candidates.add(new Pair(v, Vec3.dist(entity_location, v))) ;
	    			    	}
	    			    	k++ ;
	    			    }
	    			    System.out.println("entity candidates: " + candidates.isEmpty());
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
		    			        long endTime = System.currentTimeMillis();
				    			long totalTime = 0;
				    			totalTime = endTime - startTime;
				    			 System.out.println( "delay time when it is reachable" + totalTime);
		    			    	return result ;
		    			    }
		    		    }
		    			System.out.println(">>> no reachable nearby nodes :|") ;
		    			// no reachable node can be found. We will clear the memory, and declare the tactic as disabled
		    			memory.memorized.clear() ;
		    			long endTime = System.currentTimeMillis();
		    			long totalTime = 0;
		    			totalTime = endTime - startTime;
		    			 System.out.println( "delay time" + totalTime);
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
	
	// the same as the original one but the id of the door will be given by the belief
	public static Tactic optimisticNavigateToEntity() {
		
		MiniMemory memory = new MiniMemory("S0") ;
		
		Action move = unguardedNavigateTo("Navigate to " )
			      // replacing its guard with this new one:
	              . on((BeliefStateExtended belief) -> {
		            	 String entityId ;
		          		if(belief.highLevelGragh.currentSelectedEntity == 100) {
		          			entityId = belief.highLevelGragh.currentSelectedEntityId;
		          		} else {
		          			entityId = belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id;
		          		}                            		
	          		
	                	var e = (LabEntity) belief.worldmodel.getElement(entityId) ;
	    			    if (e==null) return null ;
	    			    
	    			    Vec3 thisTacticMemorizedGoalLocation = null ;
						if (!memory.memorized.isEmpty()) {
							thisTacticMemorizedGoalLocation = (Vec3) memory.memorized.get(0) ;
						}
						Vec3 currentGoalLocation = belief.getGoalLocation() ;
						
						if (thisTacticMemorizedGoalLocation == null
							    || currentGoalLocation == null
							    || Vec3.dist(thisTacticMemorizedGoalLocation,currentGoalLocation) >= 0.05) {
							
							Pair<Vec3,List<Vec3>> path = null ;
		    			    if (e.type.equals(LabEntity.DOOR)) {
		    			    	boolean originalBlockingState = belief.pathfinder().getBlockingStatus(e) ;
		    			    	belief.pathfinder().setBlockingState(e,true) ;
		    			    	var p = getDoorCenterPosition(e) ;
		    			    	//var p = e.getFloorPosition() ;
		    			    	// System.out.println(">>> door " + id + " center:" + p) ;
		    			    	path = calculatePathToDoor(belief,e,0.9f) ;
		    			    	belief.pathfinder().setBlockingState(e,originalBlockingState) ;	 
		    			    }
		    			    else {
		    			    	path = belief.findPathTo(e.getFloorPosition(),true) ; 
		    			    }
		    			    //System.out.println(">>> path:" + path) ;
		    			    memory.memorized.clear();
	    			        if (path != null) memory.memorize(path.fst);
		    			    return path ;
						}
						else {
							return new Pair (thisTacticMemorizedGoalLocation,null) ;
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
	


public static Tactic selectButtonFromNeighbors(TestAgent agent, BeliefStateExtended... b2) {
	
	Tactic existingbutton =  action("find the a button").do1(
			(BeliefStateExtended belief)-> {	 
				// So, it should always select an entity based on this blocked entity
				var currentNode  = belief.highLevelGragh.getIndexById(belief.highLevelGragh.currentBlockedEntity);
						
				Integer selectedNode = null; 
				// agent has interacted with some nodes before, in order to avoid loop, we need to select the
				// new node which is not interacted before. 
				var agentLocation = belief.worldmodel.position;
				String currentNodeId = belief.highLevelGragh.entities.get(currentNode).id;
				// get the neighbors of the current agent position(current node)
				
				List<String> allButtons = new ArrayList();
				// Collect all buttons in the high-level graph.
				belief.highLevelGragh.entities.forEach(button -> {
					if (button.id.contains("button")) {							
							allButtons.add(button.id);
						}
				});
				
				List<String> buttons = null;
				BeliefStateExtended bSlected = null ;
				
				//List<String> buttons = b2.domiNode(allButtons);		
				for(BeliefStateExtended b:   b2) {			
					if (b.checkEnablers(allButtons)) {
						buttons =  b.domiNode(allButtons);
						  bSlected = b;
					}
				}
				
				List<WorldEntity> entityOfButtons = new ArrayList<>();
				
				for(String button : buttons) {
					System.out.println(">>>>>buttons that have not seen by the agent but are in the other agents belief" +button );
					entityOfButtons.add(bSlected.highLevelGragh.entities.get(bSlected.highLevelGragh.getIndexById(button)));
				}
				//sort based on the location to the agent,
				
				Comparator<WorldEntity> distanceComparator = (p1, p2) -> {
				    double distance1 = Vec3.dist( p1.position,agentLocation);
		            double distance2 = Vec3.dist( p2.position,agentLocation);
		            int distanceComparison = Double.compare(distance1, distance2);            
	                return distanceComparison;
		        };		
		        
		        var sortedCandidates  =  entityOfButtons.stream().sorted(distanceComparator).collect(Collectors.toList());
		        System.out.println("first selected entity: " + sortedCandidates.get(0).id);
				belief.highLevelGragh.currentSelectedEntity = bSlected.highLevelGragh.getIndexById(sortedCandidates.get(0).id);
				belief.highLevelGragh.currentSelectedEntityId = sortedCandidates.get(0).id;
				belief.buttonDoorConnection.get(currentNodeId).add(sortedCandidates.get(0).id);						
				selectedNode = 1000;
				return new Pair(selectedNode,belief);				
				}
			
			).lift();
	
	return existingbutton;
}


}
