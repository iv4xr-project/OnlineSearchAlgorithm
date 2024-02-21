package gameTestingContest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import eu.iv4xr.framework.mainConcepts.WorldEntity;
import eu.iv4xr.framework.spatial.Vec3;

public class TestingTaskStack {

	private int weight;
	private String itemId;
	private boolean status; //if this task is taken by another agent
	private ArrayList<String> testedBy = new ArrayList<>();
	private String seenByAgent ;
	private int triedNumber ;
	private Vec3 position;
	public List<TestingTaskStack> tasksList = Collections.synchronizedList(new ArrayList<>());
	private List<String> luckedItems = Collections.synchronizedList(new ArrayList<>());
	private ArrayList<Object> doneTasks = new ArrayList<>();
	private List<List<String>> doneTasks2 = Collections.synchronizedList(new ArrayList<>());
	//public List<String> goals= new ArrayList<String>(List.of("doorKey1", "doorKey2" , "doorKey3" ,"doorKey4")); //IMPORTANT DOOR TO TEST
	//public List<String> goals= new ArrayList<String>(List.of("door12", "door8" ,"door5")); 
	public List<String> goals= new ArrayList<String>(List.of("door2" , "door3" , "door6", "door9" ));
	//public List<String> goals= new ArrayList<String>(List.of("" ));
	public List<String> goalsTem= new ArrayList<String>(List.of("door2" , "door3" , "door6", "door9"));
	
	
	public TestingTaskStack() {}
	public TestingTaskStack(String itemId, boolean status, Vec3 position, String agent, String observerAgent) {		
		this.itemId = itemId;
		this.status = status;
		this.testedBy.add(agent);
		this.seenByAgent = observerAgent;
		this.position = position;
		this.triedNumber  = this.triedNumber+1;
		if(this.goals.contains(itemId)) {
			this.weight = 10;
			}else {
		 this.weight = 5;}
		
	}
	
	public int setWeight(String itemId) {
		if(this.goals.contains(itemId)) {
			this.weight = 10;
			}else {
		 this.weight = 5;}
		 return this.weight;
	}
	
	public boolean setStatus(boolean status) {
		return this.status = status;
	}
	 public String getitemId() {
	        return itemId;
	    }
	 public int getweight() {
	        return weight;
	    }
	 public boolean getstatus() {
	        return status;
	    }
	 public Vec3 getposition() {
	        return position;
	    }
	 public ArrayList<String> gettestedBy() {
	        return testedBy;
	    }
	 
	 public int gettriedNumber() {
	        return this.triedNumber;
	    }
	 public ArrayList<Object> getdoneTasks() {
	        return doneTasks;
	    } 
	 
	 public List<List<String>>  getdoneTasks2(){
		 return doneTasks2;
	 }
	 
	 
	 public String getseenByAgent() {
			 return seenByAgent;
		 }
 
	 public ArrayList<String> settestedBy(String agentID) {
	        this.testedBy.add(agentID);
			return testedBy;
	 }
	 
	 public ArrayList<Object> setdoneTasks(String task, String agentId) {
		 	this.doneTasks.add(new Object[] {task,agentId});
		 	return doneTasks;
	    }
	 
	 public List<List<String>> setdoneTasks2(List<String> item) {
		 	this.doneTasks2.add(item);
		 	return doneTasks2;
	    }
	 
	 public boolean setluckedItems(String item){
		 return this.luckedItems.add(item);
	 }
	 
	 public int settriedItem(){
		 return this.triedNumber  = this.triedNumber+1 ;
	 }
	 
	 public List<String> getluckedItems(){
		 return this.luckedItems;
	 }
	 
	 public boolean checkLuckedItems(String item) {
		 return this.luckedItems.stream().anyMatch(e-> e.equals(item));
	 }
	 
	 public void removeLuckedItems(String item) {
		  System.out.println(">>> Remove the item from the lucked list" + item);
		  this.luckedItems.forEach(e-> System.out.println(">>> lucked item" + e));
		  this.luckedItems.removeIf(e -> e.equals(item));
		  this.luckedItems.forEach(e-> System.out.println(">>> lucked item after remove" + e));
	 }
	 
	 public boolean itemExcist(String itemId) {
		 for(TestingTaskStack tasks: tasksList) {
			 if(tasks.itemId.equals(itemId)) return true;	
		 }
		 return false;
	 }
	 
	 public TestingTaskStack itemByID(String itemId) {
		 for(TestingTaskStack tasks: tasksList) {
			 if(tasks.itemId.equals(itemId)) return tasks;	
		 }
		 return null;
	 }
	 
	 /***
	  * check the done list to see if the item is tested before
	  * @param itemId
	  * @return
	  */
	 public boolean itemTested(String itemId) {		
		 for(Object tasks: doneTasks) {			 
			 if(tasks.toString().equals(itemId)) return true;	
		 }
		 return false;
	 }
	 
	 
	 public boolean itemTested2(String itemId) {		
		  for (List<String> task : doneTasks2) {
			  System.out.println( " checked item for done list " + itemId);
	            for (String value : task) {
	                System.out.println(value + " value of the array");
	                if(value.equals(itemId)) {System.out.println(value + " value and item " + itemId); return true;}
	            }   
	        }
		  return false;
	 }
	 
	
	 
	 
//	 public static void addItems(List<WorldEntity> entities) {
//		TestingTaskStack myObj =  new TestingTaskStack();
//		for(WorldEntity e :entities) {
//			myObj.tasksList.forEach(s-> System.out.println(s.itemId));
//			if(e.id.contains("door") && !myObj.itemExcist(e.id)) {				
//				myObj.tasksList.add(new TestingTaskStack(e.id, false,e.position, null));	
//			}
//	    }
//	 }

}
