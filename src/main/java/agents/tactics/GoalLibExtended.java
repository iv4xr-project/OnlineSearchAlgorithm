package agents.tactics;

import static nl.uu.cs.aplib.AplibEDSL.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.codec.language.bm.BeiderMorseEncoder;

import com.sun.net.httpserver.Authenticator.Success;

import alice.tuprolog.InvalidTheoryException;
import static eu.iv4xr.framework.mainConcepts.ObservationEvent.*;
import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import eu.iv4xr.framework.spatial.Vec3;
import gameTestingContest.TestingTaskStack;
import nl.uu.cs.aplib.mainConcepts.Goal;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.mainConcepts.GoalStructure.GoalsCombinator;
import nl.uu.cs.aplib.mainConcepts.Tactic;
import nl.uu.cs.aplib.utils.Pair;
import world.BeliefState;
import world.BeliefStateExtended;
import world.HighLevelGraph;
import world.LabEntity;

public class GoalLibExtended extends GoalLib {

	/**
	 * This method will observe the neighbors objects(nodes) and updated the
	 * high-level graph
	 */
	public static <State> GoalStructure newObservedNodes(TestAgent agent) {

		Goal goal = goal("Update the neighbrs graph").toSolve((Pair<String, BeliefState> s) -> {
			if (s.fst == null) {
				System.out.println("There is no new entity/neighbore");
				return false;
			}
			return true;
		}).withTactic(SEQ(TacticLibExtended.updateGragh(agent), ABORT()));
		return goal.lift();
	}
	public static <State> GoalStructure newObservedNodes(TestAgent agent, TestingTaskStack goals) {

		Goal goal = goal("Update the neighbrs graph").toSolve((Pair<String, BeliefState> s) -> {
			if (s.fst == null) {
				System.out.println("There is no new entity/neighbore");
				return false;
			}
			return true;
		}).withTactic(SEQ(TacticLibExtended.updateGraghAndTasks(agent, goals), ABORT()));
		return goal.lift();
	}

	/**
	 * Explore the environment when there is no entity in the agent visibility
	 * range, until it sees a new entity
	 */
	public static <State> GoalStructure exploreTill(TestAgent agent, BeliefStateExtended beliefState) {
		var g = goal("explore").toSolve((BeliefState s) -> {
			System.out.println("Explore till the new entity be observed !! ");
			return false;
		}).withTactic(FIRSTof(TacticLibExtended.newExplore(), ABORT())).lift();
		// increasing the budget to make the agent movement smoother
		g.maxbudget(8);
		return FIRSTof(SEQ(lift((BeliefStateExtended b) -> GoalLibExtended.clearPath(beliefState)), g), SUCCESS());
	}

	/**
	 * Clear the memorized path to explore the new area
	 */
	public static Boolean clearPath(BeliefStateExtended belief) {

		if (belief.getMemorizedPath() != null && !belief.getMemorizedPath().isEmpty()) {
			belief.clearGoalLocation();
		}
		return true;
	}

	/*
	 * This method checks if there is still unvisited node to discover
	 * if there is already something visited by another agent which is close enough, it stops exploring
	 */
	public static Boolean checkExplore(BeliefStateExtended belief, TestingTaskStack toDo, int totalTask, String type, int threshold, int distance) {

		var explore = belief.pathfinder().explore(belief.worldmodel().getFloorPosition(),
				BeliefStateExtended.DIST_TO_FACE_THRESHOLD);
		
		
		System.out.println("Check if there is unvisited part to explore: " + explore);
		System.out.println(belief.worldmodel().getFloorPosition());
		//if for any reason(navigation problem), it can not navidate to the selected entity, 
		if( belief.highLevelGragh.currentSelectedEntity != null  && belief.highLevelGragh.currentSelectedEntity == 100) {
			belief.highLevelGragh.currentSelectedEntity = null;
			
		}
		
		int numberofTasks = toDo.getdoneTasks2().size();
		if( numberofTasks == totalTask) {
			System.out.println("all tasks are seen in check explore");
			return false;
		}
		//if there is already something visited by another agent which is close enough, about exploration
		boolean checkTask  = false;
		for (TestingTaskStack tasks : toDo.tasksList) {														
			if(type.contains("higher") && (tasks.getweight() > threshold)) {
				var dist  = Vec3.dist(belief.worldmodel().getFloorPosition(), tasks.getposition());			
				System.out.println("there is a task in the list to choice but lets check the distance: " + dist );
				if (!belief.isOpen(tasks.getitemId()) && !tasks.getstatus() &&  dist<= distance) 	{	
					System.out.println("----------------------------------------------------------------------------");		
					return false;	
					
				}
			}
			if(type.contains("lower") && (tasks.getweight() < threshold)) {
				var dist  = Vec3.dist(belief.worldmodel().getFloorPosition(), tasks.getposition());
				System.out.println("there is a task in the list to choice but lets check the distance: " + dist);
				if (!belief.isOpen(tasks.getitemId())&& !tasks.getstatus() && dist <= distance) 	{		
					System.out.println("----------------------------------------------------------------------------");
					return false;		
	
				}	
			}
			if(type.contains("random")) {
				var dist  = Vec3.dist(belief.worldmodel().getFloorPosition(), tasks.getposition());
				System.out.println("there is a task in the list to choice but lets check the distance: " + dist);
				if (!belief.isOpen(tasks.getitemId())&& !tasks.getstatus() && dist <= distance) 
					return false;
			}
		}		
		
		
		if (explore != null) {	
			System.out.println("There is unvisited nodes to explore!!! ");
			return true;
		}
		return false;
	}
	
	
	public static GoalStructure checkExplore2(BeliefStateExtended belief, TestingTaskStack toDo, int totalTask) {
		return  goal("check Explore 2").toSolve((Boolean b) -> {
			System.out.println("b in check explore " + b);
			if(b) {
				
				return true;
			}	
			return false;
		}).withTactic(
				SEQ(
					action("checkExplore2").do1((BeliefStateExtended b) -> {
					int numberofTasks = toDo.getdoneTasks2().size();
					if( numberofTasks == totalTask) {
						System.out.println("all tasks are seen in check explore2");
						return false;
					}
					return true;
				}).lift()
				,ABORT()
				)
				).lift();
		
		
	}
	

	/* This methods checks if there still some unexplored area or testing task is not empty
	 * 
	 */
	
	public static Boolean checkExploreAndTasks(BeliefStateExtended belief, TestingTaskStack toDo, TestAgent agent, String type, int treshold, int totalTask) {

		var explore = belief.pathfinder().explore(belief.worldmodel().getFloorPosition(),
				BeliefStateExtended.DIST_TO_FACE_THRESHOLD);
		boolean checkTask  = false;
		for (TestingTaskStack tasks : toDo.tasksList) {														
			if(type.contains("higher") && (tasks.getweight() > treshold)) {
				if (!belief.isOpen(tasks.getitemId()) ) 							
					checkTask = true;	
			}
			if(type.contains("lower") && (tasks.getweight() < treshold)) {
				if (!belief.isOpen(tasks.getitemId()) ) 					
					checkTask = true;			
			}
			if(type.contains("random")) {
				if (!belief.isOpen(tasks.getitemId()) ) 
					checkTask = true;
			}
		}					
			
		
		// removing the previous task from the list od to do task, if the agent could not navigate to the selected entity
		// this happens when previous task to do was not seen before. 
		// @ToDo maybe, it can apply to all failed type. 
		if( belief.highLevelGragh.currentSelectedEntity != null  && belief.highLevelGragh.currentSelectedEntity == 100) {
			belief.highLevelGragh.currentSelectedEntity = null;
			//remove the visited node as well
			if(belief.highLevelGragh.visitedNodes.contains(100))  {  
				System.out.println("remove the item from the visited node as well");
				belief.highLevelGragh.visitedNodes.removeIf(e -> e.equals(100));
				}
			// increase TriedNumber
			System.out.println("selected item to increase the tried number " +belief.highLevelGragh.currentSelectedEntityId + agent.getId());
			var selectedTask = toDo.tasksList.stream().filter(e-> e.getitemId().equals(belief.highLevelGragh.currentSelectedEntityId)).findAny().get();
			selectedTask.setStatus(false);
			selectedTask.settriedItem();
		}
		
		System.out.println("Check if there is unvisited part to explore: by task " + explore + checkTask);
		System.out.println(belief.worldmodel().getFloorPosition());
		int numberofTasks = toDo.getdoneTasks2().size();
		if( numberofTasks == totalTask) {
			System.out.println("all tasks are seen");
			return false;
		}
		if (explore != null || checkTask) {
			System.out.println("There is unvisited nodes to explore!!! OR some tasks in the list");
			return true;
		}

		return false;
	}
	
	
	
	
	/**
	 * This method will invoke when a dynamic goal to open the blocked door is added
	 * to the goal structure. To decide when to abort this intermediate goal, we
	 * need to check two things. firstly, check if there is still unvisited node to
	 * discover secondly, if there is not, the agent should try all exist buttons
	 * for this blocked door to be sure none can open it.
	 */
	public static Boolean checkExploreAndButtons(BeliefStateExtended belief) {	
		var explore = belief.pathfinder().explore(belief.worldmodel().getFloorPosition(),
				BeliefStateExtended.DIST_TO_FACE_THRESHOLD);
		List<String> allButtons = new LinkedList<>();
		String currentBlockedNode = belief.highLevelGragh.currentBlockedEntity;
		
		System.out.println("Exploration is  done!" + explore + " agent id " + belief.worldmodel.agentId);
		belief.highLevelGragh.entities.forEach(door -> {
			if (door.id.contains("door")) {	
				System.out.println("what are the doors: all door's status:  " + door.id + belief.isOpen(door.id));						
				}
		});
		
		belief.highLevelGragh.entities.forEach(button -> {
			if (button.id.contains("button")) {	
				System.out.println("what are the buttons: all buttons visited by agent: " + button.id);				
				}
		});
		
	
		if (explore != null) {	
			System.out.println("Exploration is not done!");
			return true;
		} else {
			
			// Collect all buttons in the high-level graph.
			belief.highLevelGragh.entities.forEach(button -> {
				if (button.id.contains("button")) {	
					System.out.println("all buttons visited by agent: " + button.id);
						allButtons.add(button.id);
					}
			});
			
			
						
			// check the list of visited button for this blocked entity. if there is still a
			// button which is not in the
			// visited list, return true
			for (int i = 0; i < allButtons.size(); i++) {
				if (!belief.buttonDoorConnection.get(currentBlockedNode).contains(allButtons.get(i)))	{
					System.out.println("Not tried button: " + allButtons.get(i));
					return true;
				}			
					
			}
		
			
		}	
		
		System.out.println(">>>>to the end");
		return false;
	}

	
	
