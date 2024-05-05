package agents.tactics;

import static nl.uu.cs.aplib.AplibEDSL.*;

import java.util.function.Predicate;

import alice.tuprolog.InvalidTheoryException;
import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import eu.iv4xr.framework.spatial.Vec3;
import myUtils.DebugUtil;
import nl.uu.cs.aplib.mainConcepts.*;
import world.BeliefStateExtended;
import world.LabEntity;

public class OnlineSearch {
	
	/**
	 * This is the online-search algorithm, as in the paper, expressed in terms of aplib's
	 * goal-structures.
	 * 
	 * @param targetBlocker The target blocker/door whose state we want to change, from blocking
	 *  				    to unblocking.
	 *  
	 * @param targetApproxPosition If specified, we can use this to give an approximate location
	 * 						of the target-blocker.
	 */
	@SuppressWarnings("deprecation")
	static public GoalStructure onlineSearchAlgorithm1(TestAgent agent, 
			String targetBlocker,
			Vec3 targetApproxPosition,
			Predicate<BeliefStateExtended> psi
			) {

		var agentState = (BeliefStateExtended) agent.state();

		var search = SEQ(
			// while psi is not established, we run a loop:
			WHILEDO((BeliefStateExtended B) -> ! psi.test(B), 
				
				SEQ(
					FIRSTof(
					   GoalLibExtended.newObservedNodes(agent),
					   WHILEDO((BeliefStateExtended B) -> GoalLibExtended.checkExplore(B),
							   SEQ(GoalLibExtended.findNodes(agent, agentState)))
					   ),
					
					FIRSTof(
					   // if during the exploration to find a new entity, agent sees the goal, we check
					   // that it is open or not
					   GoalLibExtended.finalGoal(targetBlocker),
					   // if the goal is not achieved yet, we select an entity to navigate to it based
					   // on some specific policies
					   // if the all neighbors of the current position has seen before
					   GoalLibExtended.selectedNode(agentState, agent, targetApproxPosition, targetBlocker)
					   ),
					
					IFELSE((BeliefStateExtended b) -> GoalLibExtended.entityTypePredicate(agentState),
					   // SEQ(GoalLibExtended.navigateToDoor(beliefState),lift((BeliefStateExtended b)
					   // ->
					   // GoalLibExtended.clearPath(beliefState)),GoalLibExtended.entityInCloseRange(beliefState)),
					   GoalLibExtended.navigateToDoor(agentState),
					   GoalLibExtended.navigateToButton(agentState)
					   ),
						
					IFELSE((BeliefStateExtended b) -> GoalLibExtended.checkEntityStatePredicate(agentState),
					   GoalLibExtended.dynamicGoal(agentState, agent), 
					   FAIL()),
					 // ,
					 // GoalLibExtended.checkEntityStatus(testAgent)
					 GoalLibExtended.removeDynamicGoal(agent, null) 

					 // GoalLibExtended.aStar(beliefState,"door3")
					 // GoalLibExtended.finalGoal(targetBlocker)
					 )
			  ));

		return search ;
	}
	
	/**
	 * Track a model of the game-under-test. This registers newly detected game objects,
	 * and logical connections between enablers and blockers. The algorithm works by
	 * inspecting the given agent-state. The state tracks objects that the agent has seen
	 * (new and in the past). The algorithm uses the objects time-stamps to detect which
	 * are recently added. The gained information is the registered into a Prolog database
	 * (which is linked from the state).
	 * 
	 * <p>In this implementation, only doors and buttons are tracked.
	 * 
	 * <p>The method also takes information on the button that is last interacted by the agent
	 * as parameter, and return a new last interacted button, if one is detected, and else
	 * the same button is returned.
	 */
	static public WorldEntity trackModel(BeliefStateExtended state, WorldEntity lastInteractedButton) {
				
		// Register newly found game-objects. Only those within a certain threshold distance
		// will be registered.
		float SqDistanceThreshord = 4 ;
		for(var e : state.worldmodel.elements.values()) {
			if ( e.type.equals(LabEntity.DOOR) || e.type.equals(LabEntity.SWITCH)){
				if(e.timestamp == state.worldmodel.timestamp) {
					var sqdist = Vec3.distSq(state.worldmodel.position,e.position) ;
					if(sqdist <= SqDistanceThreshord) {
						try {
							if(e.type.equals(LabEntity.SWITCH)) {			
								state.prolog.registerButton(e.id);
							}
							if(e.type.equals(LabEntity.DOOR)) {
								//System.out.println("belief state, register a door");
								state.prolog.registerDoor(e.id);
							}
						} catch (InvalidTheoryException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
				}
			}
		}
		
		// check if a button is just interacted:
		for(WorldEntity e: state.changedEntities) {
			
			if(e.type.equals("Switch") && e.hasPreviousState()) {
				DebugUtil.log(">> detecting interaction with " + e.id) ;
				lastInteractedButton = e ;					
			}
		}
		
		// check doors that change state, and add connections to lastInteractedButton:
		if(lastInteractedButton != null) {
			for(WorldEntity e: state.changedEntities) {							
				if(e.type.equals("Door") && e.hasPreviousState()) {
					try {
						state.prolog.registerConnection(lastInteractedButton.id,e.id) ;
					} catch (InvalidTheoryException e1) {
						// ouch...
						e1.printStackTrace();
					}
				}	
			}
		}
		
		return lastInteractedButton ;
	}

}
