package gameTestingContest;

import static nl.uu.cs.aplib.agents.PrologReasoner.and;
import static nl.uu.cs.aplib.agents.PrologReasoner.not;
import static nl.uu.cs.aplib.agents.PrologReasoner.predicate;
import static nl.uu.cs.aplib.agents.PrologReasoner.rule;

import java.util.*;
import java.util.stream.Collectors;

import alice.tuprolog.InvalidTheoryException;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import eu.iv4xr.framework.spatial.LineIntersectable;
import eu.iv4xr.framework.spatial.Obstacle;
import eu.iv4xr.framework.spatial.Vec3;
import nl.uu.cs.aplib.agents.PrologReasoner.PredicateName;
import nl.uu.cs.aplib.agents.PrologReasoner.Rule;
import nl.uu.cs.aplib.utils.Pair;
import world.BeliefState;
import world.BeliefStateExtended;
import world.HighLevelGraph;
import world.LabEntity;

public class Prolog {

	

	public BeliefStateExtended belief; 
	public Prolog(BeliefStateExtended belief) {
		// TODO Auto-generated constructor stub
		this.belief = belief;
	}



	// Below we define predicates and relations capturing LR-logic:
	static PredicateName isRoom = predicate("room") ;
	static PredicateName isButton = predicate("button") ;
	static PredicateName isDoor = predicate("door") ;
	static PredicateName inRoom = predicate("inRoom") ;
	static PredicateName wiredTo = predicate("wiredTo") ;
	static PredicateName notWiredTo = predicate("notWiredTo") ;	
	static PredicateName neighbor = predicate("neighbor") ;
	
	
	/**
	 * A rule defining when two rooms are neighboring, namely when they share a door.
	 */
	static Rule neigborRule = rule(neighbor.on("R1","R2"))
			.impBy(isRoom.on("R1"))
			.and(isRoom.on("R2"))
			.and("(R1 \\== R2)") // ok have to use this operator
			.and(isDoor.on("D"))
			.and(isRoom.on("R1","D"))
			.and(isRoom.on("R2","D"))
			;
	static PredicateName roomReachable = predicate("roomReachable") ;
	
	// Below are three rules defining when a room R2 is reachable from a room R1, through
	// k number of edges/doors.
	
	static Rule roomReachableRule1 = rule(roomReachable.on("R1","R2","1"))
			.impBy(neighbor.on("R1","R2"))
			;
	
	static Rule roomReachableRule2 = rule(roomReachable.on("R1","R2","K+1"))
			.impBy(neighbor.on("R1","R"))
			.and(roomReachable.on("R","R2","K"))
			.and("(R1 \\== R2)")
			.and("K > 0")
			;
	
	static Rule roomReachableRule3 = rule(roomReachable.on("R1","R2","K+1"))
			.impBy(roomReachable.on("R1","R2","K"))
			.and("K > 0")
			;
	
	/**
	 * Execute a prolog query, with var_ as the query variable. Returing a single solution,
	 * or null if there is none.
	 */
	String pQuery(String var_, String q) {
		return belief.prolog().query(q).str_(var_) ;
	}
	
	/**
	 * Test if a prolog query q is successful (has some result).
	 */

	List<String> pQueryAll(String var_, String q) {
		return belief.prolog()
			   . queryAll(q).stream()
			   . map(Q -> Q.str_(var_))
			   . collect(Collectors.toList()) ;
	}
	
	
	// end logic
	
