package gameTestingContest;

import static nl.uu.cs.aplib.agents.PrologReasoner.* ;

import java.util.List;
import java.util.stream.Collectors;

import alice.tuprolog.InvalidTheoryException;
import alice.tuprolog.MalformedGoalException;
import alice.tuprolog.NoSolutionException;
import alice.tuprolog.Prolog;
import alice.tuprolog.SolveInfo;
import alice.tuprolog.Term;
import world.BeliefStateExtended;

public class CobaProlog {
	
	static BeliefStateExtended belief = new BeliefStateExtended() ;
	
	/**
	 * Execute a prolog query, with var_ as the query variable. Returing a single solution,
	 * or null if there is none.
	 */
	static String pQuery(String var_, String q) {
		return belief.prolog().query(q).str_(var_) ;
	}
	
	/**
	 * Test if a prolog query q is successful (has some result).
	 */

	static List<String> pQueryAll(String var_, String q) {
		return belief.prolog()
			   . queryAll(q).stream()
			   . map(Q -> Q.str_(var_))
			   . collect(Collectors.toList()) ;
	}
	
	static public void main(String[] args) throws Exception {
				
		belief.attachProlog() ;
		var isRoom = predicate("room") ;
		var isButton = predicate("button") ;
		var isDoor = predicate("door") ;
		var inRoom = predicate("inRoom") ;
		var wiredTo = predicate("wiredTo") ;
		var notWiredTo = predicate("notWiredTo") ;
		var neighbor = predicate("neighbor") ;
		var initState = predicate("initState") ;

		
		var prolog = belief.prolog() ;
		prolog.facts(isRoom.on("r0")) ;
		prolog.facts(isRoom.on("r1")) ;
		prolog.facts(isRoom.on("r2")) ;

		prolog.facts(isRoom.on("r2a")) ;
		prolog.facts(isRoom.on("r3")) ;

		
		prolog.facts(isButton.on("button0")) ;
		prolog.facts(isButton.on("button1")) ;
		prolog.facts(isButton.on("button2")) ;
		prolog.facts(isButton.on("button3")) ;
		prolog.facts(isButton.on("button4")) ;
		prolog.facts(isDoor.on("door0")) ;
		prolog.facts(isDoor.on("door1")) ;
		prolog.facts(isDoor.on("door2")) ;
		prolog.facts(isDoor.on("door3")) ;
		prolog.facts(isDoor.on("door4")) ;
		
		prolog.facts(inRoom.on("r0","button0")) ;
		prolog.facts(inRoom.on("r0","button1")) ;
		prolog.facts(inRoom.on("r0","button3")) ;
		prolog.facts(inRoom.on("r0","door0")) ;
		prolog.facts(inRoom.on("r0","door1")) ;
		
		prolog.facts(inRoom.on("r1","button2")) ;
		prolog.facts(inRoom.on("r1","door0")) ;
		prolog.facts(inRoom.on("r1","door1")) ;
		prolog.facts(inRoom.on("r1","door2")) ;
		
		prolog.facts(inRoom.on("r2","door3")) ;
		prolog.facts(inRoom.on("r0","door3")) ;
		
		prolog.facts(inRoom.on("r2a","door2")) ;
		prolog.facts(inRoom.on("r2a","button4")) ;
		prolog.facts(inRoom.on("r2a","door4")) ;
		prolog.facts(inRoom.on("r3","door4")) ;
		
		// specifying the doors initial state: they are all closed:
		prolog.facts(initState.on("[[location,button0],[door0,0], [door1,0], [door2,0], [door3,0], [door4,0]]")) ;


		//prolog.facts(wiredTo.on("button1","door0")) ;
		prolog.facts(wiredTo.on("button0","door0")) ;
		prolog.facts(wiredTo.on("button0","door1")) ;
		prolog.facts(wiredTo.on("button2","door2")) ;
		prolog.facts(wiredTo.on("button4","door4")) ;
		
		//prolog.facts(notWiredTo.on("button0","door0")) ;
		
		
		
	    //	var neighbor = predicate("neighbor") ;
		
		Rule neigborRule = rule(neighbor.on("R1","R2"))
				.impBy(isRoom.on("R1"))
				.and(isRoom.on("R2"))
				.and("(R1 \\== R2)") // ok have to use this operator
				.and(isDoor.on("D"))
				.and(inRoom.on("R1","D"))
				.and(inRoom.on("R2","D"))
				;
		
		var roomReachable = predicate("roomReachable") ;
		var pathBetweenRooms = predicate("pathBetweenRooms") ;
		
		Rule roomReachableRule1 = rule(roomReachable.on("R1","R2","1"))
				.impBy(neighbor.on("R1","R2"))
				;
		
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
				.and(roomReachable.on("R1","R2","L"))
				;
		
		// Rules defining when sigma is a path between two rooms.
		// pathBetweenRoomsRule1(R1,R2,Sigma,K) true means that Sigma is a path between from the room
		// R1 to the room R2, and its "length" is 1. 
		// A path is a list of the form [r1,d1,r2,d2,r3, ... , rfinal] where each di is a door
		// between room ri and ri+1.  The length of such a path is here defined by the number 
		// of doors in the path.
		//
		// Note that this predicate does not reveal how each door in the path can be opened (if
		// it is closed)
		//
		Rule pathBetweenRoomsRule1 = rule(pathBetweenRooms.on("R1","R2","[R1,D,R2]","1"))
				.impBy(isDoor.on("D"))
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
		
		
		//prolog.add(neigborRule,roomReachableRule1,roomReachableRule2,roomReachableRule3) ;
		prolog.add(neigborRule,roomReachableRule1,roomReachableRule2) ;
		prolog.add(pathBetweenRoomsRule1,pathBetweenRoomsRule2) ;
		
		
		prolog.facts("opposite(1,0)") ;
		prolog.facts("opposite(0,1)") ;
		
		// Defining a predicate characterizing valid next state.
		// We will represent the state of this level by a sequence representing a mapping:
	    //  	
		//   [[location,Current-agent-location],
		//       [door0,State-of-door-0], 
		//       [door1,State-of-door-2], 
		//        ...
		//       [door4,State-of-door-4]]=
		//
		// If S1 and S2 are states, nextState(S1,S2) is true if S2 is a state that we would be by
		// if the agent make single transition from its current location. This can only be either
		// going to an adjacent node, or to toggle the button it is currently at (if it is on a
		// button).
		//
		var nextState = predicate("nextState") ;
		Rule planRule1 = rule(nextState.on("State","NextState"))
				.impBy("State   = [[location,B],[door0,Y0], [door1,Y1], [door2,Y2], [door3,Y3], [door4,Y4]]")
				.and("NextState = [[location,D],[door0,Z0], [door1,Z1], [door2,Z2], [door3,Z3], [door4,Z4]]")
				.and(isButton.on("B"))
				.and(isDoor.on("D"))
				.and(inRoom.on("R","B"))
				.and(inRoom.on("R","D"))
				.and("member([D,1],NextState)")
				.and(or(and(wiredTo.on("B","door0"),"opposite(Y0,Z0)"),
						"Y0 = Z0"))
				.and(or(and(wiredTo.on("B","door1"),"opposite(Y1,Z1)"),
						"Y1 = Z1"))
				.and(or(and(wiredTo.on("B","door2"),"opposite(Y2,Z2)"),
						"Y2 = Z2"))
				.and(or(and(wiredTo.on("B","door3"),"opposite(Y3,Z3)"),
						"Y3 = Z3"))
				.and(or(and(wiredTo.on("B","door4"),"opposite(Y4,Z4)"),
						"Y4 = Z4"))
				;
		
		Rule planRule2 = rule(nextState.on("State","NextState"))
				.impBy("State     = [[location,D],[door0,Y0], [door1,Y1], [door2,Y2], [door3,Y3], [door4,Y4]]")
				.and(  "NextState = [[location,B],[door0,Y0], [door1,Y1], [door2,Y2], [door3,Y3], [door4,Y4]]")
				.and(isDoor.on("D"))
				.and(isButton.on("B"))
				.and("member([D,1],State)")
				.and(inRoom.on("R","B"))
				.and(inRoom.on("R","D"))
				;
		
		Rule planRule3 = rule(nextState.on("State","NextState"))
				.impBy("State     = [[location,D],[door0,Y0], [door1,Y1], [door2,Y2], [door3,Y3], [door4,Y4]]")
				.and(  "NextState = [[location,E],[door0,Y0], [door1,Y1], [door2,Y2], [door3,Y3], [door4,Y4]]")
				.and(isDoor.on("D"))
				.and(isDoor.on("E"))
				.and(inRoom.on("R","D"))
				.and(inRoom.on("R","E"))
				.and("member([D,1],State)")
				.and("member([E,1],State)")
				;
		
		//
		// the predicate exec(Sigma,K)) is true if Sigma is a valid execution of the agent, with length K.
		// This is defined recursively as follows:
		//    (1) if s0 is the initial state, [s0] is a valid execution of length 0.
		//    (2) if [S1,...,s0] is an execution of length L, and nextState(S1,S2), then
		//        [S2,S1,...,s0] is an execution of length L+1.
		//
		//    Uhm... the execution list is ordered in reverse way :|  todo.
		//
		var exec = predicate("exec") ;
		Rule exec1 = rule(exec.on("State","[Start]","0"))
				.impBy(initState.on("State"))
				.and("member([location,Start],State)");
		
		Rule exec2 = rule(exec.on("State2","[Step | Z]","K"))
				.impBy("K > 0")
				.and("L is K-1")
				.and(nextState.on("State1","State2"))
				.and("member([location,Step],State2)")
				.and(exec.on("State1","Z","L"))
				;

		
		prolog.add(planRule1,planRule2,planRule3,exec1,exec2) ;
		
		System.out.println("Button in r0: " +  prolog.query(
				and(inRoom.on("r0","X"), 
					isButton.on("X")))
		        .str_("X")) ;
		
//		System.out.println("Button in r0 to open door1: " +  prolog.query(
//				and(wiredTo.on("B","door1"),
//					inRoom.on("r0","B")))
//		        .str_("B")) ;
		
		System.out.println("Button with no confirmed wiring or non-wiring to open door0: " 
		      +  prolog.queryAll(
		    		  and(isButton.on("B"), 
		    			  not(wiredTo.on("B","door0")),
		    			  not(notWiredTo.on("B","door0"))))
		      .stream().map(Q -> Q.str_("B"))
		      .collect(Collectors.toList())) ;
		
		System.out.println("Neighbors of r1: " +  
		        prolog.queryAll(
				neighbor.on("r1","R"))
		        .stream().map(Q -> Q.str_("R"))
		        .collect(Collectors.toList())
		) ;
		
		System.out.println("Reachable from r0: " +  
		        prolog.queryAll(
		        roomReachable.on("r0","R","1+1")
		        )
		        .stream().map(Q -> Q.str_("R"))
		        .collect(Collectors.toList())
		) ;
		
		
		String currentRoom = "r0";
		String GoalLocation = "door2";
		
		var neighborRooms1 =pQueryAll("R",
				and(						
						neighbor.on(currentRoom,"R")						
						)		
						
				   );
		System.out.println("neighborRooms1" + neighborRooms1);
		var Doors = pQueryAll("D",
				and(isDoor.on("D"),inRoom.on(currentRoom,"D")));
		

		System.out.println("Doors" + Doors);
		//neighbors of the current agent location that is also the room that goal is located
		var neighborRoom = 	pQueryAll("R",
				and(
						neighbor.on(currentRoom,"R")						
						,inRoom.on("R" , GoalLocation)
					//	, isDoor.on("D")
					//	, inRoom.on(currentRoom, "D")
					//	, inRoom.on(GoalLocation, "D")
						)
				   );
				
		var neighborRoom121 = 	pQueryAll("R",
				and(
						neighbor.on(currentRoom,"R")
						)
						);
		
		
		System.out.println("rooms in neighberhood ::: " +  
				neighborRoom + neighborRoom.size() + " jusr neigh " + neighborRoom121
				) ;	
		
		var doorsInBetween = pQueryAll("D", and( isDoor.on("D"), inRoom.on(currentRoom, "D"), inRoom.on(neighborRoom, "D")));
		
		System.out.println("doors between 2 rooms" + doorsInBetween);
		
		List<String> buttonsconnected = null;
		for(var n: neighborRoom) {
			var Doorss = pQueryAll("D", and( isDoor.on("D"), inRoom.on(currentRoom, "D"), inRoom.on(n, "D"))) ;
			System.out.println("doors in between ::: " +  Doorss +n);
			for(var d: Doorss) {
				 buttonsconnected =	pQueryAll("B", and( isButton.on("B"), wiredTo.on("B",d), inRoom.on(currentRoom, "B"))) ;
				System.out.println("buttons ::: " +  buttonsconnected +d);
			}
		}
		System.out.println("buttons add the end ::: " +  buttonsconnected );
		
		var corispondingDoor = pQueryAll("D",
				and(
						isDoor.on("D")	,		
					    inRoom.on(currentRoom,"D"),
					    wiredTo.on("button0","D")
						)
				   );
		
//		var corispondingDoor = prolog.query(
//				and(isDoor.on("D"), 
//						inRoom.on(currentRoom,"D"),
//						wiredTo.on("button0","D")))
//		        .str_("D");
//		
		System.out.println("get the corisponding door " + corispondingDoor);
		
	//	System.out.println(prolog.query(isButton.on("button10")).info.isSuccess()) ;
		
		System.out.println("Neighbors of r0:" + pQueryAll("R",neighbor.on("R","r0"))) ; 
		System.out.println("Neighbors of r1:" + pQueryAll("R",neighbor.on("R","r1"))) ; 
		System.out.println("Neighbors of r2:" + pQueryAll("R",neighbor.on("R","r2"))) ; 
		System.out.println("Neighbors of r2a:" + pQueryAll("R",neighbor.on("R","r2a"))) ; 
		System.out.println("Neighbors of r3:" + pQueryAll("R",neighbor.on("R","r3"))) ; 

		System.out.println("Room reachability from each other") ; 
		for(int k = 0; k<5; k++) {
			var reachableRooms = pQueryAll("R",roomReachable.on("R","r0",k));
			System.out.println("reachable(R,r0," + k + "): " + reachableRooms);
			var reachableRoomsX = pQueryAll("S",pathBetweenRooms.on("R","r0","S",k));
			System.out.println("path(R,r0,S," + k + "): " + reachableRoomsX);
		}
		
		System.out.println(">> pathBetweenRooms(r0,r3,Path,3)" + pQuery("Path",pathBetweenRooms.on("r0","r3","Path",3))) ;
		//System.out.println(">>>" + pQuery("Plan","exec(State,Plan,0)"))  ;

		System.out.println(">>>" 
		    + pQuery("Plan",
		    		and("reverse(RevPlan)",
		    			"exec(State,RevPlan,5)", 
		    		    "member([location,Z],State)", 
		    		    isDoor.on("Z"), 
		    		    inRoom.on("r3","Z"))
		    		))  ; 

	}
}
