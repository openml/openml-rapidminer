package org.openml.experiment;

import static org.junit.Assert.fail;

import java.io.File;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;
import org.openml.apiconnector.io.ApiException;
import org.openml.apiconnector.io.OpenmlConnector;
import org.openml.apiconnector.xml.Run;
import org.openml.apiconnector.xml.SetupParameters;
import org.openml.apiconnector.xml.UploadFlow;
import org.openml.experiment.utils.DataUtils;

public class TestExperiment {

	private String taskId;
	private String url;
	private String key;
	private int flowId;
	private OpenmlConnector connector;
	private Run run;
	private Experiment exp;
	HashMap<String, HashMap<String, String>> param;
	
	@Before
	public void setUp() throws Exception {
		
		this.taskId = "115";
		this.url = "https://test.openml.org/";
		this.key = "4bf26c358bdd980987cf4d327508fed5";
		connector = new OpenmlConnector("https://test.openml.org/", "4bf26c358bdd980987cf4d327508fed5");
		this.flowId = getExampleFlowId();
		param = new HashMap<String, HashMap<String, String>>();
		HashMap<String, String> pb = new HashMap<String, String>();
		pb.put("classification_strategies", "1 against 1");
		HashMap<String, String> svm = new HashMap<String, String>();
		svm.put("kernel_type", "radial");
		svm.put("kernel_gamma", "1.03");
		svm.put("C", "0.2");
		param.put("SVM", svm);
		param.put("PolynominalbyBinominalClassification", pb);
		
		exp = new Experiment();
		exp.setConnector(this.connector);
		exp.setFlowId(this.flowId);
		exp.setTaskId(taskId);
		exp.setUrl(url);
		exp.setKey(key);
		exp.setParameters(param);
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
			int taskId = Integer.parseInt(this.taskId);
			if (taskId == run.getTask_id() && this.flowId == run.getFlow_id()) {
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
		} else {
			fail("The run was not uploaded");
		}
	}
	
	/** Upload the example flow which is on the resources folder to OpenMl and return the id.
	 * 
	 * @return 
	 * - -1 in case the flow could not be uploaded/there was no example flow in the resources folder.
	 * -  flow id if the flow was uploaded/ if it mmanaged to retreave the id of the already present flow
	 */
	private int getExampleFlowId() throws Exception {
		
		File example_flow = new File(this.getClass().getResource("/example_flow.xml").getFile());
		// If flow exists
		if(example_flow.exists() && example_flow.isFile()) {
			try {
				// Upload flow
				UploadFlow flow = connector.flowUpload(example_flow, null, null);
				return flow.getId();
			} catch(ApiException e1) {
				if(e1.getCode() == 171) {
					String message = e1.getMessage();
					Pattern pattern = Pattern.compile("\\d+");
					Matcher matcher = pattern.matcher(message);
					if (matcher.find()) {
						return Integer.parseInt(matcher.group());
				    } 
				}
			} catch (Exception e) {
				throw e;
			}
		}
		return -1;
	}
}