	public void registerButton(String button) throws InvalidTheoryException {
		if (pTest(isButton.on(button))) {
			// button is already registered
			return;
		}
		// else this is a new button: 
		belief.prolog().facts(isButton.on(button)) ;
		
		// now check in which room it can be placed:

		Vec3 agentOriginalPosition = belief.worldmodel.position;
		Map<String, Boolean> originalDoorBlockingState = (getDoorsBlockingState() == null) ? getDoorsBlockingState() : null;
		if(belief.pathfinder != null)fakelyMakeAlldoorsBlocking(null);

		var rooms = pQueryAll("R", isRoom.on("R"));
		boolean added = false;
		for (var roomx : rooms) {
			String bt0 = pQuery("B", and(inRoom.on(roomx, "B"), isButton.on("B")));
			var ee = bt0;
			if (bt0 != null) {
				// the button can be added if there is another button in the room that is
				// reachable
				// from it:
				
				var hum = belief.worldmodel.getElement(bt0);
				
				WorldEntity b0 = belief.worldmodel.getElement(bt0);
				belief.worldmodel.position = b0.position.copy();
				belief.worldmodel.position.y = agentOriginalPosition.y;

				if (buttonIsReachable(button)) {
					// add the button:
					belief.prolog().facts(inRoom.on(roomx, button));
					added = true;
					break;
				}
			}
		}
		if (!added) {
			// button is in a new room, create the room too:
			String newRoom = "room" + rooms.size();
			belief.prolog().facts(isRoom.on(newRoom), inRoom.on(newRoom, button));			
			WorldEntity b_ = belief.worldmodel.getElement(button);
			belief.worldmodel.position = b_.position.copy();
			belief.worldmodel.position.y = agentOriginalPosition.y;
			
			// check which doors are connected to the new room:
			var doors = pQueryAll("D", isDoor.on("D"));
			for(var d0 : doors) {
				if(doorIsReachable(d0)) {
					belief.prolog().facts(inRoom.on(newRoom,d0)) ;
				}
			}	
		}

		// restore the doors' state:
		belief.worldmodel.position = agentOriginalPosition;
		if(originalDoorBlockingState != null)restoreDoorsBlockingState(originalDoorBlockingState, null);

		if (added) {
			DebugUtil.log(">>>>> registering " + button);
		}
	}
	
	
	public void registerDoor(String door) throws InvalidTheoryException {
		if (pTest(isDoor.on(door))) {
			// door is already registered
			return;
		}
		// new door. Add it:
		belief.prolog().facts(isDoor.on(door));

		// Now check which rooms it connects:

		Vec3 agentOriginalPosition = belief.worldmodel.position;
		Map<String, Boolean> originalDoorBlockingState = (getDoorsBlockingState() == null) ? getDoorsBlockingState() : null;
		fakelyMakeAlldoorsBlocking(door);

		var rooms = pQueryAll("R", isRoom.on("R"));
		int numbersOfAdded = 0; // the number of rooms the door has been added to. Max 2.
		for (var roomx : rooms) {
			String bt0 = pQuery("B", and(inRoom.on(roomx, "B"), isButton.on("B")));
			if (bt0 != null) {
				// the door can be added if there is a button the the room that is
				// reachable from it:

				WorldEntity b0 = belief.worldmodel.getElement(bt0);
				belief.worldmodel.position = b0.position.copy();
				belief.worldmodel.position.y = agentOriginalPosition.y;

				if (doorIsReachable(door)) {
					// add the door to this room:
					belief.prolog().facts(inRoom.on(roomx, door));
					numbersOfAdded++;
					if (numbersOfAdded == 2) {
						// a door can only connect at most two rooms!
						break;
					}
				}
			}
		}

		// restore the doors' state:
		belief.worldmodel.position = agentOriginalPosition;
		if(originalDoorBlockingState != null) restoreDoorsBlockingState(originalDoorBlockingState,door);
		
		if (numbersOfAdded > 0) {
			DebugUtil.log(">>>>> registering " + door);
		}
	}
	
	
	public void registerConnection(String button, String door) throws InvalidTheoryException {
		if(pTest(wiredTo.on(button,door))) {
			// the connection is already registered
			return ;
		}
		boolean wasReportedAsUnconnected = pTest(notWiredTo.on(button,door)) ;
		
		belief.prolog().facts(wiredTo.on(button,door)) ;
		if (wasReportedAsUnconnected) {
			belief.prolog().removeFacts(notWiredTo.on(button,door)) ;
		}
		
		DebugUtil.log(">>>>> registering connection " + button + " -> " 
		   + door 
		   + (wasReportedAsUnconnected ? " (conflict: was NON-connection!)" : "")) ;
	}

	public void registerNONConnection(String button, String door) throws InvalidTheoryException {
		if(pTest(notWiredTo.on(button,door))) {
			// the non-connection is already registered
			return ;
		}
		
		if(pTest(wiredTo.on(button,door))) {
		    // was previously reported as connected. Ignoring the new non-connection report
			DebugUtil.log(">>>>> IGNORING reported NON-connection " + button 
					  + " X " + door + " because it is already marked as connected.") ;
			return ;
		}
		
		belief.prolog().facts(notWiredTo.on(button,door)) ;
		DebugUtil.log(">>>>> registering NON-connection " + button + " X " + door) ;
	}
	
