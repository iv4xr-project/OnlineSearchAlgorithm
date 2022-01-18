package gameTestingContest;

import static nl.uu.cs.aplib.agents.PrologReasoner.and;
import static nl.uu.cs.aplib.agents.PrologReasoner.not;
import static nl.uu.cs.aplib.agents.PrologReasoner.predicate;
import static nl.uu.cs.aplib.agents.PrologReasoner.rule;

import java.util.*;
import java.util.stream.Collectors;

import alice.tuprolog.InvalidTheoryException;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import eu.iv4xr.framework.mainConcepts.WorldModel;
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
	public Prolog(BeliefStateExtended belief)  {		
		this.belief = belief;
		this.belief.attachProlog() ;
		System.out.print(this.belief.prolog());
		try {			
			this.belief.prolog().add(neigborRule,
					roomReachableRule1, roomReachableRule2, roomReachableRule3,pathBetweenRoomsRule1,pathBetweenRoomsRule2);
		} catch (InvalidTheoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	// Below we define predicates and relations capturing LR-logic:
	static PredicateName isRoom = predicate("room") ;
	static PredicateName isButton = predicate("button") ;
	static PredicateName isDoor = predicate("door") ;
	static PredicateName inRoom = predicate("inRoom") ;
	static PredicateName wiredTo = predicate("wiredTo") ;
	static PredicateName notWiredTo = predicate("notWiredTo") ;	
	static PredicateName neighbor = predicate("neighbor") ;
	static PredicateName roomReachable = predicate("roomReachable") ;
	static PredicateName pathBetweenRooms = predicate("pathBetweenRooms") ;
	
	/**
	 * A rule defining when two rooms are neighboring, namely when they share a door.
	 */
	static Rule neigborRule = rule(neighbor.on("R1","R2"))
			.impBy(isRoom.on("R1"))
			.and(isRoom.on("R2"))
			.and("(R1 \\== R2)") // ok have to use this operator
			.and(isDoor.on("D"))
			.and(inRoom.on("R1","D"))
			.and(inRoom.on("R2","D"))
			;
	
	
	// Below are three rules defining when a room R2 is reachable from a room R1, through
	// k number of edges/doors.
	
	static Rule roomReachableRule1 = rule(roomReachable.on("R1","R2","1"))
			.impBy(neighbor.on("R1","R2"))
			;
	
//	static Rule roomReachableRule2 = rule(roomReachable.on("R1","R2","K+1"))
//			.impBy(neighbor.on("R1","R"))
//			.and(roomReachable.on("R","R2","K"))
//			.and("(R1 \\== R2)")
//			.and("K > 0")
//			;
//	
//	static Rule roomReachableRule3 = rule(roomReachable.on("R1","R2","K+1"))
//			.impBy(roomReachable.on("R1","R2","K"))
//			.and("K > 0")
//			;

	Rule roomReachableRule2 = rule(roomReachable.on("R1","R2","K"))
			.impBy(neighbor.on("R1","R"))
			.and("K > 0")
			.and("(R1 \\== R2)")
			.and("L is K-1")
			.and(roomReachable.on("R","R2","L"))
			;
	
	Rule roomReachableRule3 = rule(roomReachable.on("R1","R2","K"))
			.impBy("K > 0")
			.and("L is K-1")
			.and(roomReachable.on("R1","R2","L"));
	
	Rule pathBetweenRoomsRule1 = rule(pathBetweenRooms.on("R1","R2","[R1,D,R2]","1"))
			.and(isDoor.on("D"))
			.and(inRoom.on("R1","D"))
			.and(inRoom.on("R2","D"))
			;
	
	Rule pathBetweenRoomsRule2 = rule(pathBetweenRooms.on("R1","R2","[R1 , D | S]","K"))
			.impBy("K > 0")
			.and(isDoor.on("D"))
			.and(inRoom.on("R1","D"))
			.and(inRoom.on("R","D"))
			.and("(R1 \\== R2)")
			.and("L is K-1")
			.and(pathBetweenRooms.on("R","R2","S","L"))
			;
	/**
	 * Execute a prolog query, with var_ as the query variable. Returning a single solution,
	 * or null if there is none.
	 */
	String pQuery(String var_, String q) {	
		if(belief.prolog().query(q) == null) return null;
		return belief.prolog().query(q).str_(var_) ;
	}
	
	/**
	 * Test if a prolog query q is successful (has some result).
	 */

	List<String> pQueryAll(String var_, String q) {
		
		if(belief.prolog()
				   . queryAll(q) == null)
		System.out.println("pQueryaAll" );
		
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
		//System.out.println("register a button :: " + button);
		// now check in which room it can be placed:

		Vec3 agentOriginalPosition = belief.worldmodel.position.copy();
		
		Map<String, Boolean> originalDoorBlockingState = (getDoorsBlockingState() != null) ? getDoorsBlockingState() : null;
		fakelyMakeAlldoorsBlocking(null);

		
		//just to know :
		
				var allbuttons = pQueryAll("B", and(isRoom.on("R"), inRoom.on("R", "B"), isButton.on("B")));
				var allRoom = pQueryAll("R", and(isRoom.on("R")));
				
				//System.out.println("allbuttons " + allbuttons + "all rooms " + allRoom);
				
				for (var roomx : allRoom) {
					//System.out.println("buttons in each room: " + roomx);
					List<String> bt0 = pQueryAll("B", and(inRoom.on(roomx, "B"), isButton.on("B")));
					//System.out.println("roomx:  bt0" + bt0);
					
				}
				
				
		var rooms = pQueryAll("R", isRoom.on("R"));
		boolean added = false;
		for (var roomx : rooms) {
			//System.out.println("roomx: " + roomx);
			String bt0 = pQuery("B", and(inRoom.on(roomx, "B"), isButton.on("B")));
			//System.out.println("selected button " + bt0);
			if (bt0 != null) {
				// the button can be added if there is another button in the room that is
				// reachable
				// from it:
				
				
				WorldEntity b0 = belief.worldmodel.getElement(bt0);
				belief.worldmodel.position = b0.position.copy();
				belief.worldmodel.position.y = agentOriginalPosition.y;
				
				//System.out.println("get button position and the current agent position: " + b0.position + belief.worldmodel.position);
				if (buttonIsReachable(button)) {
					// add the button:
					belief.prolog().facts(inRoom.on(roomx, button));
					//System.out.println("is reachable: ");
					added = true;
					break;
				}
			}
		}
		if (!added) {
			// button is in a new room, create the room too:
			String newRoom = "room" + rooms.size();
			System.out.println(">>>>----------------------------------->>>>>> Room size and the fact: " + newRoom + button);
			belief.prolog().facts(isRoom.on(newRoom), inRoom.on(newRoom, button));			
			
			
			WorldEntity b_ = belief.worldmodel.getElement(button);
			belief.worldmodel.position = b_.position.copy();
			belief.worldmodel.position.y = agentOriginalPosition.y;
			
			// check which doors are connected to the new room:
			var doors = pQueryAll("D", isDoor.on("D"));
			for(var d0 : doors) {
				//System.out.println("check which doors are connected to the new room:" + d0 );
				if(doorIsReachable(d0)) {
					//System.out.println("register a door here??" + newRoom + d0);
					added = true; 
					belief.prolog().facts(inRoom.on(newRoom,d0)) ;
				}
			}	
		}

		//just to know :
		var allRoom1 = pQueryAll("R", and(isRoom.on("R")));
		
		//System.out.println("all rooms " + allRoom1);
		for (var roomx : allRoom1) {
			//System.out.println("doors in each room: " + roomx);
			List<String> dt0 = pQueryAll("D", and(inRoom.on(roomx, "D"), isDoor.on("D")));
			//System.out.println("roomx:  dt0" + dt0);
			
		}
		
		
		// restore the doors' state:
		belief.worldmodel.position = agentOriginalPosition;
		restoreDoorsBlockingState(originalDoorBlockingState, null);

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
		System.out.println("register a door :: " + door);
		// Now check which rooms it connects:
		boolean added = false;
		Vec3 agentOriginalPosition = belief.worldmodel.position.copy();		
		Map<String, Boolean> originalDoorBlockingState = (getDoorsBlockingState() != null) ? getDoorsBlockingState() : null;
		fakelyMakeAlldoorsBlocking(door);

		
		var rooms = pQueryAll("R", isRoom.on("R"));
		int numbersOfAdded = 0; // the number of rooms the door has been added to. Max 2.
		for (var roomx : rooms) {
			String bt0 = pQuery("B", and(inRoom.on(roomx, "B"), isButton.on("B")));
			System.out.println(">>which button is selected for registering a door? " + bt0 + " in which room? " + roomx);
			if (bt0 != null) {
				// the door can be added if there is a button in the room that is
				// reachable from it:

				WorldEntity b0 = belief.worldmodel.getElement(bt0);
				belief.worldmodel.position = b0.position.copy();
				belief.worldmodel.position.y = agentOriginalPosition.y;
				//System.out.println("checking door is reachable the button position " + b0.position);
				if (doorIsReachable(door)) {
					// add the door to this room:
					//System.out.println(">>>>>Add a door in which room? " + door + roomx);
					belief.prolog().facts(inRoom.on(roomx, door));
					numbersOfAdded++;
					added = true;
					if (numbersOfAdded == 2) {
						// a door can only connect at most two rooms!
						break;
					}
				}
			}
		}

		if (!added) {
			// door is in a new room, create the room too:
			String newRoom = "room" + rooms.size();
			System.out.println(">>>>----------------------------------->>>>>> Room size and the fact: " + newRoom + door);
			belief.prolog().facts(isRoom.on(newRoom), inRoom.on(newRoom, door));			
			LabEntity d = belief.worldmodel().getElement(door) ;
			var entity_location = d.getFloorPosition() ;
			var entity_sqcenter = new Vec3((float) Math.floor((double) entity_location.x - 0.5f) + 1f,
		    		entity_location.y,
		    		(float) Math.floor((double) entity_location.z - 0.5f) + 1f) ;
			
			//Operate over the all candidates of the North, East, South,West of the current door
			List<Vec3> candidates = new LinkedList<>() ;
		    float delta = 1f ;
		    // adding North and south candidates
		    candidates.add(Vec3.add(entity_sqcenter, new Vec3(0,0,delta))) ;
		    candidates.add(Vec3.add(entity_sqcenter, new Vec3(0,0,-delta))) ;
		    // adding east and west candidates:
		    candidates.add(Vec3.add(entity_sqcenter, new Vec3(delta,0,0))) ;
		    candidates.add(Vec3.add(entity_sqcenter, new Vec3(-delta,0,0))) ;
		    for (var c : candidates) {
				//WorldEntity b_ = belief.worldmodel.getElement(door);
				belief.worldmodel.position = c.copy();
				belief.worldmodel.position.y = agentOriginalPosition.y;
				
				// check which doors are connected to the new room:
				var doors = pQueryAll("D", isDoor.on("D"));
				for(var d0 : doors) {
					System.out.println(">>>>>>check which doors are connected to the new room:" + d0);
					if(!d0.equals(door)) {
						if(doorIsReachable(d0)) {
						//System.out.println("register a door in this new room: " + newRoom + d0);
							belief.prolog().facts(inRoom.on(newRoom,d0)) ;
						}
					}	
				}
		    }
		}
		
		var allRoom1 = pQueryAll("R", and(isRoom.on("R")));
		
		
		
		for (var roomx : allRoom1) {
			//System.out.println("doors in each room:register door " + roomx);
			List<String> bt0 = pQueryAll("D", and(inRoom.on(roomx, "D"), isDoor.on("D")));
		//	System.out.println("door ::" + bt0);
		}
		
		// restore the doors' state:
		belief.worldmodel.position = agentOriginalPosition;
		restoreDoorsBlockingState(originalDoorBlockingState,door);
		
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
		for(Obstacle<LineIntersectable> o : belief.pathfinder().obstacles) {
			LabEntity e = (LabEntity) o.obstacle ;
		//	System.out.println("restore door state " + e.id );
//			if (e.type == "Door") {
//				o.isBlocking = originalState.get(e.id) ;
//				if(e.id != exception) {
//					System.out.println("restore door state!!");
//					e.extent.x -= 1f ;
//					e.extent.z -= 1f ;				
//				}
//			}
			
			if (e.type == "Door" && e.getBooleanProperty("isOpen") && !e.id.equals(exception)) {
				// WP: knowing original-state is not needed anymore??
				// o.isBlocking = originalState.get(e.id) ;
				o.isBlocking = false ;
				if(!e.id.equals(exception)) {
					if(e.extent.x > 0.8) {e.extent.x -= 0.75f; }					
					// WP FIX: should have an else
					// e.extent.z += 0.75f ;	
					else e.extent.z -= 0.75f ;	
				}
			}
		}
	}
	
	public boolean doorIsReachable(String door) {
		LabEntity d = belief.worldmodel().getElement(door) ;
		Boolean isBlockBoolean = null;
		
		for(Obstacle<LineIntersectable> o : belief.pathfinder().obstacles) {
			LabEntity e = (LabEntity) o.obstacle ;			
			if (e.id.equals(door)) {
				
				isBlockBoolean = o.isBlocking;
			}}
		
		//if(isOpen) {
		//	return findPathTo(d.getFloorPosition(),true) != null ;
		//}
		// pretend that the door is open:
		// System.out.println("*****************checking door is reachabale is started");
		fakelyUnblockDoor(door) ;
		
		
		var entity_location = d.getFloorPosition() ;
		var entity_sqcenter = new Vec3((float) Math.floor((double) entity_location.x - 0.5f) + 1f,
	    		entity_location.y,
	    		(float) Math.floor((double) entity_location.z - 0.5f) + 1f) ;
		//System.out.println("door location and center location and agent location" + entity_location + entity_sqcenter + belief.worldmodel.position);
		
		List<Vec3> candidates = new LinkedList<>() ;
	    float delta = 1f ;
	    // adding North and south candidates
	    candidates.add(Vec3.add(entity_sqcenter, new Vec3(0,0,delta))) ;
	    candidates.add(Vec3.add(entity_sqcenter, new Vec3(0,0,-delta))) ;
	    // adding east and west candidates:
	    candidates.add(Vec3.add(entity_sqcenter, new Vec3(delta,0,0))) ;
	    candidates.add(Vec3.add(entity_sqcenter, new Vec3(-delta,0,0))) ;
	    Pair<Vec3, List<Vec3>> path = null;
	    path = belief.expensiveFindPathTo(entity_sqcenter,true) ;
	    //System.out.println("path to the center of the door" + path);
	    int s = 0;
	    if(path == null) {
		    for (var c : candidates) {
		    	// if c (a candidate point near the entity) is on the navigable,
		    	// we should ignore it:
		    	//if (getCoveringFaces(belief,c) == null) continue ;
		    	path = belief.expensiveFindPathTo(c, true) ;
		    	s++;
		    	//System.out.println("***candidators :" + s + c + path);
		    	if(path != null) break;
		    }
	    }
		// path = belief.expensiveFindPathTo(entity_sqcenter,true) ;
		// restore the original state:
		restoreObstacleState(door,  isBlockBoolean) ;
		
		System.out.println("*****************checking door is reachabale is done");
		return path != null ;		
	}
	
	void fakelyUnblockDoor(String door) {
		for(Obstacle<LineIntersectable> o : belief.pathfinder().obstacles) {
			LabEntity e = (LabEntity) o.obstacle ;			
			if (e.id.equals(door)) {		
				System.out.println("fakly unblocking a door:" + door);
				o.isBlocking = false ;	
				return ;
			}
		}
	}
	
	void restoreObstacleState(String door, boolean originalState) {
		for(Obstacle<LineIntersectable> o : belief.pathfinder().obstacles) {
			LabEntity e = (LabEntity) o.obstacle ;
			if (e.id.equals(door)) {
				System.out.print("restore Obstacle State a door:" + door + originalState);
				o.isBlocking = originalState ;				
				return ;
			}
		}
	}
	
	void fakelyMakeAlldoorsBlocking(String exception) {
		for(Obstacle<LineIntersectable> o : belief.pathfinder().obstacles) {
			LabEntity e = (LabEntity) o.obstacle ;
			//System.out.println("fakly make all doors block, open? " + e.id  + ", "+ e.getBooleanProperty("isOpen"));
			if (e.type == "Door" && e.getBooleanProperty("isOpen") && !e.id.equals(exception)) {
				o.isBlocking = true ;
				if(!e.id.equals(exception)) {
					//extend the smaller one
					//System.out.println("fakly make all doors block, x and z " + e.extent.x +", "+ e.extent.y);
					if(e.extent.x < e.extent.z) {e.extent.x += 0.75f; }					
					else e.extent.z += 0.75f ;	
					//System.out.println("After change: fakly make all doors block, x and z " + e.extent.x +", "+ e.extent.y);
				}
			}
		}
	}
	
	Map<String,Boolean> getDoorsBlockingState() {
		Map<String,Boolean> map = new HashMap<>() ;	
		System.out.println("*****get doors bloking state" + belief.pathfinder());
		if(belief.pathfinder() == null) return null;
		for(Obstacle<LineIntersectable> o : belief.pathfinder().obstacles) {
			LabEntity e = (LabEntity) o.obstacle ;
			if (e.type == "Door") {
				map.put(e.id,o.isBlocking) ;
			}
		}
		return map ;
	}
	
	boolean buttonIsReachable(String button) {
		LabEntity b = belief.worldmodel().getElement(button) ;
		//var path = belief.findPathTo(b.getFloorPosition(),true) ;
	    Pair<Vec3, List<Vec3>> path = null;
	    path = belief.expensiveFindPathTo(b.getFloorPosition(),true) ;
	    System.out.println("path to a button from another button in the same room" + path);

		
		return path != null ;
	}
	
	Map<String,Boolean> untrap() {
		Map<String,Boolean> map = new HashMap<>() ;	
		System.out.println("***** tries to find a button connected to a door in the same room as agent is");
		LabEntity d = belief.worldmodel().getElement("agent1") ;
		
		if(belief.pathfinder() == null) return null;
		for(Obstacle<LineIntersectable> o : belief.pathfinder().obstacles) {
			LabEntity e = (LabEntity) o.obstacle ;
			if (e.type == "Door") {
				map.put(e.id,o.isBlocking) ;
			}
		}
		return map ;
	}
	
	Set<Pair<String,String>> getConnections() {
		Set<Pair<String,String>> cons = new HashSet<>() ;
		for(WorldEntity B : belief.knownButtons()) {
			//System.out.println("get connection: " + B.id );
			var doors = pQueryAll("D", and(isDoor.on("D"), wiredTo.on(B.id,"D"))) ;
			//System.out.println("get connection: " + doors.isEmpty() );
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

		 
		 System.out.println("rooms" + 
				 belief.prolog().queryAll(
	   					 	and(isRoom.on("R")))
	   	 					.stream().map(Q -> Q.str_("R"))
	   	 					.collect(Collectors.toList())) ;		 
		 
		 var allRooms = pQueryAll("R",
					and(
							isRoom.on("R")						
									
							)
					   );
			System.out.println("all rooms: " + allRooms);
			
			
			for (var roomx : allRooms) {
				System.out.println("doors in each room: " + roomx);
				List<String> bt0 = pQueryAll("D", and(inRoom.on(roomx, "D"), isDoor.on("D")));
				System.out.println("doors: " + bt0);
				
				System.out.println("buttons in each room: " + roomx);
				List<String> dt0 = pQueryAll("B", and(inRoom.on(roomx, "B"), isButton.on("B")));
				System.out.println("buttons: " + dt0);
			}
			
		 Set<Pair<String, String>> report = getConnections(); 
		 for (Pair<String, String> connection : report) {
	            System.out.println("conections: "+"   Button " + connection.fst + " toggles " + connection.snd);
	        }
		 		
			
	
	}	
	
	/**
	 * @return
	 */
	String getCurrentRoom() {
		Map<String, Boolean> originalDoorBlockingState = getDoorsBlockingState();
		fakelyMakeAlldoorsBlocking(null);
		var rooms = pQueryAll("R", isRoom.on("R"));
		Boolean find = false;
		String currentRoom = null ;
		System.out.println("all rooms in get current room" + rooms);

		for (var roomx : rooms) {			
			String bt0 = pQuery("B", and(inRoom.on(roomx, "B"), isButton.on("B")));
			//System.out.println("get current room function: room and selected butoon" + roomx + bt0);			
			if (bt0 != null) {
				if (buttonIsReachable(bt0)) {
					currentRoom = roomx ;
					find = true;
					break ;
				}
			}
//			else{
//				String dt0 = pQuery("D", and(inRoom.on(roomx, "D"), isDoor.on("D")));
//				System.out.println("get current room by door" + roomx + dt0);	
//				if (dt0 != null) {
//					if (doorIsReachable(dt0)) {
//						System.out.println("it is reachable from the current room");	
//						currentRoom = roomx ;
//						break ;
//					}
//				}	
//			}
		}
		
		//if there is no button in the current room, we should check the doors.
		// if there is a path from the agent position to one of the registered door
		if(!find) {
			var doors = pQueryAll("D", isDoor.on("D"));	
			for(var d: doors ) {
				//System.out.println("get current room by door");
				if (doorIsReachable(d)) {
					//System.out.println("door is reachable from the current room");	
					currentRoom = pQuery("R", and(inRoom.on("R", d), isRoom.on("R")));					
					break ;
				}
			}
		}
		

		
		//System.out.println("currentRoom end" + currentRoom);
		restoreDoorsBlockingState(originalDoorBlockingState, null);
		return currentRoom ;
	}
	
	/*.
	 * Given a door or button find closed doors such that for each D of these doors, if it is open would make 
	 * the entity reachable.
	 * This assumes the entity is not reachable. If it is, an empty list is returned.
	 */
	public List<String> getEnablingDoors(String entity) {
		
		List<String> enablingCandidates = new LinkedList<>() ;
		
		if (isReachable(entity)) {
			System.out.println("1");
			return enablingCandidates ;
		}
		
		List<String> candidates ;
		
		if (isDoor(entity)) {	
			// get doors guarding the neighboring room:
			candidates = pQueryAll("D",
					and(inRoom.on("R",entity),
						neighbor.on("R","R2"),
					    isDoor.on("D"),
						inRoom.on("R2","D")
						,
						not(inRoom.on("R","D")
							)
					)
				) ;
		}
		else {	
			// get the doors guarding the room  of the entity
			candidates =  pQueryAll("D",
					and(inRoom.on("R",entity),
						isDoor.on("D"),
						inRoom.on("R2","D"))
					) ;
		}
		
		
		var s  = getCurrentRoom();
		//System.out.println("get current room" + s);
		
		//get doors in the locked room
		var Doors = pQueryAll("D",
				and(isDoor.on("D"),inRoom.on(s,"D")));
		
		var buttonsConnected  = pQueryAll("B",
				and(isButton.on("B"),inRoom.on(s,"B")));
		
		var Connected  = pQueryAll("B",
				and(isButton.on("B"),inRoom.on(s,"B"), wiredTo.on(buttonsConnected,Doors)));
	//	
		for(String B : buttonsConnected) {
			var doors = pQueryAll("D", and(wiredTo.on(B,"D"), inRoom.on(s,"D")
					
					)) ;
			for(var D : doors) {
				System.out.println("Connected b" + D + B);
			}
		}
		System.out.println("x,y,w,z" + Doors.size() + buttonsConnected.size() );
		
		for(String D : Doors) {
			System.out.println("x" + D);
		}
		for(String D : buttonsConnected) {
			System.out.println("y" + D);
		}

		for(String D : Connected) {
			System.out.println("Connected" + D);
		}
		for(String D : candidates) {
			boolean isOpen = belief.isOpen(D) ;
			fakelyUnblockDoor(D) ;
			if(isReachable(entity)) {
				enablingCandidates.add(D) ;
			}
			restoreObstacleState(D, ! isOpen) ;
		}
		
		return enablingCandidates ;
	}
	
	/* get the buttons connected to the doors between two neighbor rooms*/
	public Map<String, List<String>> getEnablingButtons(String  blockedDoorId) {
		
		var currentRoom = getCurrentRoom();
		System.out.println("rooms in neighberhood ::: current Room and the blockedDoorID " + currentRoom + blockedDoorId);
		
	
		var neighborRooms1 =pQueryAll("R",								
						neighbor.on(currentRoom,"R")						
				   );
		System.out.println("neighbor of the corrent room" + neighborRooms1);

		
		var allRooms = pQueryAll("R",
						isRoom.on("R")						
				   );
		System.out.println("all rooms: " + allRooms);
		
		for (var roomx : allRooms) {
			System.out.println("doors in each room: " + roomx);
			List<String> bt0 = pQueryAll("D", and(inRoom.on(roomx, "D"), isDoor.on("D")));
			System.out.println("doors: " + bt0);
		}
		
		var alldoors = pQueryAll("D",
				and(
						isDoor.on("D")			
								
						)
				   );
		
		System.out.println("all doors registered" + alldoors);
		
		var blockedDoorRoom = pQuery("R",
				and(
						isRoom.on("R")	,
						inRoom.on("R" , blockedDoorId)		
						)
				   );
		
		System.out.println("room number of blocked door: " + blockedDoorRoom);
		
		
		
		
		var commenDoor = pQueryAll("D",
				and(
						isDoor.on("D")	,		
					    inRoom.on(currentRoom,"D"),
					    inRoom.on(blockedDoorRoom,"D")
						)
				   );
		
		System.out.println("doors between two rooms: current room and the blockedDoorRoom " + commenDoor);
		
		
		
		
		// this is the main part of this method
		// select the rooms between the current room and the room which the blocked door is located
		//this work only if there they are neighbors
		var neighborRooms = pQueryAll("R",
				and(
						isRoom.on("R")	,
						inRoom.on("R" , blockedDoorId)	,	
						neighbor.on("R",currentRoom)						
									
						)
				   );
		
		System.out.println("neighbors of the current room and the blocked door " + neighborRooms);
		

		Map<String, List<String>> buttonsconnectedToDoors = new HashMap<String, List<String>>() ;
		// iterate over all neighborhood rooms that are between current room and the room which the blocked door is located
//		for(var n: neighborRooms) {
//			var doors = pQueryAll("D", and( isDoor.on("D"), inRoom.on(currentRoom, "D"), inRoom.on(n, "D"))) ;
//			System.out.println("doors in between ::: " +  doors + n);
//			for(var d: doors) {
//				var buttonsconnected =	pQueryAll("B", and( isButton.on("B"), wiredTo.on("B",d), inRoom.on(currentRoom, "B"))) ;
//				System.out.println("buttons ::: " +  buttonsconnected + d);
//				if(!buttonsconnected.isEmpty())
//				buttonsconnectedToDoors.put(d, buttonsconnected); 
//			}
//		}
		
		List<String> finalPath = null;
		for(int i=0; i<allRooms.size(); i++) {
			var pathBetweenRooms1 = pQueryAll("S",
					pathBetweenRooms.on(currentRoom,blockedDoorRoom,"S",i)
					   );
			
			System.out.println("pathbetween to room" + pathBetweenRooms1);
			if(!pathBetweenRooms1.isEmpty()) { finalPath= pathBetweenRooms1;  break;}
		}
		
		System.out.println("final path between to room" + finalPath);
		
		if(!finalPath.isEmpty()) {			
			for(int i =0; i<finalPath.size(); i++) {
				var getPath  = finalPath.get(i);
				List<String> resultList = Arrays.asList(getPath.split(","));
				var door = resultList.get(1);
				var room = resultList.get(0).replace("[", "");
				System.out.println("room and door to scape: " + room +" , "+ door) ;
				var buttonsconnected =	pQueryAll("B", and( isButton.on("B"), wiredTo.on("B",resultList.get(1)), inRoom.on(resultList.get(0).replace("[", ""), "B"))) ;
				System.out.println("connected button" + buttonsconnected);
				if(!buttonsconnected.isEmpty()) {
					buttonsconnectedToDoors.put(door, buttonsconnected);
					break;
				}
			}
			 
		}
		
		return buttonsconnectedToDoors;
	}
	
	
	public List<String> getConnectedDoor(String buttonId) {
		
		var currentRoom = getCurrentRoom();
		System.out.println("current room" +  currentRoom);
		var corispondingDoor = pQueryAll("D",
				and(
						isDoor.on("D")	,		
					    inRoom.on(currentRoom,"D"),
					    wiredTo.on(buttonId,"D")
						)
				   );
		
		var neighborRooms2 = pQueryAll("R",
				and(
						isRoom.on("R")						
								
						)
				   );
		System.out.println("all rooms: " + neighborRooms2);
		
		
		for (var roomx : neighborRooms2) {
			System.out.println("doors in each room: " + roomx);
			List<String> bt0 = pQueryAll("D", and(inRoom.on(roomx, "D"), isDoor.on("D")));
			System.out.println("doors: " + bt0);
			
			System.out.println("buttons in each room: " + roomx);
			List<String> dt0 = pQueryAll("B", and(inRoom.on(roomx, "B"), isButton.on("B")));
			System.out.println("buttons: " + dt0);
		}
		
		System.out.println("get the corisponding door " + corispondingDoor + buttonId +  currentRoom);
		
		var alldoors = pQueryAll("D",
				and(
						isDoor.on("D")			
								
						)
				   );
		
		System.out.println("all doors registered" + alldoors);
		
		var alldoorsincurrentRoom = pQueryAll("D",
				and(
						isDoor.on("D"),			
						inRoom.on(currentRoom, "D")
						)
				   );
		
		System.out.println("all doors in current Room" + alldoorsincurrentRoom);
		
		
		var alldoorscunnected = pQueryAll("D",
				and(
						isDoor.on("D"),			
						wiredTo.on(buttonId,"D")
						)
				   );
		
		System.out.println("all doors cunnected" + alldoorscunnected);
		
		return corispondingDoor;
	}
	
	/* Get the connected button/buttons to a given door. if there is any. */
	public List<String> getConnectedButton(String doorId) {
		
		var currentRoom = getCurrentRoom();
		System.out.println("current room" +  currentRoom + doorId);
		// check is there any button connected to the given door at the same room
		
		List<String> buttonId = new LinkedList<>() ;
//		var corespondingButtonSameRoom = pQueryAll("B",
//				and(
//						isButton.on("B")	,		
//					    inRoom.on(currentRoom,"B"),
//					    wiredTo.on(doorId,"B")
//						)
//				   );
//		
//		if(!corespondingButtonSameRoom.isEmpty()) {			
//			for(int i = 0; i< corespondingButtonSameRoom.size(); i++ ) {
//				if (buttonIsReachable(corespondingButtonSameRoom.get(i))) {	
//					System.out.println("this button is connected to the current selected bloeck entity and it is reachable: " + corespondingButtonSameRoom.get(i));
//					buttonId.add(corespondingButtonSameRoom.get(i));					
//				}
//			}
//		}else {
		    // if there is no button at the same room, select button connected to the given door
			// if there is a button, check it is reachable from the agent position			
			var correspondingButton = pQueryAll("B", 
					and( isButton.on("B"), wiredTo.on("B",doorId))
					) ;
			System.out.println("correspondingButton " + correspondingButton);
			var alldoors = pQueryAll("D",
					and(
							isDoor.on("D")			
									
							)
					   );
			
			System.out.println("all doors registered" + alldoors);
			
			
			for(var d: alldoors) {
				var buttonsconnected =	pQueryAll("B", and( isButton.on("B"), wiredTo.on("B",d))) ;
				System.out.println("buttons ::: " +  buttonsconnected + d);		
			}
			
			if(correspondingButton.isEmpty()) return null;
			for(int i = 0; i< correspondingButton.size(); i++ ) {
				if (buttonIsReachable(correspondingButton.get(i))) {	
					System.out.println("this button is connected to the current selected bloeck entity and it is reachable: " + correspondingButton.get(i));
					buttonId.add(correspondingButton.get(i));
				}
			}
//		}
		
		return buttonId;
	}
	
	public boolean isReachable(String entityId) {
		if (isDoor(entityId)) {
			return doorIsReachable(entityId) ;
		}
		return buttonIsReachable(entityId) ;
	}
	public boolean isLockedInCurrentRoom() {
		String room = getCurrentRoom() ;
		if(room == null) return false ;
		var doors = pQueryAll("D", and(isDoor.on("D"), inRoom.on(room,"D"))) ;
		boolean anyOpen = doors.stream().anyMatch(D -> belief.isOpen(D)) ;
		return ! anyOpen ;
	}
	public boolean isDoor(String id) {
		LabEntity e = belief.worldmodel().getElement(id) ;
		return e.type.equals(LabEntity.DOOR) ;
	}
	
	public boolean isButton(String id) {
		LabEntity e = belief.worldmodel().getElement(id) ;
		return e.type.equals(LabEntity.SWITCH) ;
	}
}