	public static Boolean checkExploreAndButtons(BeliefStateExtended belief, TestingTaskStack tasks) {	
		var explore = belief.pathfinder().explore(belief.worldmodel().getFloorPosition(),
				BeliefStateExtended.DIST_TO_FACE_THRESHOLD);
		List<String> allButtons = new LinkedList<>();
		String currentBlockedNode = belief.highLevelGragh.currentBlockedEntity;
		
		System.out.println("Exploration is  done!" + explore + " agent id " + belief.worldmodel.agentId);
		belief.highLevelGragh.entities.forEach(door -> {
			if (door.id.contains("door")) {	
				System.out.println("what are the doors: all door's status:  " + door.id + belief.isOpen(door.id));						
				}
		});
		
		belief.highLevelGragh.entities.forEach(button -> {
			if (button.id.contains("button")) {	
				System.out.println("what are the buttons: all buttons visited by agent: " + button.id);				
				}
		});
		
		
		String removedGoal  = belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id;
		System.out.println("Remove lucked items when it can not for any reason navigate to it: " + removedGoal );
		tasks.removeLuckedItems( removedGoal);
		
		
		
		if (explore != null) {	
			System.out.println("Exploration is not done!");
			return true;
		} else {
			
			// Collect all buttons in the high-level graph.
			belief.highLevelGragh.entities.forEach(button -> {
				if (button.id.contains("button")) {	
					System.out.println("all buttons visited by agent: " + button.id);
						allButtons.add(button.id);
					}
			});
			
			
						
			// check the list of visited button for this blocked entity. if there is still a
			// button which is not in the
			// visited list, return true
			for (int i = 0; i < allButtons.size(); i++) {
				if (!belief.buttonDoorConnection.get(currentBlockedNode).contains(allButtons.get(i)))	{
					System.out.println("Not tried button: " + allButtons.get(i));
					return true;
				}			
					
			}
		
			
		}	
		
		System.out.println(">>>>to the end");
		return false;
	}


	
	/**
	 * This method will invoke when there is no new node/entity in the current agent
	 * position Explore the game world until the agent sees new objects
	 */
	public static GoalStructure findNodes(TestAgent agent, BeliefStateExtended belief) {
		System.out.println(">>>>>>Explore the game world to find new neighbors");
		return SEQ(SEQ(
				// To record the exploration time, the time will be recorded
				lift((b) -> GoalLibExtended.startExploreRecorder(agent)), GoalLibExtended.exploreTill(agent, belief),
				lift((b) -> GoalLibExtended.endExploreRecorder(agent))), GoalLibExtended.newObservedNodes(agent));

	}

	/**
	 * This method will invoke when there is no new node/entity in the current agent
	 * position Explore the game world until the agent sees new objects
	 */
	public static GoalStructure findNodes(TestAgent agent, BeliefStateExtended belief, TestingTaskStack Goals) {
		System.out.println(">>>>>>Explore the game world to find new neighbors with tasks");
		return SEQ(
//				SEQ(
//					// To record the exploration time, the time will be recorded
//					lift((b) -> GoalLibExtended.startExploreRecorder(agent)), 
//					GoalLibExtended.exploreTill(agent, belief)
//					,lift((b) -> GoalLibExtended.endExploreRecorder(agent))
//				),
				
				GoalLibExtended.exploreTill(agent, belief),
				GoalLibExtended.newObservedNodes(agent, Goals));				
	}
	
	/**
	 * This method is for start recording the exploration time
	 */
	public static Boolean startExploreRecorder(TestAgent agent) {
		System.out.println("predicate: start to save the data in explore goal structure ");
		agent.registerEvent(new TimeStampedObservationEvent("startExploreRecorder"));
		return true;

	}

	/**
	 * This method is for end recording the exploration time
	 */
	public static Boolean endExploreRecorder(TestAgent agent) {
		System.out.println("predicate: end to save the data in explore goal structure ");
		agent.registerEvent(new TimeStampedObservationEvent("endExploreRecorder"));
		return true;

	}

	/** Apply AStar algorithm to find a path to the given goal */
	public static GoalStructure aStar(BeliefState belief, String goalId) {
		return goal("aStar").toSolve((Pair<Boolean, BeliefState> s) -> {
			if ((s.fst) && s.snd.isOpen(goalId))
				return true;
			return false;
		}).withTactic(SEQ(TacticLibExtended.applyAStar(goalId), ABORT())).lift();
	}

	/**
	 * This method will select a nearest node from a list of neighbors to navigate
	 * to it. The selected node should not be visited before
	 */
	public static GoalStructure selectedNode(BeliefState belief, TestAgent agent, Vec3 goalPosition, String goalID) {

		Goal goal1 = goal("Select nearest node to the agent position").toSolve((Pair<String, BeliefState> s) -> {
			System.out.println("select nearest node to the agent position ");
			if (s.fst != null) {
				System.out.println(">> There is a node/entity in the visibility range of the agent!!");
				return true;
			}
			return false;
		});

		Goal goal2 = goal("Check the selectedd node was not visited before").toSolve((Pair<String, BeliefState> s) -> {
			if (s.fst != null)
				return true;
			return false;
		});


		GoalStructure g1 = goal1.withTactic(SEQ(TacticLibExtended.selectNearestNode(goalID), ABORT())).lift();
		GoalStructure g2 = goal2.withTactic(SEQ(TacticLibExtended.unvisitedNode(), ABORT())).lift();

		return SEQ(g1, g2);

	}

	
	/**
	 * This method will select a nearest node from a list of neighbors to navigate
	 * to it. The selected node should not be visited before.
	 * If a goal from the testing task list is seen, it will be selected
	 */
	public static GoalStructure selectedNodeByTask(BeliefState belief, TestAgent agent, int threshold, TestingTaskStack testingGoals, String type, int distance) {

		Goal goal1 = goal("Select nearest node to the agent position").toSolve((Pair<String, BeliefState> s) -> {
			System.out.println("select nearest node to the agent position ");
			if (s.fst != null) {
				System.out.println(">> There is a node/entity in the visibility range of the agent!!");
				return true;
			}
			return false;
		});

		Goal goal2 = goal("Check the selectedd node was not visited before").toSolve((Pair<String, BeliefState> s) -> {		
			if (s.fst != null)
				return true;
			return false;
		});


		GoalStructure g1 = goal1.withTactic(SEQ(TacticLibExtended.selectNearestNodeByTask(testingGoals,threshold,agent,type, distance), ABORT())).lift();
		GoalStructure g2 = goal2.withTactic(SEQ(TacticLibExtended.unvisitedNode(), ABORT())).lift();

		return SEQ(g1, g2);

	}
	
	/**
	 * This goal will make agent to select nearest button to the agent position It
	 * fails if there is no button near the agent position
	 */
	public static GoalStructure selectNeighborButton(BeliefState belief, TestAgent agent) {

		return goal("Select nearest inactive button to the agent position").toSolve((Pair<String, BeliefState> s) -> {
			System.out.println("select nearest inactive button to the agent position: ");
			if (s.fst != null) {
				System.out.println(">> There is a button in agent visiblity range: ");
				return true;
			}
			System.out.println(">> There is no button in agent visiblity range: ");
			return false;
		}).withTactic(SEQ(TacticLibExtended.selectInactiveButton(), ABORT())).lift();
	}

	public static GoalStructure selectNeighborButton(BeliefState belief, TestAgent agent, TestingTaskStack tasks) {

		return goal("Select nearest inactive button to the agent position").toSolve((Pair<String, BeliefState> s) -> {
			System.out.println("select nearest inactive button to the agent position: ");
			if (s.fst != null) {
				System.out.println(">> There is a button in agent visiblity range: ");
				return true;
			}
			System.out.println(">> There is no button in agent visiblity range: ");
			return false;
		}).withTactic(SEQ(TacticLibExtended.selectInactiveButton(tasks), ABORT())).lift();
	}
	
	/**
	 * The type of the selected entity is not identified, based on the type f the
	 * entity we call different method to reach it.
	 */
	public static Boolean entityTypePredicate(BeliefStateExtended belief) {
		System.out.println("predicate: diagnose the type of selected entity " + belief.highLevelGragh.currentSelectedEntity);
		var entityId = belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id;
		if (entityId.contains("door")) {
			return true;
		}
		return false;

	}

	
	/**
	 * The type of the selected entity is not identified, based on the type f the
	 * entity we call different method to reach it.
	 */
	public static Boolean entityTypePredicateByTask(BeliefStateExtended belief) {
		System.out.println("predicate: diagnose the type of selected entity " + belief.highLevelGragh.currentSelectedEntity);
		String entityId;
		if(belief.highLevelGragh.currentSelectedEntity == 100) {
			entityId = belief.highLevelGragh.currentSelectedEntityId;
		}else {
			entityId = belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id;
		}
		if (entityId.contains("door")) {
			return true;
		}
		return false;

	}
	/**
	 * Navigate to the selected button which the id nor the position is not known up
	 * front.
	 */