	/**
	 * Test if a prolog query q is successful (has some result).
	 */
	boolean pTest(String q) {
		return belief.prolog().query(q) != null ;
	}
	
	void restoreDoorsBlockingState(Map<String,Boolean> originalState, String exception) {
		for(Obstacle<LineIntersectable> o : belief.pathfinder.obstacles) {
			LabEntity e = (LabEntity) o.obstacle ;
			if (e.type == "Door") {
				o.isBlocking = originalState.get(e.id) ;
				if(e.id != exception) {
					e.extent.x -= 1f ;
					e.extent.z -= 1f ;				
				}
			}
		}
	}
	
	public boolean doorIsReachable(String door) {
		LabEntity d = belief.worldmodel.getElement(door) ;
		Boolean isOpen = d.getBooleanProperty("isOpen") ;
		//if(isOpen) {
		//	return findPathTo(d.getFloorPosition(),true) != null ;
		//}
		// pretend that the door is open:
		fakelyUnblockDoor(door) ;
		var entity_location = d.getFloorPosition() ;
		var entity_sqcenter = new Vec3((float) Math.floor((double) entity_location.x - 0.5f) + 1f,
	    		entity_location.y,
	    		(float) Math.floor((double) entity_location.z - 0.5f) + 1f) ;
		var path = belief.findPathTo(entity_sqcenter,true) ;
		// restore the original state:
		restoreObstacleState(door, ! isOpen) ;
		return path != null ;		
	}
	
	void fakelyUnblockDoor(String door) {
		for(Obstacle<LineIntersectable> o : belief.pathfinder.obstacles) {
			LabEntity e = (LabEntity) o.obstacle ;
			if (e.id == door) {
				o.isBlocking = false ;
				return ;
			}
		}
	}
	
	void restoreObstacleState(String door, boolean originalState) {
		for(Obstacle<LineIntersectable> o : belief.pathfinder.obstacles) {
			LabEntity e = (LabEntity) o.obstacle ;
			if (e.id == door) {
				o.isBlocking = originalState ;
				return ;
			}
		}
	}
	
	void fakelyMakeAlldoorsBlocking(String exception) {
		for(Obstacle<LineIntersectable> o : belief.pathfinder.obstacles) {
			LabEntity e = (LabEntity) o.obstacle ;
			if (e.type == "Door") {
				o.isBlocking = true ;
				if(e.id != exception) {
					e.extent.x += 1f ;
					e.extent.z += 1f ;				
				}
			}
		}
	}
	
	Map<String,Boolean> getDoorsBlockingState() {
		Map<String,Boolean> map = new HashMap<>() ;	
		if(belief.pathfinder == null) return null;
		for(Obstacle<LineIntersectable> o : belief.pathfinder.obstacles) {
			LabEntity e = (LabEntity) o.obstacle ;
			if (e.type == "Door") {
				map.put(e.id,o.isBlocking) ;
			}
		}
		return map ;
	}
	
	boolean buttonIsReachable(String button) {
		LabEntity b = belief.worldmodel.getElement(button) ;
		var path = belief.findPathTo(b.getFloorPosition(),true) ;
		return path != null ;
	}
	
	Set<Pair<String,String>> getConnections() {
		Set<Pair<String,String>> cons = new HashSet<>() ;
		for(WorldEntity B : belief.knownButtons()) {
			var doors = pQueryAll("D", and(isDoor.on("D"), wiredTo.on(B.id,"D"))) ;
			for(var D : doors) {
				cons.add(new Pair(B.id,D)) ;
			}
		}
		return cons ;
	}
	
	public void report() {
		 System.out.println("buttons" + 
				 belief.prolog().queryAll(
	   					 	and(isButton.on("B")
	   					 ))
	   	 					.stream().map(Q -> Q.str_("B"))
	   	 					.collect(Collectors.toList())) ;	
		 
		 System.out.println("doors" + 
				 belief.prolog().queryAll(
	   					 	and(isDoor.on("D")))
	   	 					.stream().map(Q -> Q.str_("D"))
	   	 					.collect(Collectors.toList())) ;
		 Set<Pair<String, String>> report = getConnections();
		 
		 for (Pair<String, String> connection : report) {
	            System.out.println("conections: "+"   Button " + connection.fst + " toggles " + connection.snd);
	        }
		 		
			
	
	}	
	
	
}
