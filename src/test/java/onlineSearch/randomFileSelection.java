package onlineSearch;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class randomFileSelection {

	public static void main(String[] args)
	        throws Exception {
	            File dir = new File("C://Users//Shirz002//OneDrive - Universiteit Utrecht//Samira//Ph.D//OnlineSearch//src//test//resources//Levels//MutatedFiles//A-test");
	            //String[] files = dir.list();	  
	            File[] files = dir.listFiles();
	            final List<File> stringsList = Arrays.stream(files)
	            	    .distinct().collect(Collectors.toList());
	            	Collections.shuffle(stringsList);
	            	final int k = 5; // the sample size
	            	List<File> sample = stringsList.subList(0, k); // this is a view, not another list
	            	String path = "C://Users//Shirz002//OneDrive - Universiteit Utrecht//Samira//Ph.D//OnlineSearch//src//test//resources//Levels//MutatedFiles//selectedLevels//";
	            	System.err.println(sample);
	            	
	            	
	            	for(File file : sample) {
	            	    Files.copy(file.toPath(),
	            	        (new File(path + file.getName())).toPath(),
	            	        StandardCopyOption.REPLACE_EXISTING);
	            	}
	    }
}
