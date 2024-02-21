package world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.text.html.parser.Entity;

import alice.tuprolog.InvalidTheoryException;
import environments.LabRecruitsEnvironment;
import eu.iv4xr.framework.extensions.pathfinding.SurfaceNavGraph;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import eu.iv4xr.framework.spatial.Vec3;
import gameTestingContest.DebugUtil;
import gameTestingContest.Prolog;
import gameTestingContest.TestingTaskStack;
import nl.uu.cs.aplib.mainConcepts.Environment;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.utils.Pair;
import world.HighLevelGraph;


public class BeliefStateExtended extends BeliefState {
	
	public HighLevelGraph highLevelGragh;
	public Prolog prolog; 

	public BeliefStateExtended () {
		super() ;
		highLevelGragh = new HighLevelGraph();
		prolog  = new Prolog(this);
		//this.attachProlog() ;
	}
	
	public HashMap<String,GoalStructure> goalsmap = new HashMap<String,GoalStructure>() ;
	
	public Map<String, List<String>>  buttonDoorConnection = new HashMap<String, List<String>>() ;
	
	/**
	 * Return entities in the same time stamp of the current worm time stamp.
	 */
	public List<WorldEntity> newlyObservedEntities() {
		return knownEntities().stream().filter(e -> e.timestamp == worldmodel.timestamp
				&& (e.type.equals(LabEntity.DOOR) || (e.type.equals(LabEntity.SWITCH)))).collect(Collectors.toList());
	}

