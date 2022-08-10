package onlineSearch;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.opentest4j.AssertionFailedError;

import game.Platform;

public class RunTest {

	
	/*
	 * To call all levels in a folder, the directory should be given and also to save the traces in the 
	 * onlineSearch algorithm you need to set the directory where to save the data. 
	 * The name of the target door should be given for each levels 
	 * */
	public static void main(String[] args) throws InterruptedException, IOException{
		
		String doorName = "door6";		
		String levelName = "BM2021_diff3_R4_2_2_M";
		String levelDirectory = "MutatedFiles"+File.separator + levelName +File.separator+ "selectedLevels";
		
		
		File directory = new File(Platform.LEVEL_PATH +File.separator + levelDirectory );
		File fileCount[] = directory.listFiles();
		
		System.out.print("tets" + directory);
		
		String folderPath = Platform.LEVEL_PATH + File.separator +"MutatedFiles"+ File.separator + "result";
		File theDir = new File(folderPath);
		if(!theDir.exists())
			theDir.mkdirs();
		String resultFile = folderPath+ File.separator + levelName + File.separator + levelName+ ".csv";  
		BufferedWriter br = new BufferedWriter(new FileWriter(resultFile));
		StringBuilder sb = new StringBuilder();
				
		
		//read file's name
    	for(int s = 0; s < fileCount.length; s++) {
	        String fileName = fileCount[s].getName();
	        fileName = fileName.replaceFirst("[.][^.]+$", "");
	    	sb.append(fileName);
			sb.append(","); 
			
			
	    	//String fileName = "GameLevel2_2020_10_28_16.14.14";
	    	try {
	    		List<Object> myList = new ArrayList<Object>();
	    		
	    		/*normal level test*/
//	    		LevelTest objLevelTest = new LevelTest();
//				LevelTest.start();
//				myList = objLevelTest.closetReachableTest(levelName, fileName );
//				LevelTest.close();
	    		
	    		/*Level test smarter agent*/
	    		onlineSearchMultiRun objLevelTestSmarter = new onlineSearchMultiRun();
	    		onlineSearchMultiRun.start();
				myList = objLevelTestSmarter.closetReachableTest(levelDirectory, fileName, doorName );
				onlineSearchMultiRun.close();
//				
				for (Object element : myList) {
					sb.append(element);
					sb.append(","); 
					 
				}
				sb.append("\n");
	    	}catch(AssertionFailedError afe){
	    		//throw new AssertionFailedError();
	    		sb.append("\n");
	    		continue;
	    	}
	    	
    	}
    	br.write(sb.toString());
		br.close();
	}
}
