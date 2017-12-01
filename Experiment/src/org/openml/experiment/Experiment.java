package org.openml.experiment;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openml.apiconnector.io.OpenmlConnector;
import org.openml.apiconnector.settings.Config;
import org.openml.apiconnector.settings.Constants;
import org.openml.apiconnector.xml.Flow;
import org.openml.apiconnector.xml.SetupParameters;
import org.openml.experiment.utils.DataUtils;

import com.rapidminer.Process;
import com.rapidminer.RapidMiner;
import com.rapidminer.operator.AbstractIOObject;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.MissingIOObjectException;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;

public class Experiment {
	
	private String taskId;
	private int flowId;
	private int setupId;
	private String url = null;
	private String key = null;
	private Logger logger;
	Process process;
	private HashMap<String, HashMap<String, String>> parameters;
	
	public Experiment(HashMap<String, String> parameters) {
		
		logger = Logger.getInstance();
		
		try {
			this.taskId = parameters.get("task_id");
			this.flowId = Integer.parseInt(parameters.get("flow_id"));
			this.setupId = Integer.parseInt(parameters.get("setup_id"));
			
			if(parameters.containsKey("url") && parameters.containsKey("key")) {
				this.url = parameters.get("url");
				this.key = parameters.get("key");
			} else {
				Config config = getConfigurationFile();
				this.url = config.getServer();
				this.key = config.getApiKey();
			}
		} catch(NumberFormatException | FileNotFoundException e) {
			logger.logToFile(e.getMessage() + ExceptionUtils.getStackTrace(e));
			System.exit(0);
		}
	}
	
	/** Build the experiment by configuring the operators with their corresponding parameters.
	 * 
	 */
	public void setUp() {
		
		try {
			OpenmlConnector connector = new OpenmlConnector(this.url, this.key);
			File processFile = getTheProcessFile(this.flowId, connector);
			SetupParameters paramFile = connector.setupParameters(this.setupId);
			this.parameters = DataUtils.groupParameters(paramFile);
			RapidMiner.setExecutionMode(RapidMiner.ExecutionMode.COMMAND_LINE);
			RapidMiner.init();
			this.process = RapidMiner.readProcessFile(processFile);
			processFile.deleteOnExit();
			configureDownloadOp(process, this.url, this.key, this.taskId);
			configureUploadOp(process, this.url, this.key);
			configureOtherOp(process, this.parameters);
		} catch(Exception e) {
			logger.logToFile(e.getMessage() + ExceptionUtils.getStackTrace(e));
			System.exit(0);
		}
		
	}
	
	public int run() {
		
		try {
			IOContainer c = process.run();
			AbstractIOObject uploadRun = c.get(AbstractIOObject.class);
			return Integer.parseInt(uploadRun.toString());
		} catch(MissingIOObjectException | NumberFormatException e) {
			logger.logToFile(e.getMessage() + ExceptionUtils.getStackTrace(e));
		} catch (OperatorException e) {
			logger.logToFile(e.getMessage() + ExceptionUtils.getStackTrace(e));
		}
		return -1;
	}
	
	private void configureDownloadOp(Process process, String url, String key, String taskId ) {

		Operator download = process.getOperator("Download");           
		download.setParameter("Url", url);
		download.setParameter("Api key", key);
		download.setParameter("Task_id", taskId);
	}
	
	private void configureUploadOp(Process process, String url, String key) {

		Operator download = process.getOperator("Upload");           
		download.setParameter("Url", url);
		download.setParameter("Api key", key);
	}
	
	private void configureOtherOp(Process process, HashMap<String, HashMap<String, String>> param) {
		
		for (String operatorName : param.keySet()) {
			Operator operator = process.getOperator(operatorName);
			for (Entry<String, String> entry : param.get(operatorName).entrySet()) {
			    operator.setParameter(entry.getKey(), entry.getValue());
			}
		}
	}
	
	/**
	 * Uses the processUrl field from the flow and downloads the process file.
	 * @param flowId
	 * @param connector
	 * @return RapidMiner Process file
	 * @throws Exception
	 */
	
	private File getTheProcessFile(int flowId, OpenmlConnector connector) throws Exception {
		
		Flow flow = connector.flowGet(flowId);
		String processUrl = flow.getSource_url();
		URL url = new URL(processUrl);
		File processFile = File.createTempFile("svm_experiment", ".xml");
		// copy the file from Openml
		FileUtils.copyURLToFile(url, processFile);
		return processFile;
	}
	
	/**
	 * Get the configuration file
	 * @return - Config file
	 * @throws Exception
	 */
	private Config getConfigurationFile() throws FileNotFoundException {
		
		File configFile = new File(Constants.OPENML_DIRECTORY + "/openml.conf");
		if(configFile.exists() && configFile.isFile()) {
			return new Config();
		} else {
			throw new FileNotFoundException("No configuration file found");
		}
	}
	
	public HashMap<String, HashMap<String, String>> getParameters() {
		return this.parameters;
	}
	
	public void setParameters(HashMap<String, HashMap<String, String>> paramMap) {
		this.parameters = paramMap;
	}
}