package world;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import alice.tuprolog.InvalidTheoryException;
import environments.LabRecruitsEnvironment;
import eu.iv4xr.framework.extensions.pathfinding.SurfaceNavGraph;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import eu.iv4xr.framework.spatial.Vec3;
import gameTestingContest.DebugUtil;
import gameTestingContest.Prolog;
import nl.uu.cs.aplib.mainConcepts.Environment;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import world.HighLevelGraph;


public class BeliefStateExtended extends BeliefState {
	
	public HighLevelGraph highLevelGragh;
	public Prolog prolog; 

	public BeliefStateExtended () {
		super() ;
		highLevelGragh = new HighLevelGraph();
		prolog  = new Prolog(this);
		this.attachProlog() ;
	}
	
	public HashMap<String,GoalStructure> goalsmap = new HashMap<String,GoalStructure>() ;
	
	/**
	 * Return entities in the same time stamp of the current worm time stamp.
	 */
	public List<WorldEntity> newlyObservedEntities() {
		return knownEntities().stream().filter(e -> e.timestamp == worldmodel.timestamp).collect(Collectors.toList());
	}

	/**
	 * Merge the newly observed entities and their connections in the high-level
	 * graph
	 * @return 
	 */
	public boolean mergeNewlyObservedEntities(List<WorldEntity> entities) {
		final boolean[] newEntity = {false}; 
		boolean[] currentEntity = {false};
		System.out.println("merge Newly Observed Entities ");
		entities.forEach(e -> {
			//System.out.println("if " +  e.id);
			// if the list of entities is empty or the entity is not exist in the list add it to the list
			if(highLevelGragh.entities.isEmpty() || (highLevelGragh.getIndexById(e.id) == -1)){
				System.out.println("new entitiy add " +  e.id);
				newEntity[0] = true;
				highLevelGragh.addEntites(e);
				highLevelGragh.addVertices(e);
			}
			/* By default the entity blocking state is false means, if we see a door we
			  need to change the state to true */
			// TODO: if it is necessary to have a list of non-blocked entities, complete this part
//			if ( e.type.equals(LabEntity.DOOR)) { 
//				/* unless it is a door, the obstacle is always blocking: */
//				  highLevelGragh.addObstacleInBlockingState((LabEntity) e); 
//		    }else {
//			      highLevelGragh.addObstacle((LabEntity) e); 
//		    }	 
		});
		
		if(!newEntity[0]) return false;
		
		//add connection between two entities
		System.out.println("add connection between two entities" + entities.size());
		if(entities.size() > 1) {highLevelGragh.addEdgs(entities);
		System.out.println("add connection between two entities: if there are more than one");
		}	
		
		/*
		 * if in the current position of an agent, there is no nearest entities and the
		 * agent has to explore the environment, then maybe between the current node and
		 * the newly observe entity be a long distance. which means the connection
		 * between the current node and newly observed entity has broken. we add this
		 * connection here.
		 */		
		if(highLevelGragh.currentSelectedEntity != null) System.out.println("equals" + entities.equals(highLevelGragh.entities.get(highLevelGragh.currentSelectedEntity)));
		//if(highLevelGragh.currentSelectedEntity != null && !(entities.contains(highLevelGragh.entities.get(highLevelGragh.currentSelectedEntity)))) {
		if(highLevelGragh.currentSelectedEntity != null) {			
			entities.forEach(e->{
				if(e.id.equals(highLevelGragh.entities.get(highLevelGragh.currentSelectedEntity).id)) {
					System.out.println("add connection between two entities: if there is a distance");
					currentEntity[0] = true;
				}
			});
			if(!currentEntity[0]) {
				System.out.println("add connection between two entities: if there is a distance if");
				entities.add(highLevelGragh.entities.get(highLevelGragh.currentSelectedEntity));
				highLevelGragh.addEdgs(entities);
			}
		}
		//System.out.println("size: " + highLevelGragh.entities.size() + highLevelGragh.entities.get(0).id);

		
		highLevelGragh.entities.forEach(e->System.out.println("entities: " + e.id ));
		System.out.println("agdes: " + highLevelGragh.edges.toString());			
		if(entities.size() >= 2) {	
//			highLevelGragh.neighbours(highLevelGragh.getIndexById("button1"));
			System.out.println("neighborsss?????: " + highLevelGragh.neighbours(highLevelGragh.getIndexById("button1")));
//			var  p = highLevelGragh.heuristic(highLevelGragh.getIndexById("button1"), highLevelGragh.getIndexById("button2"));
//			System.out.println("heuristic : " + "from : "+ highLevelGragh.getIndexById("button1")  + "to: "+ highLevelGragh.getIndexById("button2") + " " +  p);
		}	
	return true;
	}
	
	@Override
	public boolean isStuck() {
	
    	int N = recentPositions.size() ;
    	if(N<3) return false ;
    	Vec3 p0 = recentPositions.get(N-3) ;
    	System.out.println("====== checking stcuk" + N + p0 +  recentPositions.get(N-2) + recentPositions.get(N-1)
    			) ;
    	
    	return Vec3.dist(p0,recentPositions.get(N-2)) <= 0.02
    		   &&  Vec3.dist(p0,recentPositions.get(N-1)) <= 0.02 ;
         //    && p0.distance(q) > 1.0 ;   should first translate p0 to the on-floor coordinate!
	}
	
    @Override
    public void updateState() {
        super.updateState();
        var observation = this.env().observe(id) ;            
        
        //update prolog
        try {
			registerFoundGameObjects();
		} catch (InvalidTheoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	/**
    	 * To keep track which button the agent toggled last.
    	 */
    	// FRAGILE!
    	WorldEntity lastInteractedButton = null;
		// check if a button is just interacted:
//		for(WorldEntity e: changedEntities) {
//			if(e.type.equals("Switch") && e.hasPreviousState()) {
//				DebugUtil.log(">> detecting interaction with " + e.id) ;
//				lastInteractedButton = e ;					
//			}
//		}
//		// check doors that change state, and add connections to lastInteractedButton:
//		if(lastInteractedButton != null) {
//			for(WorldEntity e: changedEntities) {
//				var di = e.id;
//				if(e.type.equals("Door") && e.hasPreviousState()) {
//					try {
//						prolog.registerConnection(lastInteractedButton.id,e.id);
//					} catch (InvalidTheoryException e1) {
//						// TODO Auto-generated catch block
//						e1.printStackTrace();
//					}
//				}	
//			}
//		}
        
        
        // updating recent positions tracking (to detect stuck) here, rater than in mergeNewObservationIntoWOM,
        // because the latter is also invoked directly by some tactic (Interact) that do not/should not update
        // positions
        recentPositions.add(new Vec3(worldmodel.position.x, worldmodel.position.y, worldmodel.position.z)) ;
        if (recentPositions.size()>4) recentPositions.remove(0) ;
    }
    
	/**
	 * Register all buttons and doors currently in the agent's belief to the models
	 * of rooms and connections that it keeps track.
	 * @throws InvalidTheoryException 
	 */
	private void registerFoundGameObjects() throws InvalidTheoryException {
	
		if(this.knownButtons().size() > 0) {			
		for(WorldEntity e : this.knownButtons()) {			
			prolog.registerButton(e.id);
		}}
		//agent.getState(). why????
		if(this.knownDoors() != null) {
		for(WorldEntity e : this.knownDoors()) {
			prolog.registerDoor(e.id);
		}}
	}
}
