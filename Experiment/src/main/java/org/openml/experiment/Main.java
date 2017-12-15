package org.openml.experiment;

import java.util.HashMap;

public class Main {

	public static void main(String[] args) throws Exception {
	
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("setup_id", "3950");
		map.put("task_id", "115");
		map.put("flow_id", "17143");
		Experiment exp = new Experiment(map);
		exp.setUp();
		int runId= exp.run();
		System.out.println(runId);
	}	
}