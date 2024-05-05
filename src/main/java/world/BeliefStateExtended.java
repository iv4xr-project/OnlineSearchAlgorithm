package world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import agents.tactics.OnlineSearch;
import alice.tuprolog.InvalidTheoryException;
import environments.LabRecruitsEnvironment;
import eu.iv4xr.framework.extensions.pathfinding.SurfaceNavGraph;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import eu.iv4xr.framework.spatial.Vec3;
import gameTestingContest.DebugUtil;
import gameTestingContest.Prolog;
import nl.uu.cs.aplib.mainConcepts.Environment;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.utils.Pair;
import world.HighLevelGraph;


public class BeliefStateExtended extends BeliefState {
	
	public HighLevelGraph highLevelGragh;
	public Prolog prolog; 
	WorldEntity lastInteractedButton ;

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
			highLevelGragh.addEdges(entities);
		}	
		
		/*
		 * if in the current position of an agent, there is no nearest entities and the
		 * agent has to explore the environment, then maybe between the current node and
		 * the newly observe entity there would be a long distance. which means the connection
		 * between the current node and newly observed entity has broken. we add this
		 * connection here.
		 */	
		
		if(highLevelGragh.currentSelectedEntity != null) {
			//System.out.println("equals" + entities.equals(highLevelGragh.entities.get(highLevelGragh.currentSelectedEntity)));
			entities.forEach(e->{
				if(e.id.equals(highLevelGragh.entities.get(highLevelGragh.currentSelectedEntity).id)) {
					currentEntity[0] = true;
				}
			});
			if(!currentEntity[0]) {
				System.out.println("add connection between two entities: if there is a distance between them");
				entities.add(highLevelGragh.entities.get(highLevelGragh.currentSelectedEntity));
				highLevelGragh.addEdges(entities);
			}
		}
		
		//Print all entities in the high level graph
		highLevelGragh.entities.forEach(e->System.out.println("All entities in the high level graph: " + " id " + e.id ));
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
		// invoke the model-inferencer here:
		this.lastInteractedButton = OnlineSearch.trackModel(this, this.lastInteractedButton) ;
	}
    
	
	/**
	 * This finding path is the same as the original findPathTo. The difference is that 
	 * we call enhanceFindPath in the class pathfinder. Which means all combination of 
	 * vertices around two position is checked.
	 * */
	private Vec3 memorizedGoalLocation;
    public Pair<Vec3,List<Vec3>> enhancedFindPathTo(Vec3 q, boolean forceIt) {
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
}
