package org.openml.experiment;
import java.io.File;
import java.net.URL;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.openml.apiconnector.io.OpenmlConnector;
import org.openml.apiconnector.settings.Config;
import org.openml.apiconnector.settings.Constants;
import org.openml.apiconnector.xml.Flow;
import org.openml.experiment.operators.AnovaSVM;
import org.openml.experiment.operators.DotSVM;
import org.openml.experiment.operators.SVM;
import org.openml.experiment.operators.Download;
import org.openml.experiment.operators.EpachnenikovSVM;
import org.openml.experiment.operators.GaussianCombinationSVM;
import org.openml.experiment.operators.MultiquadricSVM;
import org.openml.experiment.operators.NeuralSVM;
import org.openml.experiment.operators.PolynomialSVM;
import org.openml.experiment.operators.RadialSVM;
import org.openml.experiment.operators.SVMWrapper;
import org.openml.experiment.operators.Upload;

import com.rapidminer.Process;
import com.rapidminer.RapidMiner;
import com.rapidminer.operator.Operator;

/**
 * 
 * @author Arlind Kadra
 * Set up an experiment using multiple operators with a collection of parameters.
 */

public class Experiment{
	
	private HashMap<String, String> args;
	private String taskId;
	private int flowId;
	private Config config;
	private String classificationStrategy;
	private SVMWrapper wrapper;
	private SVM svm;
	private Process process;
	
	public Experiment(HashMap<String, String> arguments) throws Exception{
		
		args = arguments;
		taskId = arguments.get("task_id");
		flowId = Integer.parseInt(arguments.get("flow_id"));
		classificationStrategy = arguments.get("classification_strategy");
		config = getConfigurationFile();
		buildSVMExperiment();
	}
	
	private void keepOnlySVMParameters(){
		
		args.remove("task_id");
		args.remove("flow_id");
		args.remove("classification_strategy");
	}
	
	private void buildSVMExperiment() throws Exception{
		
		File processFile = getTheProcessFile(flowId);
		keepOnlySVMParameters();
		RapidMiner.setExecutionMode(RapidMiner.ExecutionMode.COMMAND_LINE);
		RapidMiner.init();
		process = RapidMiner.readProcessFile(processFile);
		processFile.deleteOnExit();
		new Download(process.getOperator("Download"), config, taskId);
		wrapper = new SVMWrapper(process.getOperator("Polynominal by Binominal Classification"), classificationStrategy);
		Operator svmOperator = process.getOperator("SVM");
		svm = buildParticularSVM(svmOperator, args.get("kernel_type"));
		new Upload(process.getOperator("Upload"), config);
	}
	
	public void run() throws Exception{
		
		process.run();
	}
	
	/**
	 * Use the flow id to get the RapidMiner process
	 * @param flowId
	 * @return - The xml RapidMiner file that we want to run
	 * @throws Exception
	 */
	private File getTheProcessFile(int flowId) throws Exception{
		
		OpenmlConnector connector = new OpenmlConnector(config.getServer(), config.getApiKey());
		Flow flow = connector.flowGet(flowId);
		String processUrl = flow.getSource_url();
		URL url = new URL(processUrl);
		File processFile = File.createTempFile("svm_experiment", ".xml");
		// copy the file from Openml
		FileUtils.copyURLToFile(url, processFile);
		return processFile;
	}
	
	/**
	 * Build a particular SVM considering the kernel type and the arguments given
	 * @param operator - RapidMiner SVM operator
	 * @param kernelType - kernel type of the SVM
	 * @return - SVM Wrapper
	 */
	private SVM buildParticularSVM(Operator operator, String kernelType) throws Exception{
		
		SVM temp = null;
		switch(kernelType){
			case "dot":
				temp = new DotSVM(args, operator);
				break;
			case "radial":
				temp = new RadialSVM(args, operator);
				break;
			case "polynomial":
				temp = new PolynomialSVM(args, operator);
				break;
			case "neural":
				temp = new NeuralSVM(args, operator);
				break;
			case "anova":
				temp = new AnovaSVM(args, operator);
				break;
			case "epachnenikov":
				temp = new EpachnenikovSVM(args, operator);
				break;
			case "gaussian_combination":
				temp = new GaussianCombinationSVM(args, operator);
				break;
			case "multiquadric":
				temp = new MultiquadricSVM(args, operator);
				break;
			default:
				throw new Exception("Wrong value for kernel type");
		}
		return temp;
	}
	
	/**
	 * Get the configuration file
	 * @return - Config file
	 * @throws Exception
	 */
	private Config getConfigurationFile() throws Exception{
		
		File configFile = new File(Constants.OPENML_DIRECTORY + "/openml.conf");
		if(configFile.exists() && configFile.isFile()){
			return new Config();
		}
		else{
			throw new Exception("No configuration file found");
		}
	}

	public HashMap<String, String> getArgs(){
		return args;
	}

	public void setArgs(HashMap<String, String> args){
		
		this.args = args;
		if(svm != null){
			svm.setParameters(args);
		}
	}

	public String getTaskId(){
		
		return taskId;
	}

	public void setTaskId(String taskId){
		
		this.taskId = taskId;
	}

	public int getFlowId(){
		
		return flowId;
	}

	public void setFlowId(int flowId){
		
		this.flowId = flowId;
	}

	public Config getConfig(){
		
		return config;
	}

	public void setConfig(Config config){
		
		this.config = config;
	}

	public String getClassificationStrategy(){
		
		return classificationStrategy;
	}

	public void setClassificationStrategy(String classificationStrategy){
		
		this.classificationStrategy = classificationStrategy;
		if(wrapper != null) {
			wrapper.setClassificationStrategy(classificationStrategy);
		}
	}
}