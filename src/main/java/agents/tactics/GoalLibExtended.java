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
import nl.uu.cs.aplib.mainConcepts.Tactic;
import nl.uu.cs.aplib.utils.Pair;
import world.BeliefState;
import world.BeliefStateExtended;
import world.HighLevelGraph;
import world.LabEntity;

public class GoalLibExtended extends GoalLib {
	
	
	public static <State> GoalStructure IFELSE2(GoalStructure p, GoalStructure g1, GoalStructure g2) {
	   // GoalStructure not_g = lift((State state) -> p.test(state));
	   return FIRSTof(SEQ(p, g1), g2);
	}

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
	 */
	public static Boolean checkExplore(BeliefStateExtended belief) {

		var explore = belief.pathfinder().explore(belief.worldmodel().getFloorPosition(),
				BeliefStateExtended.DIST_TO_FACE_THRESHOLD);
		
		
		System.out.println("Check if there is unvisited part to explore: " + explore);
		System.out.println(belief.worldmodel().getFloorPosition());
		if (explore != null) {
			System.out.println("There is unvisited nodes to explore!!! ");
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
		if (explore != null) {			
			return true;
		} else {
			
			// Collect all buttons in the high-level graph.
			belief.highLevelGragh.entities.forEach(button -> {
				if (button.id.contains("button")) {				
						allButtons.add(button.id);
					}
			});
						
			// check the list of visited button for this blocked entity. if there is still a
			// button which is not in the
			// visited list, return true
			for (int i = 0; i < allButtons.size(); i++) {
				if (!belief.buttonDoorConnection.get(currentBlockedNode).contains(allButtons.get(i)))				
					return true;
			}
			
		}		
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
	 * to it The selected node should not be visited before
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

//	   	  Goal goal3 =  goal("check the selected node is a blocked entity")
//	   			. toSolve(
//	       				(Pair<String,BeliefState> s) -> {
//	       					if(s.fst != null) return true;
//	       					return false;
//	       		});	

		GoalStructure g1 = goal1.withTactic(SEQ(TacticLibExtended.selectNearestNode(goalID), ABORT())).lift();
		GoalStructure g2 = goal2.withTactic(SEQ(TacticLibExtended.unvisitedNode(), ABORT())).lift();

//	   	 GoalStructure g3 = goal3.withTactic(SEQ(
//	   	    		TacticLib.checkEntityStatus(agent),
//	                 ABORT())).lift();
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

	/**
	 * The type of the selected entity is not identified, based on the type f the
	 * entity we call different method to reach it.
	 */
	public static Boolean entityTypePredicate(BeliefStateExtended belief) {
		System.out.println("predicate: diagnose the type of selected entity ");
		var entityId = belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id;
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
			System.out.println(">>>>>>>>>>> navigate to id of the selected button: " + e.id);
			return Vec3.sub(belief.worldmodel().getFloorPosition(), e.getFloorPosition()).lengthSq() <= 1;

		}).withTactic(FIRSTof(TacticLibExtended.navigateTo(), // try to move to the entity
				TacticLibExtended.guidedExplore(), // find the entity
				ABORT())).lift();
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
			System.out.println(">>>>>>>>>>> navigate to id of the selected door: " 
								+ entity.id
								+ ",distance: "
								+ Vec3.sub(belief.worldmodel().getFloorPosition(), entity.getFloorPosition()).lengthSq());

			if (Vec3.sub(belief.worldmodel().getFloorPosition(), entity.getFloorPosition()).lengthSq() <= 1.5
					&& (belief.evaluateEntity(entity.id, e -> belief.age(e) == 0)))
			//if (Vec3.sub(belief.worldmodel().getFloorPosition(), entity.getFloorPosition()).lengthSq() <= 1.5
			//	&& (belief.evaluateEntity(entity.id, e -> belief.age(e) <= 5)))
				return true;
			return false;

		}).withTactic(FIRSTof(TacticLibExtended.navigateToClosestReachableNode(), // try to move to the entity
				TacticLibExtended.guidedExplore(), // find the entity
				ABORT())).lift();

	}

	/**
	 * This goal will make agent to navigate to an entity while the id of the entity
	 * is not define at the beginning nor the position If the path to the entity's
	 * location is not known, the agent explore the game world in the direction
	 * which can lead the agent to the entity. It is exploration but limited to the
	 * specific part of the world.
	 */
	public static GoalStructure navigateTo(BeliefStateExtended b) {
		var goal1 = goal("This entity is in visible distance: navigate to").toSolve((BeliefStateExtended belief) -> {

			var entityId = belief.highLevelGragh.entities.get(belief.highLevelGragh.currentSelectedEntity).id;
			Boolean result;
			var e = (LabEntity) belief.worldmodel.getElement(entityId);
			if (e == null)
				return false;
			System.out.println(">>>>>>>>>>> navigate to id of the selected node: " + e.id);
			if (entityId.contains("door")) {
				System.out.println("navigateTo a door: "
						+ Vec3.dist(belief.worldmodel().getFloorPosition(), e.getFloorPosition()));
				result = Vec3.sub(belief.worldmodel().getFloorPosition(), e.getFloorPosition()).lengthSq() <= 4;

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
	 * This predicate returns true if the door is closed.
	 */
	public static Boolean isDoorClosedPredicate(BeliefState belief, String id) {
		System.out.println("predicate: " + id + belief.isOpen(id));
		if (!belief.isOpen(id)) {
			return true;
		} else {
			return false;
		}

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
				  findingAButtonToUnlockedAgent(b,agent,"door"), GoalLibExtended.navigateTo(b)
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
	 * This method will construct a goal to navigate the agent to the position of
	 * the given id. If the path is not known, it explore the world in the specific
	 * direction.
	 */
	public static GoalStructure explorationTo(Vec3 position, String id) {
		Goal goal = goal("Explor to the given direction").toSolve((BeliefState belief) -> {
			if (Vec3.sub(belief.worldmodel().getFloorPosition(), position).lengthSq() <= 1.5
					|| (belief.evaluateEntity(id, e -> belief.age(e) == 0)))
				return true;
			return false;
		}).withTactic(FIRSTof(TacticLibExtended.navigateToClosestReachableNode(id),
				TacticLibExtended.guidedExplore(position), ABORT()));
		return goal.lift();
	}

	/** Predicate to check the selected node state */
	public static Boolean checkEntityStatePredicate(BeliefStateExtended belief) {
		System.out.println("Check selected entity state, predicate: ");
		var selectedNode = belief.highLevelGragh.currentSelectedEntity;
		if (selectedNode != null) {
			var entity = belief.highLevelGragh.entities.get(selectedNode);
			if (entity.type.equals(LabEntity.DOOR)) {
				if (!belief.isOpen(entity.id)) {
					return true;
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
	public static GoalStructure dynamicGoal(BeliefStateExtended b, TestAgent agent) {

		//this code is match with the structure mentioned in the paper. 
		// it does not work because the last part which is related to the exploring still needs to debug.
		// the problem is explorationTo() is based on the current blocked node which I do not have access to it 
		// in this level(it is null at the beginning). I should find a way to pass it to this function.
		/*return
		  WHILEDO( 
				  (BeliefStateExtended belif) -> checkEntityStatePredicate(b), 
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
		  return SEQ(IFELSE2(checkPrologToFindACorrespondinButton(agent),
		  checkBlockedEntityStatus(b, agent), checkKnownButtonToFindButton(agent)),
		  removeDynamicGoal(agent, null), FAIL());
		  
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