	public static GoalStructure navigateToButton(BeliefStateExtended b) {
		return goal("This entity is in visible distance: navigate to button").toSolve((BeliefStateExtended belief) -> {

			var entityId = belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id;
			var e = (LabEntity) belief.worldmodel.getElement(entityId);
			if (e == null)
				return false;
			System.out.println(">>>>>>>>>>> navigate to id of the selected button: " + e.id + " : Agent id " + belief.worldmodel.agentId);
			return Vec3.sub(belief.worldmodel().getFloorPosition(), e.getFloorPosition()).lengthSq() <= 0.5;

		}).withTactic(FIRSTof(TacticLibExtended.navigateTo(), // try to move to the entity
				TacticLibExtended.guidedExplore(), // find the entity
				ABORT())).lift();
	}
	
	
	/**
	 * Navigate to the selected button which the id nor the position is not known up
	 * front.
	 */

	public static GoalStructure navigateToButtonByTask(BeliefStateExtended b, TestingTaskStack tasks, TestAgent agent) {
		return goal("This entity is in visible distance: navigate to button").toSolve((BeliefStateExtended belief) -> {

			var entityId = belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id;
			var e = (LabEntity) belief.worldmodel.getElement(entityId);
			if (e == null)
				return false;
			System.out.println(">>>>>>>>>>> navigate to id of the selected button: " + e.id + " : Agent id " + belief.worldmodel.agentId + Vec3.sub(belief.worldmodel().getFloorPosition(), e.getFloorPosition()).lengthSq());
			return Vec3.sub(belief.worldmodel().getFloorPosition(), e.getFloorPosition()).lengthSq() <= 0.5;

		})
		.withTactic(FIRSTof( SEQ(TacticLibExtended.navigateTo(), TacticLibExtended.updateGraghAndTasksSecond(agent, tasks)), // try to move to the entity
				
				SEQ(TacticLibExtended.explore(tasks), TacticLibExtended.updateGraghAndTasksSecond(agent, tasks)) ,// find the entity	
				ABORT())
				).lift();
	}

	/**
	 * Navigate to the selected button which the id nor the position is not known up
	 * front.
	 */

	public static GoalStructure navigateToDoor(BeliefStateExtended b) {
		return goal("This entity is in visible distance: navigate to door").toSolve((BeliefStateExtended belief) -> {
			
			var entityId = belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id;
			var entity = (LabEntity) belief.worldmodel.getElement(entityId);
			if (entity == null)
				return false;
			System.out.println(">>>>>>>>>>> navigate to id of the selected door: " + entity.id);
			System.out.println(">>>>>>>>>>> navigate to id of the selected door: distance "
					+ Vec3.sub(belief.worldmodel().getFloorPosition(), entity.getFloorPosition()).lengthSq());

			if (Vec3.sub(belief.worldmodel().getFloorPosition(), entity.getFloorPosition()).lengthSq() <= 1.5
					&& (belief.evaluateEntity(entity.id, e -> belief.age(e) == 0)))
				return true;
			return false;

		}).withTactic(
				FIRSTof(
					TacticLibExtended.navigateToClosestReachableNode(), // try to move to the entity
					TacticLibExtended.guidedExplore(), // find the entity
					ABORT()
				)).lift();

	}
	
	/**
	 * Navigate to the selected button which the id nor the position is not known up
	 * front.
	 */

	public static GoalStructure navigateToDoorByTaskImproved(BeliefStateExtended b, TestingTaskStack tasks, TestAgent agent) {		
		
		
		GoalStructure withGap = goal("there is no gap:").toSolve((BeliefStateExtended belief) -> {
			String entityId;
			if(belief.highLevelGragh.currentSelectedEntity == 100) {
				entityId = belief.highLevelGragh.currentSelectedEntityId;
				
			}else {
				entityId = belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id;
			}		
			
			System.out.println("Gap, ther eis a gap, keep exploring: what is the entity Id: " + entityId);
			var entity = (LabEntity) belief.worldmodel.getElement(entityId);
			if (entity == null)
				return false;
			double threshold ;
			if(belief.isOpen(entity)) {
				threshold = 1.5;
			}else {
				threshold = 4;
			}
			System.out.println(">>>>>>>>>>>GAP:::: navigate to id of the selected door: distance "
					+ Vec3.sub(belief.worldmodel().getFloorPosition(), entity.getFloorPosition()).lengthSq()
					+" : " +  (belief.evaluateEntity(entity.id, e -> belief.age(e) == 0))
					
					+" : "+ threshold);
			
			if (Vec3.sub(belief.worldmodel().getFloorPosition(), entity.getFloorPosition()).lengthSq() <= threshold
					&& (belief.evaluateEntity(entity.id, e -> belief.age(e) == 0)))
				return true;
			return false;
			
		}).withTactic(
				
				 FIRSTof(
						// SEQ(TacticLibExtended.navigateToClosestReachableNode() , TacticLibExtended.updateGraghAndTasksSecond(agent, tasks)),
						  SEQ(TacticLibExtended.explore(tasks), TacticLibExtended.updateGraghAndTasksSecond(agent, tasks)) ,// find the entity	
								ABORT())

				).lift();
		
		
		GoalStructure g1 =  goal("This entity is in visible distance: navigate to door").toSolve((BeliefStateExtended belief) -> {

			String entityId;
			if(belief.highLevelGragh.currentSelectedEntity == 100) {
				entityId = belief.highLevelGragh.currentSelectedEntityId;
				
			}else {
				entityId = belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id;
			}		
			
			System.out.println("what is the entity Id: " + entityId);
			var entity = (LabEntity) belief.worldmodel.getElement(entityId);
			if (entity == null)
				return false;
			System.out.println(">>>>>>>>>>> navigate to id of the selected door: " + entity.id + " position: " + entity.getFloorPosition() + "entity position: " + belief.worldmodel().getFloorPosition());
			System.out.println(">>>>>>>>>>> navigate to id of the selected door: distance "
					+ Vec3.sub(belief.worldmodel().getFloorPosition(), entity.getFloorPosition()).lengthSq());
			
			// if the door is open the agent should go closer to see the new nodes in the room
			double threshold ;
			if(belief.isOpen(entity)) {
				threshold = 1.5;
			}else {
				threshold = 4;
			}
			
			
			System.out.println(">>>>>>>>>>> threshold " + threshold);
			if (Vec3.sub(belief.worldmodel().getFloorPosition(), entity.getFloorPosition()).lengthSq() <= threshold
					&& (belief.evaluateEntity(entity.id, e -> belief.age(e) == 0)))
				return true;
			return false;

		}).withTactic(
				
				 FIRSTof(
						 SEQ(TacticLibExtended.navigateToClosestReachableNode(), TacticLibExtended.updateGraghAndTasksSecond(agent, tasks)), // try to move to the entity
								
						 SEQ(TacticLibExtended.explore(tasks), TacticLibExtended.updateGraghAndTasksSecond(agent, tasks)) ,// find the entity	
								ABORT())

				).lift();			
			
			return IF2((BeliefStateExtended be) -> checkRechability(b), dummy -> g1, dummy ->REPEAT( withGap));
		
	}

	
	
	public static GoalStructure navigateToDoorByTask(BeliefStateExtended b, TestingTaskStack tasks, TestAgent agent) {		
		GoalStructure g1 =  goal("This entity is in visible distance: navigate to door").toSolve((BeliefStateExtended belief) -> {

			String entityId;
			if(belief.highLevelGragh.currentSelectedEntity == 100) {
				entityId = belief.highLevelGragh.currentSelectedEntityId;
				
			}else {
				entityId = belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id;
			}		
			
			System.out.println("what is the entity Id: " + entityId);
			var entity = (LabEntity) belief.worldmodel.getElement(entityId);
			if (entity == null)
				return false;
			System.out.println(">>>>>>>>>>> navigate to id of the selected door: " + entity.id + " position: " + entity.getFloorPosition() + "entity position: " + belief.worldmodel().getFloorPosition());
			System.out.println(">>>>>>>>>>> navigate to id of the selected door: distance "
					+ Vec3.sub(belief.worldmodel().getFloorPosition(), entity.getFloorPosition()).lengthSq());
			
			// if the door is open the agent should go closer to see the new nodes in the room
			double threshold ;
			if(belief.isOpen(entity)) {
				threshold = 1.5;
			}else {
				threshold = 4;
			}
			
			
			System.out.println(">>>>>>>>>>> threshold " + threshold);
			if (Vec3.sub(belief.worldmodel().getFloorPosition(), entity.getFloorPosition()).lengthSq() <= threshold
					&& (belief.evaluateEntity(entity.id, e -> belief.age(e) == 0)))
				return true;
			return false;
		}).withTactic(
			
			 FIRSTof(
					 SEQ(TacticLibExtended.navigateToClosestReachableNode(), TacticLibExtended.updateGraghAndTasksSecond(agent, tasks)), // try to move to the entity
							
					 SEQ(TacticLibExtended.explore(tasks), TacticLibExtended.updateGraghAndTasksSecond(agent, tasks)) ,// find the entity	
					ABORT())

			).lift();
		
		return g1;
	}
	
