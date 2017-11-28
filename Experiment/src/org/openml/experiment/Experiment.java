package org.openml.experiment;

import java.io.File;
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
import org.openml.apiconnector.xml.SetupParameters.Parameter;

import com.rapidminer.Process;
import com.rapidminer.RapidMiner;
import com.rapidminer.operator.Operator;

public class Experiment {
	
	private String taskId;
	private int flowId;
	private int setupId;
	private String url = null;
	private String key = null;
	private Logger logger;
	private HashMap<String, HashMap<String, String>> parameters;
	
	public Experiment(HashMap<String, String> parameters) throws Exception{
		
		logger = Logger.getInstance();
		
		try {
			this.taskId = parameters.get("task_id");
			this.flowId = Integer.parseInt(parameters.get("flow_id"));
			this.setupId = Integer.parseInt(parameters.get("setup_id"));
			
			if(parameters.containsKey("url") && parameters.containsKey("key")) {
				this.url = parameters.get("url");
				this.key = parameters.get("key");
			}
			else {
				Config config = getConfigurationFile();
				this.url = config.getServer();
				this.key = config.getApiKey();
			}
			this.build();
		}
		catch(NumberFormatException e) {
			logger.logToFile(e.getMessage() + ExceptionUtils.getStackTrace(e));
			throw e;
		}
		catch(Exception e) {
			logger.logToFile(e.getMessage() + ExceptionUtils.getStackTrace(e));
			throw e;
		}
	}
	
	/** Build the experiment by configuring the operators with their corresponding parameters.
	 * 
	 * @throws Exception
	 */
	private void build() throws Exception{
		
		File processFile;
		OpenmlConnector connector = new OpenmlConnector(this.url, this.key);
		
		try {
			processFile = getTheProcessFile(this.flowId, connector);
			SetupParameters paramFile = connector.setupParameters(this.setupId);
			this.parameters = groupParameters(paramFile);
		}
		catch(Exception e) {
			logger.logToFile(e.getMessage() + ExceptionUtils.getStackTrace(e));
			throw e;
		}
		RapidMiner.setExecutionMode(RapidMiner.ExecutionMode.COMMAND_LINE);
		RapidMiner.init();
		Process process = RapidMiner.readProcessFile(processFile);
		processFile.deleteOnExit();
		configureDownloadOp(process, this.url, this.key, this.taskId);
		configureUploadOp(process, this.url, this.key);
		configureOtherOp(process, this.parameters);
		
		process.run();

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
	 * Groups the parameter names and values on a HashMap. The HashMaps are then grouped on operators in another HashMap.
	 * @param paramFile 
	 * @return
	 */
	private HashMap<String, HashMap<String, String>> groupParameters(SetupParameters paramFile) {
		
		HashMap<String, HashMap<String, String>> parameters = new HashMap<String, HashMap<String, String>>();
		
		for(Parameter param: paramFile.getParameters()) {
			String[] mapping = param.getParameter_name().split("__");
			String operator = mapping[0];
			String paramName = mapping[1];
			String value = param.getValue();
			if(parameters.containsKey(operator)) {
				HashMap<String, String> temp = parameters.get(operator);
				temp.put(paramName, value);
				parameters.put(operator, temp);
			}
			else {
				HashMap<String, String> temp = new HashMap<String, String>();
				temp.put(paramName, value);
				parameters.put(operator, temp);
				
			}
		}
		return parameters;
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
	private Config getConfigurationFile() throws Exception {
		
		File configFile = new File(Constants.OPENML_DIRECTORY + "/openml.conf");
		if(configFile.exists() && configFile.isFile()) {
			return new Config();
		}
		else {
			throw new Exception("No configuration file found");
		}
	}
}
