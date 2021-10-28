package gameTestingContest;

import static nl.uu.cs.aplib.agents.PrologReasoner.* ;

import java.util.List;
import java.util.stream.Collectors;

import alice.tuprolog.InvalidTheoryException;
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
	String pQuery(String var_, String q) {
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
	
	static public void main(String[] args) throws InvalidTheoryException {
				
		belief.attachProlog() ;
		var isRoom = predicate("room") ;
		var isButton = predicate("button") ;
		var isDoor = predicate("door") ;
		var inRoom = predicate("inRoom") ;
		var wiredTo = predicate("wiredTo") ;
		var notWiredTo = predicate("notWiredTo") ;
		var neighbor = predicate("neighbor") ;
		
		var prolog = belief.prolog() ;
		prolog.facts(isRoom.on("r0")) ;
		prolog.facts(isRoom.on("r1")) ;
		prolog.facts(isRoom.on("r2")) ;
		prolog.facts(isButton.on("button0")) ;
		prolog.facts(isButton.on("button1")) ;
		prolog.facts(isButton.on("button2")) ;
		prolog.facts(isButton.on("button3")) ;
		prolog.facts(isDoor.on("door0")) ;
		prolog.facts(isDoor.on("door1")) ;
		prolog.facts(isDoor.on("door2")) ;
		prolog.facts(isDoor.on("door3")) ;
		
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

		//prolog.facts(wiredTo.on("button1","door0")) ;
		prolog.facts(wiredTo.on("button0","door0")) ;
		prolog.facts(wiredTo.on("button0","door1")) ;
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
		
		Rule roomReachableRule1 = rule(roomReachable.on("R1","R2","1"))
				.impBy(neighbor.on("R1","R2"))
				;
		
		Rule roomReachableRule2 = rule(roomReachable.on("R1","R2","K+1"))
				.impBy(neighbor.on("R1","R"))
				.and(roomReachable.on("R","R2","K"))
				.and("(R1 \\== R2)")
				.and("K > 0")
				;
		Rule roomReachableRule3 = rule(roomReachable.on("R1","R2","K+1"))
				.impBy(roomReachable.on("R1","R2","K"))
				.and("K > 0")
				;
		
		prolog.add(neigborRule,roomReachableRule1,roomReachableRule2,roomReachableRule3) ;
		
		
		
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
		
	}
	
	

}