	public static Boolean checkRechability(BeliefStateExtended belief) {
		String entityId ;
		if(belief.highLevelGragh.currentSelectedEntity == 100) {
			entityId = belief.highLevelGragh.currentSelectedEntityId;
		} else {
			entityId = belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id;
		}                  
		LabEntity e = belief.worldmodel().getElement(entityId) ;
		
		// in all these cases we need to calculate the node to go
		long startTime = System.currentTimeMillis();
        var entity_location = e.getFloorPosition() ;
        System.out.println("pathToEntity first: " + belief.findPathTo(e.getFloorPosition(),true));
        // fakely unblocking door to make it reachable via pathfinder

        var object  = belief.pathfinder().obstacles.stream().filter(o -> o.obstacle.equals(e)).findAny().get();
        System.out.println("object: " + object.isBlocking + "id: "+((LabEntity) object.obstacle).id);
   
       
        if (belief.prolog.belief  == belief ) {
        	System.out.println(" is th beilief the same:: ");
        }
        System.out.println(" is it open:: " + belief.isOpen(entityId));
        
        // check the path now:
        var pathToEntity =  belief.findPathTo(e.getFloorPosition(),true) ;		        
        		 
        System.out.println("pathToEntity after changing: " + pathToEntity + belief.expensiveFindPathTo(e.position, true));
        
       // belief.prolog.restoreObstacleState(entityId, true);
        
        System.out.println("pathToEntity???: " + pathToEntity);
        if(!belief.prolog.doorIsReachable(entityId) ) {  
        	System.out.println("pathToEntity: " + pathToEntity + belief.prolog.doorIsReachable(entityId)); 
        	long endTime = System.currentTimeMillis();
			long totalTime = 0;
			totalTime = endTime - startTime;
			 System.out.println( "delay time when is null " + totalTime);
        	return false;
        	}
       
        System.out.println("entity location: " + entity_location);
	        return true;

	}
	/**
	 * This goal will make agent to navigate to an entity while the id of the entity
	 * is not define at the beginning nor the position If the path to the entity's
	 * location is not known, the agent explore the game world in the direction
	 * which can lead the agent to the entity. It is exploration but limited to the
	 * specific part of the world.
	 */
	public static GoalStructure navigateTo(BeliefStateExtended b) {
		var goal1 = goal("This entity is in visible distance: navigate to: is it herereee").toSolve((BeliefStateExtended belief) -> {
			
			var entityId = belief.highLevelGragh.currentSelectedEntityId;
			Boolean result;
			var e = (LabEntity) belief.worldmodel.getElement(entityId);
			if (e == null)
				return false;
			System.out.println(">>>>>>>>>>> navigate to id of the selected node: " + e.id);
			if (entityId.contains("door")) {
				System.out.println("navigateTo a door: "
						+ Vec3.dist(belief.worldmodel().getFloorPosition(), e.getFloorPosition()));
				result = Vec3.sub(belief.worldmodel().getFloorPosition(), e.getFloorPosition()).lengthSq() <= 1.5;

			} else {
				System.out.println("navigateTo a button: " + e.id + " ,dis, "
						+ Vec3.sub(belief.worldmodel().getFloorPosition(), e.getFloorPosition()).lengthSq()
						+ " , dis2, " + Vec3.dist(belief.worldmodel().getFloorPosition(), e.getFloorPosition()));

				result = Vec3.sub(belief.worldmodel().getFloorPosition(), e.getFloorPosition()).lengthSq() <= 1;
			}
			return result;
		}).withTactic(FIRSTof(TacticLibExtended.navigateTo(), TacticLibExtended.guidedExplore(), ABORT())).lift();

		return goal1;

	}

	public static GoalStructure navigateTo(BeliefStateExtended b, BeliefStateExtended... b2) {
		String entityId = b.highLevelGragh.currentSelectedEntityId;
		System.out.println("Navigate to with neighbors id" + entityId);
		BeliefStateExtended bSlected = null ;
		for(BeliefStateExtended be:   b2) {			
			if (be.worldmodel.getElement(entityId) != null) {					
				bSlected = be;
			}
		}
		var goal1 = goal("This entity is in visible distance: navigate to").toSolve((BeliefStateExtended belief) -> {			
			LabEntity e = null;
			System.out.println("navigate to based on the two agents setup:" + belief.highLevelGragh.currentSelectedEntityId);
			
			String eId = belief.highLevelGragh.currentSelectedEntityId;
			if(b.highLevelGragh.getIndexById(belief.highLevelGragh.currentSelectedEntityId) != -1) {			
				e = (LabEntity) belief.worldmodel.getElement(eId);
				System.out.println(">>>>>>>>>>> navigate to id of the selected node: if " + e.id);
			}else {	
				System.out.println("select from the neighbors: " + "entity Id"+ eId);
				for(BeliefStateExtended be:   b2) {			
					if (be.worldmodel.getElement(eId) != null) {					
						e = (LabEntity) be.worldmodel.getElement(eId);
					}
				}						
				System.out.println(">>>>>>>>>>> navigate to id of the selected node: else " + e.id);
			}
			 
			Boolean result;
			
			if (e == null) return false;
			System.out.println(">>>>>>>>>>> navigate to id of the selected node: " + e.id);
			if (entityId.contains("door")) {
				System.out.println("navigateTo a door: "
						+ Vec3.dist(belief.worldmodel().getFloorPosition(), e.getFloorPosition()));
				result = Vec3.sub(belief.worldmodel().getFloorPosition(), e.getFloorPosition()).lengthSq() <= 1.5;

			} else {
				System.out.println("navigateTo a button: " + e.id + " ,dis, "
						+ Vec3.sub(belief.worldmodel().getFloorPosition(), e.getFloorPosition()).lengthSq()
						+ " , dis2, " + Vec3.dist(belief.worldmodel().getFloorPosition(), e.getFloorPosition()));

				result = Vec3.sub(belief.worldmodel().getFloorPosition(), e.getFloorPosition()).lengthSq() <= 0.5;
			}
			
			if(result) 
				belief.highLevelGragh.currentSelectedEntity = belief.highLevelGragh.getIndexById(entityId);
			return result;
		}).withTactic(FIRSTof(TacticLibExtended.navigateTo(bSlected), TacticLibExtended.explore(), ABORT())).lift();

		return goal1;

	}

	/**
	 * This goal will make agent to navigate to an entity while the id of the entity
	 * is not define at the beginning nor the position If the path to the entity's
	 * location is not known, the agent explore the game world in the direction
	 * which can lead the agent to the entity. It is exploration but limited to the
	 * specific part of the world.
	 */
	public static GoalStructure navigateTo(BeliefStateExtended b, TestingTaskStack tasks) {
		var goal1 = goal("This entity is in visible distance: navigate to with belief and tasks").toSolve((BeliefStateExtended belief) -> {

			var entityId = belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id;
			Boolean result;
			var e = (LabEntity) belief.worldmodel.getElement(entityId);
			if (e == null)
				return false;
			System.out.println(">>>>>>>>>>> navigate to id of the selected node: " + e.id);
			if (entityId.contains("door")) {
				System.out.println("navigateTo a door: "
						+ Vec3.dist(belief.worldmodel().getFloorPosition(), e.getFloorPosition()));
				result = Vec3.sub(belief.worldmodel().getFloorPosition(), e.getFloorPosition()).lengthSq() <= 1.5;

			} else {
				System.out.println("navigateTo a button: " + e.id + " ,dis, "
						+ Vec3.sub(belief.worldmodel().getFloorPosition(), e.getFloorPosition()).lengthSq()
						+ " , dis2, " + Vec3.dist(belief.worldmodel().getFloorPosition(), e.getFloorPosition()));

				result = Vec3.sub(belief.worldmodel().getFloorPosition(), e.getFloorPosition()).lengthSq() <= 0.5;
			}
			return result;
		}).withTactic(FIRSTof(TacticLibExtended.navigateTo(tasks), TacticLibExtended.guidedExplore(), ABORT())).lift();

		return goal1;

	}
	
	// If then combinator
//	   public static <State>GoalStructure IFTHEN(Predicate<State> p, GoalStructure subgoal) {
//		 GoalStructure[] subgoals = new GoalStructure[2];
//		 subgoals[0] =  GoalLib.lift____(p);
//			return new GoalStructure(GoalsCombinator.FIRSTOF,
//					subgoals[0]
//					,subgoal				
//					) ;
//	   }
	// new Repeat structure

	/**
	 * This predicate returns true if the door is open
	 */
	public static Boolean openDoorPredicate(BeliefState belief, String id) {
		System.out.println("predicate: " + id + belief.isOpen(id));
		if (!belief.isOpen(id)) {
			return true;
		} else {
			return false;
		}

	}

	
	/**
	 * This predicate returns true if ....
	 * @return 
	 * @return 
	 */
	public static boolean highWeightTasks(BeliefState belief, TestAgent agent,TestingTaskStack testingTaskList, int threshold) {	
		System.out.println("predicate if there is high waight tasks ");
		for (TestingTaskStack e : testingTaskList.tasksList) {
			if(e.getweight() > threshold  && !e.getstatus()) {	//&& !e.gettestedBy().contains(agent.getId())
				return true;
			} 
		};
		return false;
	}
	
