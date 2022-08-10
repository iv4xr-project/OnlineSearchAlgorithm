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
import game.Platform;


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
	private static String CSV = ".csv";
	private static String levelName = "BM2021_diff3_R4_2_2_M";
	private static String excelFilePath =  Platform.LEVEL_PATH + File.separator + "CompetitionGrander"+ File.separator +"bm2021"+ File.separator +  levelName+CSV;   	    
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
		String newFileLocation = Platform.LEVEL_PATH + File.separator +"MutatedFiles"+ File.separator +levelName;		
		CSVReader readerTmp= new CSVReader(new FileReader(excelFilePath)); 		
		List<String[]> csvBody = readerTmp.readAll();		
		readerTmp.close();
		
		csvBody.get((int) newData[0]-1)[(int) newData[1]] = replaceWith;

		
		BufferedWriter br = new BufferedWriter(new FileWriter(newFileLocation+ File.separator +  levelName +"_" + newData[2]+CSV ), ',');
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
		String newFileLocation = Platform.LEVEL_PATH + File.separator +"MutatedFiles"+ File.separator +levelName;

		for(int i=0; i< buttons.size(); i++) {				
			System.out.println((newData[2] )+ "*--buttons.get(i)[2]--**");
				if(!buttons.get(i)[2].equals(newData[2])) {	
					
					String fileName = newData[2]+"_"+newData[3]+"_"+i;					
					CSVReader readerTmp= new CSVReader(new FileReader(excelFilePath));
					List<String[]> csvBody = readerTmp.readAll();		
					readerTmp.close();					
					csvBody.get((int) newData[0]-1)[(int) newData[1]] = (String) buttons.get(i)[2];
					
					BufferedWriter br = new BufferedWriter(new FileWriter(newFileLocation+ File.separator +   levelName +"_" + fileName+CSV), ',');
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
