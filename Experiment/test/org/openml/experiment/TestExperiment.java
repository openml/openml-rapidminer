package org.openml.experiment;

import static org.junit.Assert.*;

import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;
import org.openml.apiconnector.io.OpenmlConnector;
import org.openml.apiconnector.xml.Run;
import org.openml.apiconnector.xml.SetupParameters;
import org.openml.experiment.utils.DataUtils;

public class TestExperiment {

	private HashMap<String, String> map;
	private OpenmlConnector connector;
	private Run run;
	private Experiment exp;
	
	@Before
	public void setUp() throws Exception {
		
		map = new HashMap<String, String>();
		map.put("setup_id", "3951");
		map.put("task_id", "115");
		map.put("flow_id", "17144");
		map.put("url", "https://test.openml.org/");
		map.put("key", "4bf26c358bdd980987cf4d327508fed5");
		connector = new OpenmlConnector("https://test.openml.org/", "4bf26c358bdd980987cf4d327508fed5");
		exp = new Experiment(map);
		exp.setUp();
		int runId = exp.run();
		if(runId != -1) {
			run = connector.runGet(runId);
		} else {
			run = null;
		}

	}

	@Test
	public void testRun() throws Exception {
		
		if(run != null) {
			int setupId = Integer.parseInt(map.get("setup_id"));
			int taskId = Integer.parseInt(map.get("task_id"));
			int flowId = Integer.parseInt(map.get("flow_id"));
			if (setupId == run.getSetup_id() && taskId == run.getTask_id() && flowId == run.getFlow_id()) {
				return;
			} else {
			fail("The setup/task/flow Id of the uploaded run do not match with the used configuration");
			}
		} else {
			fail("The run was not uploaded");
		}
	}
	
	@Test
	public void testSetUp() throws Exception {

		if(run != null) {
			SetupParameters setup = connector.setupParameters(run.getSetup_id());
			HashMap<String, HashMap<String, String>> paramMap = DataUtils.groupParameters(setup);
			if (paramMap.equals(exp.getParameters())) {
				return;
			} else {
				fail("The setup parameters do not match with the used configuration");
			}
		}
		else {
			fail("The run was not uploaded");
		}
	}
}