	/**
	 * This predicate returns true if ....
	 * @return 
	 * @return 
	 */
	public static boolean lowWeightTasks(BeliefState belief, TestAgent agent,TestingTaskStack testingTaskList, int threshold) {	
		System.out.println("predicate if there is low waight tasks");
		for (TestingTaskStack e : testingTaskList.tasksList) {
			if(e.getweight() < threshold  && !e.getstatus()) {	//!e.gettestedBy().contains(agent.getId())
				System.out.println("predicate if there is low waight tasks!!!!!!");
				return true;
			} 
		};
		return false;
	}
	
	
	/**
	 * This predicate returns true if ....
	 * @return 
	 * @return 
	 */
	public static boolean randomTasks(BeliefState belief, TestAgent agent,TestingTaskStack testingTaskList, int threshold) {	
		System.out.println("predicate: select a testing task randomly if there is a task in the stack");
		if(!testingTaskList.tasksList.isEmpty())
			return true;

		return false;
	}
	
	
	/**
	 * This method is a dynamic goal structure to open the blocked entity. Firstly,
	 * the agent checks if there is still buttons or the unexplored area. If all
	 * buttons have tried and there is no undiscovered area to explore means the
	 * door can not be open and the goal is aborted. Then, a button from the agent
	 * positions neighbors is selected. If there is no such a button, look for the
	 * indirect neighbors. If all seen buttons have tried, explore the world to find
	 * a new button. If the path to the selected button is blocked, the prolog is
	 * invoked to unblock the path
	 */
	public static GoalStructure findCorrespondingButton(BeliefStateExtended b, TestAgent agent) {

		System.out.println(" Find corresponding button to open the blocked entity");

		return WHILEDO(
				(BeliefStateExtended belif) -> GoalLibExtended.checkExploreAndButtons(belif), SEQ(
				/*
				 * look for a button, if there is a neighbor select one, if not select indirect
				 * neighbor.after trying all has seen button, the agent will explore the world
				 * to find a inactive button. if there is no- button, it reset all buttons
				 * once(because of multi-connection setup)
				 */

				FIRSTof(GoalLibExtended.selectNeighborButton(b, agent), selectIndirectButton(),
						SEQ(GoalLibExtended.findNodes(agent, b), GoalLibExtended.selectNeighborButton(b, agent))),

				/*
				 * navigate to the selected button and interact with it if the agent can not
				 * navigate to it, means there is something that blocks its way therefore it has
				 * to unblocked the way first.
				 */

				// without checking the prolog

				//GoalLibExtended.navigateTo(b), interact()

				// if we want to use prolog to unblock the agent, in the situation that the path
				// to the selected
				// button is locked

				
				  SEQ( IFELSE2( GoalLibExtended.navigateTo(b) , SUCCESS() , SEQ(
				  findingAButtonToUnlockedAgent(b,agent,"button"), GoalLibExtended.navigateTo(b)
				  ,removeDynamicGoal(agent, "temporaryDoor")			  
				  ) ) ,interact() )
				 
				,

				//after interacting with the button, the agent should checks the blocked entity status. 
				//It should go back to the entity's location, but interacting with buttons might block some
				// doors in the way to reach the blocked door. We use prolog to unblock the path. 
				// without checking the prolog
				/*GoalLibExtended.explorationTo(b.worldmodel.getElement(b.highLevelGragh.currentBlockedEntity).position,
						b.highLevelGragh.currentBlockedEntity),
				checkBlockedEntityStatus(b, agent)*/

				// if we want to use prolog to unblock the agent
				
				  SEQ( 
						  
							  IFELSE2(
								  GoalLibExtended.explorationTo(b.worldmodel.getElement(b.highLevelGragh.
								  currentBlockedEntity).position,b.highLevelGragh.currentBlockedEntity) ,
								  SUCCESS() ,
								  WHILEDO((BeliefStateExtended belif) -> GoalLibExtended.checkEntityVisibilityPredicate(belif),
									  SEQ(
											  findingAButtonToUnlockedAgent(b,agent,"door"),
											  removeDynamicGoal(agent,"temporaryDoor"),
											  GoalLibExtended.explorationTo(
													  b.worldmodel.getElement(b.highLevelGragh.currentBlockedEntity).position,
													  b.highLevelGragh.currentBlockedEntity)
											  
											  
											  
										) 
							  ) 
						  ),
				checkBlockedEntityStatus(b, agent) )
				 

		));
	}

	
	public static GoalStructure findCorrespondingButtonByTask(BeliefStateExtended b, TestAgent agent, TestingTaskStack tasks) {

		System.out.println(" Find corresponding button to open the blocked entity: tasks" + "agent Id " + agent.getId());

		return WHILEDO(
				(BeliefStateExtended belif) -> GoalLibExtended.checkExploreAndButtons(belif,tasks), SEQ(
				/*
				 * look for a button, if there is a neighbor select one, if not select indirect
				 * neighbor.after trying all has seen button, the agent will explore the world
				 * to find a inactive button. if there is no- button, it reset all buttons
				 * once(because of multi-connection setup)
				 */

				FIRSTof(GoalLibExtended.selectNeighborButton(b, agent, tasks), selectIndirectButton(tasks),
						SEQ(GoalLibExtended.findNodes(agent, b, tasks), GoalLibExtended.selectNeighborButton(b, agent,tasks))),

				/*
				 * navigate to the selected button and interact with it if the agent can not
				 * navigate to it, means there is something that blocks its way therefore it has
				 * to unblocked the way first.
				 */

				// without checking the prolog

				//GoalLibExtended.navigateTo(b), interact()

				// if we want to use prolog to unblock the agent, in the situation that the path
				// to the selected
				// button is locked

				
				  SEQ(
						  IFELSE2( GoalLibExtended.navigateTo(b,tasks) , SUCCESS() , SEQ(
								 //FIRSTof(  findingAButtonToUnlockedAgent(b,agent,"button"), removeLuckedItem(agent, tasks) )
								  findingAButtonToUnlockedAgent(b,agent,"button")
								  , GoalLibExtended.navigateTo(b,tasks)
								  ,removeDynamicGoal(agent, "temporaryDoor")			  
								  	) 
								  ) 
					,interact() ) 
				,

				//after interacting with the button, the agent should checks the blocked entity status. 
				//It should go back to the entity's location, but interacting with buttons might block some
				// doors in the way to reach the blocked door. We use prolog to unblock the path. 
				// without checking the prolog
				/*GoalLibExtended.explorationTo(b.worldmodel.getElement(b.highLevelGragh.currentBlockedEntity).position,
						b.highLevelGragh.currentBlockedEntity),
				checkBlockedEntityStatus(b, agent)*/

				// if we want to use prolog to unblock the agent
				
				  SEQ( 
						  IFELSE2(
							  GoalLibExtended.explorationTo(b.worldmodel.getElement(b.highLevelGragh.
							  currentBlockedEntity).position,b.highLevelGragh.currentBlockedEntity) ,
							  SUCCESS() ,
							  WHILEDO((BeliefStateExtended belif) -> GoalLibExtended.checkEntityVisibilityPredicate(belif),
								  SEQ(
										  findingAButtonToUnlockedAgent(b,agent,"door"),
										  removeDynamicGoal(agent,"temporaryDoor"),
										  GoalLibExtended.explorationTo(
												  b.worldmodel.getElement(b.highLevelGragh.currentBlockedEntity).position,
												  b.highLevelGragh.currentBlockedEntity)
  
									) 
						  ) 
						  ),
				checkBlockedEntityStatus(b, agent,tasks) )
				 

		));
	}
	
	
	
	/**
	 * this method will interact with the selected node which is set in the variable
	 * currentSelectedEntity
	 */

	public static GoalStructure interact() {
		return goal("approach the current node and interact with it").toSolve((BeliefStateExtended belief) -> {
			var entityId = belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id;
			System.out.println("interacted button" + entityId + belief.isOn(entityId));
			return true;
		}).withTactic(SEQ(TacticLibExtended.interact(), ABORT())).lift();
	}
	
	public static GoalStructure interact(TestingTaskStack tasks) {
		return goal("approach the current node and interact with it").toSolve((BeliefStateExtended belief) -> {
			var entityId = belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id;
			System.out.println("interacted button" + entityId + belief.isOn(entityId));
//			if(belief.isOn(entityId)) {
//				System.out.print("remove the button" + entityId + " from the lucked list");
//				tasks.removeLuckedItems(entityId);
//				return true;
//			}
			return false;
		}).withTactic(SEQ(TacticLibExtended.interact(), ABORT())).lift();
	}

	/**
	 * This method will return a node(button) which does not have the direct edge to
	 * the selected node in high-level graph
	 */

	public static GoalStructure selectIndirectButton() {
		return goal("look for the indirect neighbors for interactin").toSolve((Pair<String, BeliefState> s) -> {
			System.out.println("look for the indirect neighbors for interactin");
			if (s.fst != null) {
				System.out.println(">> there is a indirect button to interact: ");
				return true;
			}
			System.out.println(">> there is no indirect button to interact!!");
			return false;
		}).withTactic(SEQ(TacticLibExtended.indirectNeighbors(), ABORT())).lift();
	}
	
	public static GoalStructure selectIndirectButton(TestingTaskStack task) {
		return goal("look for the indirect neighbors for interactin task").toSolve((Pair<String, BeliefState> s) -> {
			System.out.println("look for the indirect neighbors for interactin task");
			if (s.fst != null) {
				System.out.println(">> there is a indirect button to interact: ");
				return true;
			}
			System.out.println(">> there is no indirect button to interact!!");
			return false;
		}).withTactic(SEQ(TacticLibExtended.indirectNeighbors(task), ABORT())).lift();
	}

	/**
	 * This method use prolog to unblock the agent. The agent selects a button to
	 * open the closed door. If there is such a button.
	 */
	public static GoalStructure findingAButtonToUnlockedAgent(BeliefStateExtended b, TestAgent agent, String s) {
		return goal("Using the prolog to find the connected button").toSolve((BeliefStateExtended belief) -> {
			System.out.println(">>Checkin the prolog to find a button to unlocked the agent");
			return true;
		}).withTactic(SEQ(TacticLibExtended.unlockAgent(b, agent, s),
				// TacticLibExtended.unlockAgentWithTheLastInteractedButton(b, agent),
				ABORT())).lift();
	}
	//this one is only when the agent wants to navigate to a button and then the path is lucked.
	public static GoalStructure findingAButtonToUnlockedAgent2(BeliefStateExtended b, TestAgent agent, String s) {
		return goal("Using the prolog to find the connected button").toSolve((BeliefStateExtended belief) -> {
			System.out.println(">>Checkin the prolog to find a button to unlocked the agent");
			return true;
		}).withTactic(SEQ(TacticLibExtended.unlockAgent2(b, agent, s),
				// TacticLibExtended.unlockAgentWithTheLastInteractedButton(b, agent),
				ABORT())).lift();
	}
	public static GoalStructure findingAButtonToUnlockedAgent(BeliefStateExtended b, TestAgent agent, String s,BeliefStateExtended... b2) {
		return goal("Using the prolog to find the connected button").toSolve((BeliefStateExtended belief) -> {
			System.out.println(">>Checking the prolog to find a button to unlocked the agent");
			return true;
		}).withTactic(SEQ(TacticLibExtended.unlockAgent(b, agent, s,b2),
				// TacticLibExtended.unlockAgentWithTheLastInteractedButton(b, agent),
				ABORT())).lift();
	}

