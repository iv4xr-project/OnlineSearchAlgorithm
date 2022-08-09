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

	public static void main(String[] args) throws InterruptedException, IOException{
		
		String levelName = "MutatedFiles"+File.separator + "selectedLevels"+File.separator + "test";
		File directory = new File(Platform.LEVEL_PATH +File.separator + levelName );
		File fileCount[] = directory.listFiles();
		
		String folderPath = Platform.LEVEL_PATH + File.separator +"MutatedFiles"+ File.separator + "selectedLevels";
		File theDir = new File(folderPath);
		if(!theDir.exists())
			theDir.mkdirs();
		String resultFile = folderPath+ File.separator +"testResult.csv";  
		BufferedWriter br = new BufferedWriter(new FileWriter(resultFile));
		StringBuilder sb = new StringBuilder();
		
		String doorName = "door6";
		
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
				myList = objLevelTestSmarter.closetReachableTest(levelName, fileName, doorName );
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
