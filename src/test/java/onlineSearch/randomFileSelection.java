package onlineSearch;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
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
				ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		    	System.setOut(new PrintStream(buffer));
				String levelName = "MOSA";
	            File dir = new File(Platform.LEVEL_PATH + File.separator +"MutatedFiles"+ File.separator +levelName);	           
	            File[] files = dir.listFiles();
	            final List<File> stringsList = Arrays.stream(files)
	            	    .distinct().collect(Collectors.toList());
	            	Collections.shuffle(stringsList);
	            	final int k = 9; // the sample size
	            	List<File> sample = stringsList.subList(0, k); // this is a view, not another list
	            	String path = Platform.LEVEL_PATH + File.separator +"MutatedFiles"+ File.separator + levelName + File.separator + "selectedLevels"+ File.separator;
	            	System.err.println(sample);
	            	
	            	
	            	for(File file : sample) {
	            	    Files.copy(file.toPath(),
	            	        (new File(path + file.getName())).toPath(),
	            	        StandardCopyOption.REPLACE_EXISTING);
	            	}
	            	// Stop capturing
					System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));

					// Use captured content
					String content = buffer.toString();
					buffer.reset();
	    }
}