	/**
	 * Merge the newly observed entities and their connections in the high-level graph
	 * If all newly observed entities are already in the graph, return null.
	 * Add connection between entities.
	 * @return 
	 */
	public boolean mergeNewlyObservedEntities(List<WorldEntity> entities) {
		final boolean[] newEntity = {false}; 
		boolean[] currentEntity = {false};
		System.out.println("Merge newly observed entities in the high-level graph");
		entities.forEach(e -> {			
			// if the list of entities is empty(the first observation) 
			//or the entity does not exist in the graph add it to the graph
			if(highLevelGragh.entities.isEmpty() || (highLevelGragh.getIndexById(e.id) == -1)){					
				if ( e.type.equals(LabEntity.DOOR) || e.type.equals(LabEntity.SWITCH)){
					
					System.out.println("new entitiy add " +  e.id);		
					newEntity[0] = true;
					highLevelGragh.addEntites(e);
					highLevelGragh.addVertices(e);
					try {
						registerFoundGameObjects(e);
					} catch (InvalidTheoryException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
			        }
							
			}
			/* By default the entity blocking state is false means, if we see a door we
			  need to change the state to true */
			// TODO: if it is necessary to have a list of non-blocked entities, complete this part
			//if ( e.type.equals(LabEntity.DOOR)) { 
				/* unless it is a door, the obstacle is always blocking: */
			//	  highLevelGragh.addObstacleInBlockingState((LabEntity) e); 
		    //}else {
			//      highLevelGragh.addObstacle((LabEntity) e); 
		   // }	 
		});
		
		if(!newEntity[0]) return false;
		
		//add connection between two entities
		System.out.println("Add connection between two entities" + entities.size());
		if(entities.size() > 1) {
			System.out.println("add connection between two entities: if there are more than one");
			highLevelGragh.addEdgs(entities);
		}	
		
		/*
		 * if in the current position of an agent, there is no nearest entities and the
		 * agent has to explore the environment, then maybe between the current node and
		 * the newly observe entity there would be a long distance. which means the connection
		 * between the current node and newly observed entity has broken. we add this
		 * connection here.
		 */	
		System.out.println("lets check the entity id" + highLevelGragh.currentSelectedEntity);
		
		if(highLevelGragh.currentSelectedEntity != null) {
			
			if(highLevelGragh.currentSelectedEntity == 100) {
				WorldEntity entityIsSeen = highLevelGragh.entities.stream().filter(e-> e.id.equals(highLevelGragh.currentSelectedEntityId)).findAny().orElse(null);
				
				if(entityIsSeen != null) {
					System.out.println("entityIsSeen: " + entityIsSeen.id + highLevelGragh.getIndexById(entityIsSeen.id) );
					highLevelGragh.currentSelectedEntity = highLevelGragh.getIndexById(entityIsSeen.id);
					highLevelGragh.visitedNodes.set(highLevelGragh.visitedNodes.indexOf(100), highLevelGragh.getIndexById(entityIsSeen.id));
				
				}
					
			}
			
			if(highLevelGragh.currentSelectedEntity != 100) {
				//System.out.println("equals" + entities.equals(highLevelGragh.entities.get(highLevelGragh.currentSelectedEntity)));
				entities.forEach(e->{
					
					if(e.id.equals(highLevelGragh.entities.get(highLevelGragh.currentSelectedEntity).id)) {
						currentEntity[0] = true;
					}
				});
				if(!currentEntity[0]) {
					System.out.println("add connection between two entities: if there is a distance between them");
					entities.add(highLevelGragh.entities.get(highLevelGragh.currentSelectedEntity));
					highLevelGragh.addEdgs(entities);
				}
			}
		}
		
		//Print all entities in the high level graph
		highLevelGragh.entities.forEach(e->System.out.println("All entities in the high level graph: " + " id " + e.id + highLevelGragh.getIndexById(e.id) ));
		System.out.println("Adges in the high level graph : " + highLevelGragh.edges.toString());			
		
		// get neighbors information
		/*
		 * if(entities.size() >= 2) {
		 * highLevelGragh.neighbours(highLevelGragh.getIndexById("button1"));
		 * System.out.println("neighbors: " + highLevelGragh.neighbours(highLevelGragh.getIndexById("button1"))); var p = highLevelGragh.heuristic(highLevelGragh.getIndexById("button1"),
		 * highLevelGragh.getIndexById("button2")); System.out.println("heuristic : " +
		 * "from : "+ highLevelGragh.getIndexById("button1") + "to: "+
		 * highLevelGragh.getIndexById("button2") + " " + p); }
		 */
	
		
		/*update stack*/  //TODO
		//TestingTaskStack.addItems(entities);
    	
		
		return true;
	}
	
	@Override
	public boolean isStuck() {
	
    	int N = recentPositions.size() ;
    	if(N<3) return false ;
    	Vec3 p0 = recentPositions.get(N-3) ;
    //	System.out.println("====== checking stcuk" + N + p0 +  recentPositions.get(N-2) + recentPositions.get(N-1)
    //			) ;
    	
    	return Vec3.dist(p0,recentPositions.get(N-2)) <= 0.02
    		   &&  Vec3.dist(p0,recentPositions.get(N-1)) <= 0.02 ;
         //    && p0.distance(q) > 1.0 ;   should first translate p0 to the on-floor coordinate!
	}
	
	@Override
	public void updateState(String id) {
		super.updateState(worldmodel.agentId);
		for(var e : worldmodel.elements.values()) {
		//	System.out.println("update State" + e.id);
			if ( e.type.equals(LabEntity.DOOR) || e.type.equals(LabEntity.SWITCH)){
				if(e.timestamp == worldmodel.timestamp) {
					var sqdist = Vec3.distSq(worldmodel.position,e.position) ;
			//		System.out.println("update State befor if" + e.id + sqdist);
					if(sqdist <= 4) {
				//		try {
				//			System.out.println("update State if" + e.id);
						//	registerFoundGameObjects(e);
							mergeNewlyObservedEntities(newlyObservedEntities());
				//	} catch (InvalidTheoryException e1) {
							// TODO Auto-generated catch block
				//			e1.printStackTrace();
				//		}
					}
				}
			}
		}
	}
    
	/**
	 * Register all buttons and doors currently in the agent's belief to the models
	 * of rooms and connections that it keeps track.
	 * @throws InvalidTheoryException 
	 */
	public void registerFoundGameObjects(WorldEntity e) throws InvalidTheoryException {
		
		if(e.type.equals(LabEntity.SWITCH)) {
			//System.out.println("belief state, register a button" + e.id);
			prolog.registerButton(e.id);
		}
		if(e.type.equals(LabEntity.DOOR)) {
		//	System.out.println("belief state, register a door");
			prolog.registerDoor(e.id);
		}
	}
	
	
	
	/**
	 * This finding path is the same as the original findPAthTo. The difference is that 
	 * we call enhanceFindPath in the class pathfinder. Which means all combination of 
	 * vertices around two position is checked.
	 * */
	private Vec3 memorizedGoalLocation;
    public Pair<Vec3,List<Vec3>> expensiveFindPathTo(Vec3 q, boolean forceIt) {
    	if (!forceIt && memorizedGoalLocation!=null) {
    		if (Vec3.dist(q,memorizedGoalLocation) <= DIST_TO_MEMORIZED_GOALLOCATION_SOFT_REPATH_THRESHOLD)
    			return new Pair(q,null) ;
    	}
    	// else we invoke the pathfinder to calculate a path:
    	// be careful with the threshold 0.05..
    	var abstractpath = pathfinder().enhancedFindPath(worldmodel().getFloorPosition(),q,BeliefState.DIST_TO_FACE_THRESHOLD) ;
    	if (abstractpath == null) return null ;
    	List<Vec3> path = abstractpath.stream().map(v -> pathfinder().vertices.get(v)).collect(Collectors.toList()) ;
    	// add the destination path too:
    	path.add(q) ;
    	return new Pair(q,path) ;
    }
    
    /**
     * This method checks if there is an enabler that is not in the requested agents belief 
     */
    
    public boolean checkEnablers(List<String> enablers) {
    	List<String> allButtons = new ArrayList<>();
    	if(!highLevelGragh.entities.isEmpty()) {
    		highLevelGragh.entities.forEach(button -> {
				if (button.id.contains("button")) {						
						allButtons.add(button.id);
					}
			});
    		System.out.println("number of buttons in two agents:" + allButtons.size() + enablers.size());
    		if(allButtons.size() > enablers.size()) return true;
    	}
    	return false;
    }
    
   /*
     * This function compare two agents belief and checks the ask variable 
     */
    
    public List<String> domiNode(List<String> enablers) {
    	//do you have a button that I do not have?
    	// compare the list of buttons first -> list of buttons should be sent
    	//invoke compare to 
    	//yes I do
    	// return the button and how to reach it
    	// how to reach it depends on where the agent is. 
    	//how it is going to send this? if I have A1 position as well it might be able to find a route
    	
    	List<String> allButtons = new ArrayList<>();   	
    		highLevelGragh.entities.forEach(button -> {
				if (button.id.contains("button")) {						
						allButtons.add(button.id);
					}
			});
    		List<String> elementsOnlyInallButtons = new ArrayList<>(allButtons);
    		elementsOnlyInallButtons.removeAll(enablers);
    		return elementsOnlyInallButtons;
    }
}