	/**
	 * This method use prolog to unblock the agent. The agent selects a button to
	 * open the closed door. If there is such a button.
	 */
	public static GoalStructure findingAButtonToUnlockedAgentReachinButton(BeliefStateExtended b, TestAgent agent, String s) {
		return goal("Using the prolog to find the connected button").toSolve((BeliefStateExtended belief) -> {
			System.out.println(">>Checkin the prolog to find a button to unlocked the agent when it tries to reach a button");
			return true;
		}).withTactic(SEQ(TacticLibExtended.unlockAgent(b, agent,s),
				// TacticLibExtended.unlockAgentWithTheLastInteractedButton(b, agent),
				ABORT())).lift();
	}
	
	/**
	 * After facing with a blocked entity, the agent looks for a button to open it
	 * At the ends, it needs to check the blocked entity status.If it is open, to
	 * continue exploring forward, we again set the currentSelectedNode to the door
	 * id.
	 */
	public static GoalStructure checkBlockedEntityStatus(BeliefStateExtended b, TestAgent agent) {

		return goal("checking the blocked node's state").toSolve((BeliefStateExtended belief) -> {
			System.out.println(">>checking the blocked node's state: id, " + belief.highLevelGragh.currentBlockedEntity
					+ ", status: " + belief.isOpen(belief.highLevelGragh.currentBlockedEntity));
			
			belief.highLevelGragh.currentSelectedEntity = belief.highLevelGragh
					.getIndexById(belief.highLevelGragh.currentBlockedEntity);
			if (belief.isOpen(belief.highLevelGragh.currentBlockedEntity)) {

				return true;
			}
			return false;
		}).withTactic(SEQ(TacticLib.observe(), TacticLibExtended.checkBlockedEntityStatus(b, agent), ABORT())).lift();

	}
	
	public static GoalStructure checkBlockedEntityStatus(BeliefStateExtended b, TestAgent agent, TestingTaskStack tasks) {

		return goal("checking the blocked node's state").toSolve((BeliefStateExtended belief) -> {
			System.out.println(">>checking the blocked node's state: id, " + belief.highLevelGragh.currentBlockedEntity
					+ ", status: " + belief.isOpen(belief.highLevelGragh.currentBlockedEntity));
			
			System.out.println("belief.highLevelGragh.currentSelectedEntity" + belief.highLevelGragh.currentSelectedEntity + "entity id" + belief.highLevelGragh.currentSelectedEntityId + "id of this one" + belief.highLevelGragh
					.getIndexById(belief.highLevelGragh.currentSelectedEntityId
							)
			+ belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id
					);
			
			tasks.removeLuckedItems( belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id);
			belief.highLevelGragh.currentSelectedEntity = belief.highLevelGragh
					.getIndexById(belief.highLevelGragh.currentBlockedEntity);
			if (belief.isOpen(belief.highLevelGragh.currentBlockedEntity)) {

				return true;
			}
			return false;
		}).withTactic(SEQ(TacticLib.observe(), TacticLibExtended.checkBlockedEntityStatus(b, agent), ABORT())).lift();

	}

	/**
	 * To avoid repeating the previous dynamic goals that has been added, we need to
	 * remove them at the end of the testing task.
	 */
	public static GoalStructure removeDynamicGoal(TestAgent agent, String goalId) {
		return goal("Remove dynamicly added goal structures").toSolve((BeliefStateExtended belief) -> {
			return true;
		}).withTactic(action("remove dynamic goal if exist one").do1((BeliefStateExtended belief) -> {
			String removedGoal;
			removedGoal = belief.highLevelGragh.currentBlockedEntity;

			if (goalId != null)
				removedGoal = goalId;
			if (!belief.goalsmap.isEmpty()) {
				// dynamic goal should be removed
				System.out.println("remove a goal : " + removedGoal + "tried door status: "+ belief.isOpen(removedGoal));
				agent.remove(belief.goalsmap.get(removedGoal));
				belief.goalsmap.clear();				
			}
			return belief;
		}).lift()).lift();
	}

	/**
	 * To avoid repeating the previous dynamic goals that has been added, we need to
	 * remove them at the end of the testing task.
	 */
	public static GoalStructure removeDynamicGoalByTask(TestAgent agent, String goalId, TestingTaskStack tasks) {
		return goal("Remove dynamicly added goal structures by list of tasks").toSolve((BeliefStateExtended belief) -> {
			return true;
		}).withTactic(action("remove dynamic goal if exist one").do1((BeliefStateExtended belief) -> {
			
			String removedGoal = belief.highLevelGragh.currentBlockedEntity;

			System.out.println("is the goals map empty: " + belief.goalsmap + belief.goalsmap.size() );
			if (goalId != null)
				removedGoal = goalId;
			if (!belief.goalsmap.isEmpty()) {
				// dynamic goal should be removed
				System.out.println("remove a goal by task: " + removedGoal + "tried door status: "+ belief.isOpen(removedGoal));
				agent.remove(belief.goalsmap.get(removedGoal));
				belief.goalsmap.clear();	
			}
			
			String id = removedGoal;
			TestingTaskStack x  = (TestingTaskStack) tasks.tasksList.stream().filter(e-> id.equals(e.getitemId())).findAny().orElse(null);
			if(belief.isOpen(id)) {
				//it might be already removed in the update state
				if(x != null) {
					//add it to the Done list and remove from the toDo tasks list
					tasks.setdoneTasks2(new ArrayList<>(List.of(removedGoal,agent.getId())));
					//tasks.setdoneTasks(removedGoal,agent.getId());
					System.out.println("remove it from the lit of task: " + x.getitemId());
					tasks.tasksList.remove(x);
				}
			}else {
				System.out.println("increase the item id when it could not solve it: " + x.getitemId());
				x.setStatus(false);
				x.settriedItem();
			}
			return belief;
		}).lift()).lift();
	}
	
	
	//removeLuckedItems
	public static GoalStructure removeLuckedItem( TestAgent agent,  TestingTaskStack tasks) {
		return goal("Remove lucked items").toSolve((Boolean b) -> {
			return false;
		}).withTactic(action("Remove lucked items").do1((BeliefStateExtended belief) -> {
					
			String removedGoal  = belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id;
			System.out.println("Remove lucked items when it can not for any reason navigate to it: " + removedGoal );
			tasks.removeLuckedItems( removedGoal);
			return false;
		}).lift()).lift();
	}
	
	/**
	 * This method checks the belief to see if the final goal is visible and it is
	 * open
	 */
	public static GoalStructure finalGoal(String goalId) {
		return goal("Check the final goal").toSolve((BeliefState belief) -> {
			if (belief.isOpen(goalId)) {
				return true;
			}
			return false;
		}).withTactic(SEQ(TacticLib.observe(), ABORT())).lift();
	}

	
	/**
	 * This method checks the belief to see if the final goal is visible and it is
	 * open
	 */
	public static GoalStructure Failed() {
		return goal("Force it to Always fail!!!").toSolve((BeliefState belief) -> {			
			System.out.println("*****always failss");
			return true;
		}).withTactic(SEQ(TacticLib.observe(), ABORT())).lift();
	}
	/**
	 * This method will construct a goal to navigate the agent to the position of
	 * the given id. If the path is not known, it explore the world in the specific
	 * direction.
	 */
	public static GoalStructure explorationTo(Vec3 position, String id) {
		Goal goal = goal("Explor to the given direction").toSolve((BeliefState belief) -> {
			
			//System.out.println("explore to with position : " + id + " position: " + + " agent location: " + belief.worldmodel().getFloorPosition());
			if (Vec3.sub(belief.worldmodel().getFloorPosition(), position).lengthSq() <= 1.5
					&& (belief.evaluateEntity(id, e -> belief.age(e) == 0))
					
					)
				return true;
			return false;
		}).withTactic(FIRSTof(TacticLibExtended.navigateToClosestReachableNode(id),
				TacticLibExtended.guidedExplore(position), ABORT()));
		return goal.lift();
	}

	/** Predicate to check the selected node state */
	public static Boolean checkEntityStatePredicate(BeliefStateExtended belief, TestingTaskStack toDo, int treshold, String agentId, String type) {
		
		var selectedNode = belief.highLevelGragh.currentSelectedEntity;
		System.out.println("Check selected entity state, predicate: " + selectedNode);
		String entityId ;
		if (selectedNode != null) {
			if(selectedNode == 100) {
				entityId = belief.highLevelGragh.currentSelectedEntityId;
			}else {
				var entity = belief.highLevelGragh.entities.get(selectedNode);
				entityId = entity.id;
			}
			toDo.tasksList.forEach(e -> System.out.println("alll tasks" + e.getitemId() + e.getweight()));
			
			for (TestingTaskStack tasks : toDo.tasksList) {
				if (tasks.getitemId().equals(entityId)) {
					if (entityId.contains("door")) {						
						System.out.println("Check selected entity state/*****: " + belief.isOpen(entityId) + tasks.getweight() + tasks.getitemId() + type+ entityId + type.contains("higher"));
						System.out.println("Check selected"+ entityId + tasks.gettestedBy().contains(agentId) + agentId + tasks.gettestedBy());
						if(tasks.getstatus() && !tasks.gettestedBy().contains(agentId)) {
							System.out.println("Check herereree");
							return false;
						}
							if(type.contains("higher") && (tasks.getweight() > treshold) ) {
								if (!belief.isOpen(entityId) ) {								
									return true;
								}else {
									//the task is checked and it is open. It should be removed from the list
									//add it to the Done list and remove from the toDo tasks list
									//toDo.setdoneTasks(entityId,agentId);
									toDo.setdoneTasks2(new ArrayList<>(List.of(entityId,agentId)));
									toDo.tasksList.remove(tasks);				
									return false;
								}
							}
							if(type.contains("lower") && (tasks.getweight() < treshold) ) {
								if (!belief.isOpen(entityId) ) {
									//add the door into testing tasks stack,					
									return true;
								}else {
									//the task is checked and it is open. It should be removed from the list
									//add it to the Done list and remove from the toDo tasks list
									toDo.setdoneTasks2(new ArrayList<>(List.of(entityId,agentId)));						
									toDo.tasksList.remove(tasks);				
									return false;
								}
							}
							if(type.contains("random") ) {
								if (!belief.isOpen(entityId) ) {									
									//add the door into testing tasks stack,					
									return true;
								}else {
									//the task is checked and it is open. It should be removed from the list
									//add it to the Done list and remove from the toDo tasks list
									//toDo.setdoneTasks(entityId,agentId);		
									toDo.setdoneTasks2(new ArrayList<>(List.of(entityId,agentId)));
									toDo.tasksList.remove(tasks);				
									return false;
								}
							}
						}					
				}
			}
		}
		return false;

	}

	
	/** Predicate to check the selected node is visible */
	public static Boolean checkEntityVisibilityPredicate(BeliefStateExtended belief) {
		System.out.println("Check if selected entity is visible, predicate: ");
		String selectedNode = belief.highLevelGragh.currentBlockedEntity;
		Vec3 position = belief.worldmodel.getElement(selectedNode).position;
		if (Vec3.sub(belief.worldmodel().getFloorPosition(), position).lengthSq() <= 1.5
				|| (belief.evaluateEntity(selectedNode, e -> belief.age(e) == 0)))
			return false;

		return true;
		
		
	}
	
	
	/**
	 * This goal will interact with a button and check the corresponding door which
	 * is known upfront.
	 */

