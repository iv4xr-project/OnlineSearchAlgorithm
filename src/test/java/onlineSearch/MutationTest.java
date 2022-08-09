package onlineSearch;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.ss.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.*;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;


public class MutationTest {
//removing doors(replace with walls)
//removing buttons
//changing the connection

//one strategy, get a level -> find all cell contains d and then remove one by one and create a 
// new csv file at the same time and replace it back( in a loop)
// the same for the button
// relations? get the all d and b (reading the csv file), read the first coulmn and then change the each connection

	private static String[] nextLine;
	private static CSVReader reader;
	private static String levelName = "BM2021_diff1_R4_1_1.csv";
	private static String excelFilePath = "C://Users//Shirz002//OneDrive - Universiteit Utrecht//Samira//Ph.D//OnlineSearch//src//test//resources//Levels//CompetitionGrander//bm2021//" + levelName;   	    
	private static ArrayList<Object[]> doors = new ArrayList<>();
	private static ArrayList<Object[]> buttons = new ArrayList<>();;
	private static ArrayList<Object[]> connections = new ArrayList<>();;
	
	/*
	 * read .csv file and create a list of all buttons, doors, connections
	 * */
	public static void readFromExcelFile(String excelFilePath) throws IOException{	 
        reader = new CSVReader(new FileReader(excelFilePath)); 
        int lineNumber = 0;
        while ((nextLine = reader.readNext()) != null) {	
        	lineNumber++;
	         String[] arr = nextLine;
	       //  System.out.println(Arrays.toString(arr) );
	         
				 for(int j =0; j<arr.length ;j++) {
					 if(!arr[0].contains("button")) {
						if(arr[j].contains("door") ) {	
							
							Object[] temp = new Object[3];
							
							temp[0] = lineNumber;
							temp[1] = j;
							String entityName = arr[j].split("\\^")[1]; 							
							
							if(!entityName.substring(entityName.lastIndexOf("r") + 2).isBlank() )							
								entityName = entityName.split(":")[0]; 
								
							temp[2] = entityName;
							
							doors.add(temp);										
						}	
						
						if(arr[j].contains("button") ) {	
							Object[] temp = new Object[3];
							
							temp[0] = lineNumber;
							temp[1] = j;
							temp[2] = arr[j].split("\\^")[1];				
							buttons.add(temp);				
						}	
					 }else {
		        	 // save the connections 
						 Object[] temp = new Object[4];
						if(!arr[j].isEmpty()) {	
							temp[0] = lineNumber;
							temp[1] = j;
							temp[2] = arr[j];
							temp[3] = arr[j+1];
							connections.add(temp);	break;
						}
			         }	
	         }
		} 
        	
//		System.out.println("////////////************");
//		connections.forEach(se -> System.out.println(se) );
		
    }

	
	
	public static void saveMutatedFile(Object[] newData, String replaceWith) throws IOException {
		String newFileLocation = "C://Users//Shirz002//OneDrive - Universiteit Utrecht//Samira//Ph.D//OnlineSearch//src//test//resources//Levels//MutatedFiles//A-test";
		CSVWriter writer = new CSVWriter(new FileWriter(newFileLocation+"//" + newData[2] +"_"+ levelName ), ',');
		CSVReader readerTmp= new CSVReader(new FileReader(excelFilePath)); 		
		List<String[]> csvBody = readerTmp.readAll();		
		readerTmp.close();
		
		csvBody.get((int) newData[0]-1)[(int) newData[1]] = replaceWith;
//		writer.writeAll(csvBody);
//		writer.flush();
//		writer.close();
		
		BufferedWriter br = new BufferedWriter(new FileWriter(newFileLocation+"//" + newData[2] +"_"+ levelName ), ',');
		StringBuilder sb = new StringBuilder();
		for (String[] element : csvBody) {
			for (String a : element) {
				sb.append(a);
				sb.append(","); 
			}
			sb.append("\n"); 
		}	
		br.write(sb.toString());
		br.close();		
	}
	
	public static void saveMutatedConnections(Object[] newData, String replaceWith) throws IOException {
		String newFileLocation = "C://Users//Shirz002//OneDrive - Universiteit Utrecht//Samira//Ph.D//OnlineSearch//src//test//resources//Levels//MutatedFiles//A-test" ;
		System.out.println(buttons.size() + "*--buttons.size()--**");
		for(int i=0; i< buttons.size(); i++) {				
			System.out.println((newData[2] )+ "*--buttons.get(i)[2]--**");
				if(!buttons.get(i)[2].equals(newData[2])) {	
					
					String fileName = newData[2]+"_"+newData[3]+"_"+i;
					CSVWriter writer = new CSVWriter(new FileWriter(newFileLocation+"//" + fileName +"_"+ levelName ), ',');					 		
					CSVReader readerTmp= new CSVReader(new FileReader(excelFilePath));
					List<String[]> csvBody = readerTmp.readAll();		
					readerTmp.close();					
					csvBody.get((int) newData[0]-1)[(int) newData[1]] = (String) buttons.get(i)[2];
					
					BufferedWriter br = new BufferedWriter(new FileWriter(newFileLocation+"//" + fileName +"_"+ levelName ), ',');
					StringBuilder sb = new StringBuilder();
					for (String[] element : csvBody) {
						for (String a : element) {
							
							sb.append(a);
							sb.append(","); 
						}
						sb.append("\n"); 
					}	
					br.write(sb.toString());
					br.close();	
					
//					writer.writeAll(csvBody);
//					writer.flush();
//					writer.close();
				}
			}			
	}
	public static void main(String[] args) throws IOException {
	    
	    
	    Boolean replaceDoor = true;
	    Boolean replaceButton = true;
	    Boolean replaceConnectoin = true;
	    readFromExcelFile(excelFilePath);
	    
	    if(replaceDoor) {	
	    	
	    	for(int i=0; i<doors.size(); i++ ) {				 
				 saveMutatedFile(doors.get(i), "w");
			 }
	    }
	    if(replaceButton) {	    	
	    	for(int i=0; i<buttons.size(); i++ ) {				 
				 saveMutatedFile(buttons.get(i), "f");
			 }
	    }
	    if(replaceConnectoin){
	    	for(int i=0; i<connections.size(); i++ ) {	    		
	    		saveMutatedConnections(connections.get(i), " ");
			 }
	    }
	}
	
}
