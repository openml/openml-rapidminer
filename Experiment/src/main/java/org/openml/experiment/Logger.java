package org.openml.experiment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

public class Logger {
	
	private static Logger instance;
	private File file;
	private final String DIR = System.getProperty("user.dir");
	private Logger() {
		
		file = new File(DIR, "errorLog.txt");
		if(!(file.exists() && file.isFile())) {
			try {				
				file.createNewFile();
			} catch(IOException e) {
				System.out.println("Not able to log messages, problems creating the file:" + e.getMessage());
				e.printStackTrace();
			}
		}
	}
	
	public static synchronized Logger getInstance() {
		
		if(instance != null){
			return instance;
		} else {
			instance = new Logger();
			return instance;
		}
	}
	
	public void logToFile(String text) {
		
		Date date = new Date();
		try {
			FileWriter writer = new FileWriter(file, true);
			writer.write(date.toString() + "-" + text );
			writer.write(System.getProperty("line.separator"));
			writer.close();
		} catch(IOException e) {
			System.out.println("Not able to log messages, problems writting to the file:" + e.getMessage());
			e.printStackTrace();
		}
	}
}