	public static GoalStructure interactWithAButtonAndCheckDoor(String buttonId, String doorID) {
		return SEQ(GoalLib.entityInteracted(buttonId), GoalLib.entityStateRefreshed(doorID));
	}

	/**
	 * This method is invoked when the blocked entity is known and the agent first
	 * checks if there is a button connected o it before adding a subgoal to solve
	 * it Checking the prolog data set to find a corresponding button to open the
	 * current blocked entity
	 **/
	public static GoalStructure checkPrologToFindACorrespondinButton(TestAgent agent) {

		return goal("check the prolog data set to find a corresponding button")
				.toSolve((Pair<Boolean, BeliefStateExtended> s) -> {
					System.out.println("is there a button?" + s.fst);
					if (s.fst) {
						System.out.println("YES!!");
						return true;
					}
					System.out.println("NO!!");
					return false;
				}).withTactic(SEQ(TacticLibExtended.lookForAbutton(agent), ABORT())).lift();

	}

	/**
	 * This method is invoked when the blocked entity is known and the agent first
	 * checks if there is a button connected o it before adding a subgoal to solve
	 * it Checking the prolog data set to find a corresponding button to open the
	 * current blocked entity
	 **/
	public static GoalStructure checkPrologToFindACorrespondinButtonByTask(TestAgent agent, TestingTaskStack task) {

		return goal("check the prolog data set to find a corresponding button")
				.toSolve((Pair<Boolean, BeliefStateExtended> s) -> {
					System.out.println("is there a button?" + s.fst);
					if (s.fst) {
						System.out.println("YES!!");
						return true;
					}
					System.out.println("NO!!");
					return false;
				}).withTactic(SEQ(TacticLibExtended.lookForAbutton(agent, task), ABORT())).lift();

	}
	
	
	/**
	 * This method will marked the selected node(button) to open the current blocked
	 * node to prevent being selected again
	 */

	public static GoalStructure mark(BeliefStateExtended b) {

		return goal("marked the selected node to prevent selecteing again").toSolve((BeliefStateExtended belief) -> {
			return true;
		}).withTactic(action("remove dynamic goal if exist one").do1((BeliefStateExtended belief) -> {
			System.out.println("mark");
			String currentNodeId = belief.highLevelGragh.currentBlockedEntity;
			int selectedNode = belief.highLevelGragh.currentSelectedEntity;
			String selectedNodeId = belief.highLevelGragh.entities.get(selectedNode).id;
			System.out.println("mark" + selectedNodeId + currentNodeId);
			belief.buttonDoorConnection.get(currentNodeId).add(selectedNodeId);
			return belief;
		}).lift()).lift();
	}

	/**
	 * Look for a button to open the blocked selected entity If the selected entity
	 * is a blocked door, we firstly check the prolog data set to know is there any
	 * button related to this door, if not we interact with has seen buttons.
	 */
	public static GoalStructure dynamicGoal(BeliefStateExtended b, TestAgent agent, TestingTaskStack tasks, BeliefStateExtended...b2) {

		//this code is match with the structure mentioned in the paper. 
		// it does not work because the last part which is related to the exploring still needs to debug.
		// the problem is explorationTo() is based on the current blocked node which I do not have access to it 
		// in this level(it is null at the beginning). I should find a way to pass it to this function.
		/*return
		  WHILEDO( 
				  (BeliefStateExtended belief) -> checkEntityStatePredicate(b), 
				  SEQ(
						  IFELSE2(
								  checkPrologToFindACorrespondinButton2(agent), //call prolog to return a button
								  checkBlockedEntityStatus(b, agent),
								  checkKnownButtonToFindButton2(agent,b)
								  SEQ(
								  checkKnownButtonToFindButton2(agent,b) // dynamically ad a goal to find a button(neighbor/not neighbor)
								 findCorrespondingButton2(b,agent)) // or just do it in sequence
								  
								  ),
						  mark(b),
						  navigateTo(b),
						  interact(),
						  //reach(o)
						 
						  GoalLibExtended.explorationTo(b.worldmodel.getElement(b.highLevelGragh.currentBlockedEntity).position,
									b.highLevelGragh.currentBlockedEntity),					
					  removeDynamicGoal(agent, null),
					  FAIL()
					  ) 
				  );*/
		
		
		// if we want to use the data constructed, uncomment below line
		System.out.print("dynamic goal: first check the prolog then check known buttons!");
		  return SEQ(
				  IFELSE2(// firstly check the prolog, if not, check all known buttons
						  checkPrologToFindACorrespondinButtonByTask(agent,tasks),
						  checkBlockedEntityStatus(b, agent , tasks), 
						  FIRSTof( checkKnownButtonToFindButtonByTask(agent,tasks), removeDynamicGoal(agent, null))),

				 IFELSE2(	//if none of the knows buttons can open it, ask other agents for a not visited button					  					
						 checkBlockedEntityStatus(b, agent , tasks) ,
						  SUCCESS(),
						  FIRSTof( checkAgentsToFindButton(b,agent,b2) , removeDynamicGoal(agent, null))
				  ),
				  removeDynamicGoalByTask(agent, null, tasks),
				  FAIL()
				  );
		  
		  // without prolog
		/*
		 * return SEQ( IFELSE2(FAIL(), checkBlockedEntityStatus(b, agent),
		 * checkKnownButtonToFindButton(agent)) ,removeDynamicGoal(agent, null),FAIL());
		 */
	}

	/**
	 * Dynamically add a new goal in order to open the blocked entity. the goal is
	 * to interact with the selected node which has seen before, and check the
	 * blocked entity status.
	 */
	public static GoalStructure checkKnownButtonToFindButton(TestAgent agent) {
		return goal("add a new goal to open the door, looking for a button to open the correspanding door")
				.toSolve((Pair<Integer, BeliefStateExtended> s) -> {
					System.out.println("find a nearest button to interact" + s.fst);
					// this goal should always returns false because the parents combinator is
					// FirstOF if we use IFELSE combinator. therefore to add a new goal after this
					// one and
					// check it this should return false. if we do not want to use IFELSE then the
					// above
					// comments are true.
					return false;
				}).withTactic(SEQ(TacticLibExtended.dynamicallyAddGoalToFindButton(agent), ABORT())).lift();
	}

	
	
	/**
	 * Dynamically add a new goal in order to open the blocked entity. the goal is
	 * to interact with the selected node which has seen before, and check the
	 * blocked entity status.
	 */
	public static GoalStructure checkKnownButtonToFindButtonByTask(TestAgent agent, TestingTaskStack tasks) {
		var g = goal("add a new goal to open the door, looking for a button to open the correspanding door: tasks" + agent.getId())
				.toSolve((Pair<Integer, BeliefStateExtended> s) -> {
					System.out.println("find a nearest button to interact" + s.fst + " :  "+ agent.getId());
					// this goal should always returns false because the parents combinator is
					// FirstOF if we use IFELSE combinator. therefore to add a new goal after this
					// one and
					// check it this should return false. if we do not want to use IFELSE then the
					// above
					// comments are true.
					return false;
				}).withTactic(SEQ(TacticLibExtended.dynamicallyAddGoalToFindButtonByTask(agent, tasks), ABORT())).lift();
	
	return g.maxbudget(10);
	}
	
	
	

