package onlineSearch;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import game.Platform;

public class randomFileSelection {

	public static void main(String[] args)
	        throws Exception {
				String levelName = "BM2021_diff3_R4_2_2_M";
	            File dir = new File(Platform.LEVEL_PATH + File.separator +"MutatedFiles"+ File.separator +levelName);	           
	            File[] files = dir.listFiles();
	            final List<File> stringsList = Arrays.stream(files)
	            	    .distinct().collect(Collectors.toList());
	            	Collections.shuffle(stringsList);
	            	final int k = 5; // the sample size
	            	
	            	List<File> sample = stringsList.subList(0, k); // this is a view, not another list
	            	String path = Platform.LEVEL_PATH + File.separator +"MutatedFiles"+ File.separator + levelName + File.separator + "selectedLevels"+ File.separator;
	            	System.err.println(sample);
	            	
	            	
	            	for(File file : sample) {
	            	    Files.copy(file.toPath(),
	            	        (new File(path + file.getName())).toPath(),
	            	        StandardCopyOption.REPLACE_EXISTING);
	            	}
	    }
}
