package org.openml.experiment;

import java.io.File;
import java.net.URL;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openml.apiconnector.io.OpenmlConnector;
import org.openml.apiconnector.settings.Config;
import org.openml.apiconnector.settings.Constants;
import org.openml.apiconnector.xml.Flow;
import org.openml.apiconnector.xml.SetupParameters;
import org.openml.apiconnector.xml.SetupParameters.Parameter;

public class Experiment {
	
	private int taskId;
	private int flowId;
	private int setupId;
	private String url = null;
	private String key = null;
	private Logger logger;
	private Config config = null;
	private HashMap<String, HashMap<String, String>> parameters;
	
	public Experiment(HashMap<String, String> parameters) {
		
		logger = Logger.getInstance();
		
		try {
			this.taskId = Integer.parseInt(parameters.get("taskId"));
			this.flowId = Integer.parseInt(parameters.get("flowId"));
			this.setupId = Integer.parseInt(parameters.get("setupId"));
			
			if(parameters.containsKey("url") && parameters.containsKey("key")) {
				this.url = parameters.get("url");
				this.key = parameters.get("key");
			}
			else {
				config = getConfigurationFile();
			}
		}
		catch(NumberFormatException e) {
			logger.logToFile(e.getMessage() + ExceptionUtils.getStackTrace(e));
			throw e;
		}
		catch(Exception e) {
			
		}
	}
	
	private void build() {
		
		
		OpenmlConnector connector;
		File processFile;
		if(config != null) {
			connector = new OpenmlConnector(config.getServer(), config.getApiKey());
		}
		else {
			connector = new OpenmlConnector(this.url, this.key);
		}
		
		try {
			processFile = getTheProcessFile(this.flowId, connector);
			SetupParameters paramFile = connector.setupParameters(this.setupId);
			this.parameters = groupParameters(paramFile);
		}
		catch(Exception e) {
			
		}

	}
	
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
				parameters.put(operator, new HashMap<String, String>());
			}
		}
		return parameters;
	}
	
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