	/**
	 * Check if other agents have some enablers to unblocked the selected blocked item. 
	 * If there is a button add the goal dynamically to interact and check the door
	 */
	public static GoalStructure checkAgentsToFindButton(BeliefStateExtended b, TestAgent agent, BeliefStateExtended... b2) {
		return SEQ(
				IFELSE(
						(BeliefStateExtended belif) -> askAgentsPredicate(b, agent, b2),					
						dynamicllyAddGoalToUnblock(agent,b2),
						Failed()
						)
				, FAIL());
	}
	
	
	public static Boolean askAgentsPredicate(BeliefStateExtended belief, TestAgent agent, BeliefStateExtended... belief2) {
		List<String> allButtons = new ArrayList();
		// Collect all buttons in the high-level graph.
		belief.highLevelGragh.entities.forEach(button -> {
			if (button.id.contains("button")) {	
				System.out.println("all buttons visited by agent: ask Agents Predicate" + button.id);
					allButtons.add(button.id);
				}
		});
		
		for(BeliefStateExtended b:   belief2) {			
			if (b.checkEnablers(allButtons))
				return b.checkEnablers(allButtons);
		}
		return false;
		
	}
	
	
	public static GoalStructure dynamicllyAddGoalToUnblock(TestAgent agent, BeliefStateExtended... b) {
		return goal("add a goal to open the blocked door by asking other agents!")
				.toSolve((Pair<Integer, BeliefStateExtended> s) -> {
					System.out.println("find a button which is not visited before" + s.fst);				
					return true;
				}).withTactic(SEQ(TacticLibExtended.dynamicllyAddGoalToUnblock(agent,b), ABORT())).lift();	
	}
	
	
	public static GoalStructure findButtonFromNeighbors(BeliefStateExtended b1, TestAgent agent, BeliefStateExtended... b2) {
		
		return WHILEDO(
				(BeliefStateExtended belif) -> GoalLibExtended.askAgentsPredicate(belif, agent, b2), SEQ(
				/*
				 * look for a button, till there is a button in an neighboring agent
				 */

				GoalLibExtended.selectNeighborButtonFromAgents(b1, agent,b2),

				/*
				 * navigate to the selected button and interact with it if the agent can not
				 * navigate to it, means there is something that blocks its way therefore it has
				 * to unblocked the way first.
				 */
			
				//when it want to unblock it, it should look at the second agent
				  SEQ( IFELSE2( GoalLibExtended.navigateTo(b1,b2) , SUCCESS() , SEQ(
				  findingAButtonToUnlockedAgent(b1,agent,"door",b2), GoalLibExtended.navigateTo(b1,b2)
				  ,removeDynamicGoal(agent, "temporaryDoor")			  
				  ) ) ,interact() )
				 
				,

				//after interacting with the button, the agent should checks the blocked entity status. 
				//It should go back to the entity's location, but interacting with buttons might block some
				// doors in the way to reach the blocked door. We use prolog to unblock the path. 
				// without checking the prolog
				/*GoalLibExtended.explorationTo(b.worldmodel.getElement(b.highLevelGragh.currentBlockedEntity).position,
						b.highLevelGragh.currentBlockedEntity),
				checkBlockedEntityStatus(b, agent)*/

				// if we want to use prolog to unblock the agent
				
				  SEQ( 
						  
							  IFELSE2(
								  GoalLibExtended.explorationTo(b1.worldmodel.getElement(b1.highLevelGragh.
								  currentBlockedEntity).position,b1.highLevelGragh.currentBlockedEntity) ,
								  SUCCESS() ,
								  WHILEDO((BeliefStateExtended belief) -> GoalLibExtended.checkEntityVisibilityPredicate(belief),
									  SEQ(
											  findingAButtonToUnlockedAgent(b1,agent,"door"),
											  removeDynamicGoal(agent,"temporaryDoor"),
											  GoalLibExtended.explorationTo(
													  b1.worldmodel.getElement(b1.highLevelGragh.currentBlockedEntity).position,
													  b1.highLevelGragh.currentBlockedEntity)
											  
											  
											  
										) 
							  ) 
						  ),
				checkBlockedEntityStatus(b1, agent) )
				 

		));
	}
	
	
	/**
	 * select an enabler from the other agents
	 */
	public static  GoalStructure selectNeighborButtonFromAgents(BeliefStateExtended b1, TestAgent agent, BeliefStateExtended... b2) {
		return goal("Select a button from neighbors").toSolve((Pair<String, BeliefState> s) -> {
			System.out.println("Select a button from neighbors ");
			if (s.fst != null) {
				System.out.println(">> There is a button that agent has not tried before: ");
				return true;
			}
			System.out.println(">> There is not a button that agent has not tried before:: ");
			return false;
		}).withTactic(SEQ(TacticLibExtended.selectButtonFromNeighbors(agent,b2), ABORT())).lift();
	}
	
	/**
	 * move the agent to stop in a empty space
	 */
	public static GoalStructure moveAgent(TestAgent agent) { 
		return DEPLOY(agent,
		  (BeliefStateExtended belief) -> {
			  
				int k=0 ;
				int s = 0;
				for(Vec3 x : belief.highLevelGragh.vertices) {
		    		System.out.println("selected node belief highLevelGragh.vertices: "+ x);
				}
				Vec3 selectedPosition = null;
				
				for (int vid=0; vid < belief.pathfinder().vertices.size() ; vid++) {
				    var v = belief.pathfinder().vertices.get(vid);
				    if (belief.pathfinder().seenVertices.get(vid)) {
				    	var path = belief.findPathTo(v,true);
				    	if(path != null) {
				    		s = 0;
					    	for(Vec3 x : belief.highLevelGragh.vertices) {
					    		
						    	if ( 6 < (Vec3.distSq(v, x)) ) {
						    		s++;
						    	}
					    	}
					    	k++;
					    	if(s == belief.highLevelGragh.vertices.size()) {selectedPosition = v; break;}
				    	}
				    }
				    }
				 
				    System.out.println("selected node : "+ selectedPosition + k  +"  : " + s + belief.worldmodel.position);
				    if(selectedPosition == null) return null; 
				    return positionsVisited(selectedPosition);
		   }
	    ) ;
	}
	
	
	
	
	/****************************************************************************************************/
	 //This part is not completed. The functions are related to the dynamicgoal goal structure. They are mainly 
	 // the same functions as we already have just small change to make it more dynamic
	public static GoalStructure checkPrologToFindACorrespondinButton2(TestAgent agent) {

		return goal("check the prolog data set to find a corresponding button")
				.toSolve((Pair<Boolean, BeliefStateExtended> s) -> {
					System.out.println("is there a button?" + s.fst);
					if (s.fst) {
						System.out.println("YES!!");
						return true;
					}
					System.out.println("NO!!");
					return false;
				}).withTactic(SEQ(TacticLibExtended.lookForAbutton2(agent), ABORT())).lift();

	}

	public static GoalStructure checkKnownButtonToFindButton2(TestAgent agent, BeliefStateExtended belief) {
		return goal("add a new goal to open the door, looking for a button to open the correspanding door")
				.toSolve((Pair<Integer, BeliefStateExtended> s) -> {
					System.out.println("find a nearest button to interact" + s.fst);
					// this goal should always returns false because the parents combinator is
					// FirstOF if we use IFELSE combinator. therefore to add a new goal after this
					// one and
					// check it this should return false. if we do not want to use IFELSE then the
					// above
					// comments are true.

					var selectedNode = belief.highLevelGragh.currentSelectedEntity;
					var entity = belief.highLevelGragh.entities.get(selectedNode);
					GoalStructure unblockedDoor = GoalLibExtended.findCorrespondingButton2(belief, agent);
					agent.addAfter(unblockedDoor);
					System.out.println(">>**** A new goal to open the blocked door added "
							+ belief.highLevelGragh.currentBlockedEntity);
					belief.goalsmap.put(entity.id, unblockedDoor);

					return false;
				}).withTactic(SEQ(TacticLibExtended.dynamicallyAddGoalToFindButton2(agent), ABORT())).lift();
	}

	public static GoalStructure findCorrespondingButton2(BeliefStateExtended b, TestAgent agent) {

		System.out.println(" Find corresponding button to open the blocked entity");

		return WHILEDO((BeliefStateExtended belif) -> GoalLibExtended.checkExploreAndButtons(belif), SEQ(
				/*
				 * look for a button, if there is a neighbor select one, if not select indirect
				 * neighbor.after trying all has seen button, the agent will explore the world
				 * to find a inactive button. if there is no- button, it reset all buttons
				 * once(because of multi-connection setup)
				 */

				FIRSTof(GoalLibExtended.selectNeighborButton(b, agent), selectIndirectButton(),
						SEQ(GoalLibExtended.findNodes(agent, b), GoalLibExtended.selectNeighborButton(b, agent)))

		/*
		 * navigate to the selected button and interact with it if the agent can not
		 * navigate to it, means there is something that blocks its way therefore it has
		 * to unblocked the way first.
		 */

		// if we want to use prolog to unblock the agent, in the situation that the path
		// to the selected
		// button is locked

		/*
		 * SEQ( IFELSE2( GoalLibExtended.navigateTo(b) , SUCCESS() , SEQ(
		 * findingAButtonToUnlockedAgent(b,agent), GoalLibExtended.navigateTo(b)
		 * ,removeDynamicGoal(agent, "temporaryDoor")
		 * 
		 * ) ) ,interact() )
		 */

		// with out checking the prolog

		// if we want to use prolog to unblock the agent
		/*
		 * SEQ( IFELSE2(
		 * GoalLibExtended.explorationTo(b.worldmodel.getElement(b.highLevelGragh.
		 * currentBlockedEntity).position,b.highLevelGragh.currentBlockedEntity) ,
		 * SUCCESS() ,
		 * SEQ(findingAButtonToUnlockedAgent(b,agent),GoalLibExtended.explorationTo(b.
		 * worldmodel.getElement(b.highLevelGragh.currentBlockedEntity).position,b.
		 * highLevelGragh.currentBlockedEntity), removeDynamicGoal(agent,
		 * "temporaryDoor")) ) , checkBlockedEntityStatus(b, agent) )
		 */

		));
	}

	public static GoalStructure explorationTo(BeliefStateExtended b) {
		Goal goal = goal("Explor to the given direction").toSolve((BeliefState belief) -> {

			var position = b.worldmodel.getElement(b.highLevelGragh.currentBlockedEntity).position;
			var id = b.highLevelGragh.currentBlockedEntity;
			if (Vec3.sub(belief.worldmodel().getFloorPosition(), position).lengthSq() <= 1.5
					|| (belief.evaluateEntity(id, e -> belief.age(e) == 0)))
				return true;
			return false;
		}).withTactic(FIRSTof(TacticLibExtended.navigateToBlockedNode(), TacticLibExtended.guidedExplore(), ABORT()));
		return goal.lift();
	}
}
