package org.openml.experiment;

import java.io.File;
import java.util.HashMap;

import org.openml.apiconnector.io.OpenmlConnector;
import org.openml.apiconnector.xml.UploadFlow;

public class Main {

	public static void main(String[] args) {
	
		OpenmlConnector connector = new OpenmlConnector("https://test.openml.org/", "4bf26c358bdd980987cf4d327508fed5");
		File example_flow = new File("resources/example_flow.xml");
		if(example_flow.exists()) {
			System.out.print("true");
			System.out.println(example_flow.toString());
		}
		try {
			UploadFlow flow = connector.flowUpload(example_flow, null, null);
			System.out.println(flow.getId());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		/*
		
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("setup_id", "3950");
		map.put("task_id", "115");
		map.put("flow_id", "17143");
		Experiment exp = new Experiment(map);
		exp.setUp();
		int runId= exp.run();
		*/
	}

}
