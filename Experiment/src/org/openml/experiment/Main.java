package org.openml.experiment;

import java.util.HashMap;

public class Main {

	public static void main(String[] args) {
		
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("setup_id", "1088");
		map.put("task_id", "115");
		map.put("flow_id", "4682");
		try {
			new Experiment(map);
		}
		catch(Exception e) {
			
		}
		
	}

}
