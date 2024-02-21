package world;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import eu.iv4xr.framework.extensions.pathfinding.Navigatable;
import eu.iv4xr.framework.extensions.pathfinding.SimpleNavGraph;
import eu.iv4xr.framework.mainConcepts.WorldEntity;
import eu.iv4xr.framework.spatial.LineIntersectable;
import eu.iv4xr.framework.spatial.Obstacle;
import eu.iv4xr.framework.spatial.Vec3;
import eu.iv4xr.framework.spatial.meshes.Edge;
import eu.iv4xr.framework.spatial.meshes.EdgeMap;
import eu.iv4xr.framework.spatial.meshes.Face;

public class HighLevelGraph implements Navigatable {

	/**
	 * The set of in-game entities represented by their name. So, if i =
	 * entities("entityName"), then i is the integer id of the entity, and
	 * entityName is the name of the entity.
	 */
	public ArrayList<WorldEntity> entities;

	/**
	 * The set of vertices that form the navigation graph. The index of the vertex is its
	 * id. So if v = vertices(i), then i is its id, and vertices(i) actually returns the
	 * 3D position of the vertex.
	 */
	public ArrayList<Vec3> vertices;
		
	/**
	 * Describe the connectivity (edges) between the vertices.
	 */
    public EdgeMap edges;
    
	/** Current node which the agent is there*/
    public Integer currentSelectedEntity;
    
    /** Current node*/
    public String currentSelectedEntityId;
    
    
	/** List of Visited nodes */
    public ArrayList<Integer> visitedNodes; 
    		
   
    /** Current node which the agent is there*/
    public String currentBlockedEntity;
    
    /** Approximate position of the target*/
    public Vec3 goalPosition;
    
	public HighLevelGraph() {
		this.entities = new ArrayList<WorldEntity>();
		this.vertices  = new ArrayList<Vec3>();
		this.edges = new EdgeMap();	
		this.visitedNodes = new ArrayList<Integer>();
	}

	/**
	 * Add the new entity to this high-level graph. It can be set of non-blocking or
	 * blocking entities.
	 */
	public void addEntites(WorldEntity e) {
		entities.add(e);
		
	}

	/** Get index of an entity in the array list given it is id, it is because we consider this index
	 *  as a unique id represent that entity and put this index in the edges
	 *  */
    public int getIndexById(String e) {
    	for (int i = 0; i < entities.size(); i++) {
            if (entities !=null && entities.get(i).id.equals(e)) {
                return i;
            }
        }
        return -1;
    }
    
	/**
	 * Add vertices of entities to this high-level graph.
	 */
	public void addVertices(WorldEntity e) {
		vertices.add(e.position);
	}
	
	/**
	 * Add edges between newly observed entities and existing entities
	 */
	public void addEdgs(List<WorldEntity> entities) {
		
    	// Connect entities to each other
        for (int i = 0; i < entities.size(); i++) {
            for (int j = 0; j < entities.size() ; j++) {
            	if (i == j) {continue;}
            	
            	//print entity information
            	//System.out.println("firstEntity " + entities.get(i).id +" index :"+ getIndexById(entities.get(i).id) + ": "+ i);
            	//System.out.println("secondEntity " + entities.get((j)).id +" index :" + getIndexById(entities.get(j).id )+ ": "+ j);
            	
            	int firstEntity = getIndexById(entities.get(i).id);
            	int secondEntity = getIndexById(entities.get(j).id);
            	
				/* we ignore a loop in each entity*/
            	
				/* if the first entity has some neighbors, check the other entities was not connected before.
				 * if it is a new neighbor, add it to the edge.
				 * if there is no neighbor, add a new neighbors.
				 */
            	if(!this.edges.neighbours(firstEntity).isEmpty()) {	
            		if(!this.edges.neighbours(firstEntity).contains(secondEntity))
            			edges.put(new Edge(firstEntity, secondEntity));
            		
            		}
            	
            	edges.put(new Edge(firstEntity, secondEntity));
            	//System.out.println("added edges " + this.edges.toString());
            }
        }  	
    }

	
	/**
	 * Get the list of entities with the connected cedges by ids
	 * @return 
	 * */
	
	public ArrayList<ArrayList<String>> getEntityConnections() {
		ArrayList<ArrayList<String>> model = new ArrayList<>();
		for(int i=0;i<entities.size(); i++) {
			ArrayList<String> temt = new ArrayList<>();
			temt.add(entities.get(i).id);
			edges.get(getIndexById(entities.get(i).id)).forEach(e->{
				temt.add(entities.get(e).id);
			});			
			
			
			model.add(temt);
		}
		return model;
	}
	
	
	public Iterable<Integer> neighboursNew(int id) {
		ArrayList<Integer> neighbors = new ArrayList<Integer>();
		
		return edges.neighbours(id);				
	}

	/**
     * Heuristic distance between any two vertices. Here it is chosen to be the geometric
     * distance between them.
     */
	public float heuristic(int from, int to) {
        return Vec3.dist(vertices.get(from), vertices.get(to));
	}

	/**
     * The distance between two NEIGHBORING vertices. If the connection is not blocked,
     * it is deined to be their geometric distance, and else +inf.
     */
	public float distance(int from, int to) {
		Vec3 a = entities.get(from).position;
        Vec3 b = entities.get(to).position;
		
        return Vec3.dist(a, b);
	}


	public float heuristic(Object from, Object to) {
		// TODO Auto-generated method stub
		return 0;
	}

	public float distance(Object from, Object to) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Iterable neighbours(Object id) {
		// TODO Auto-generated method stub
		return null;
	}

}
