package org.openml.experiment;
import java.io.File;
import java.net.URL;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openml.apiconnector.io.OpenmlConnector;
import org.openml.apiconnector.settings.Config;
import org.openml.apiconnector.settings.Constants;
import org.openml.apiconnector.xml.Flow;
import org.openml.experiment.operators.AnovaSVM;
import org.openml.experiment.operators.DotSVM;
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
public class Experiment 
{
	private List<String> args;
	private String taskId;
	private int flowId;
	private Config config;
	private String classificationStrategy;
	private SVMWrapper wrapper;
	private DotSVM svm;
	private Process process;
	
	public Experiment(List<String> arguments)
	{
		try
		{
			args = arguments;
			taskId = arguments.get(0);
			flowId = Integer.parseInt(arguments.get(1));
			classificationStrategy = arguments.get(2);
			config = getConfigurationFile();
			buildSVMExperiment();
		}
		catch(Exception e)
		{
			Logger.getInstance().logToFile(e.getMessage() + ExceptionUtils.getStackTrace(e));
		}
	}
	
	// Reduce the list size, since it will propagated to configure the SVM
	private void keepOnlySVMParameters()
	{
		//args.subList(0, 3).clear();
		// Removing first 3 elements
		args.remove(0);
		args.remove(0);
		args.remove(0);
	}
	
	private void buildSVMExperiment()
	{
		try
		{
			File processFile = getTheProcessFile(flowId);
			keepOnlySVMParameters();
			RapidMiner.setExecutionMode(RapidMiner.ExecutionMode.COMMAND_LINE);
			RapidMiner.init();
			process = RapidMiner.readProcessFile(processFile);
			processFile.deleteOnExit();
			new Download(process.getOperator("Download"), getConfigurationFile(), taskId);
			wrapper = new SVMWrapper(process.getOperator("Polynominal by Binominal Classification"), classificationStrategy);
			Operator svmOperator = process.getOperator("SVM");
			svm = buildParticularSVM(svmOperator, args.get(0));
			new Upload(process.getOperator("Upload"), getConfigurationFile());
		}
		catch(Exception e)
		{
			Logger.getInstance().logToFile(e.getMessage() + ExceptionUtils.getStackTrace(e));
		}
	}
	
	public void run()
	{
		try
		{
			process.run();
		}
		catch(NullPointerException e1)
		{
			Logger.getInstance().logToFile("An experiment needs to be build, there is no process");
		}
		catch(Exception e2)
		{
			Logger.getInstance().logToFile(e2.getMessage() + ExceptionUtils.getStackTrace(e2));
		}
	}
	
	/**
	 * Use the flow id to get the RapidMiner process
	 * @param flowId
	 * @return - The xml RapidMiner file that we want to run
	 * @throws Exception
	 */
	private File getTheProcessFile(int flowId) throws Exception
	{
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
	 * @return - SVM
	 */
	private DotSVM buildParticularSVM(Operator operator, String kernelType) throws Exception
	{
		DotSVM temp = null;
		switch(kernelType)
		{
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
				Logger.getInstance().logToFile("Wrong value for kernel type");
		}
		return temp;
	}
	
	/**
	 * Get the configuration file
	 * @return - Config file
	 * @throws Exception
	 */
	private Config getConfigurationFile() throws Exception
	{
		File configFile = new File(Constants.OPENML_DIRECTORY + "/openml.conf");
		if(configFile.exists() && configFile.isFile())
		{
			return new Config();
		}
		else
		{
			throw new Exception("No configuration file found");
		}
	}

	public List<String> getArgs() 
	{
		return args;
	}

	public void setArgs(List<String> args) throws Exception 
	{
		this.args = args;
		svm = buildParticularSVM(svm.getOperator(), args.get(0));
	}

	public String getTaskId() 
	{
		return taskId;
	}

	public void setTaskId(String taskId) 
	{
		this.taskId = taskId;
	}

	public int getFlowId() 
	{
		return flowId;
	}

	public void setFlowId(int flowId) 
	{
		this.flowId = flowId;
	}

	public Config getConfig() 
	{
		return config;
	}

	public void setConfig(Config config) 
	{
		this.config = config;
	}

	public String getClassificationStrategy() 
	{
		return classificationStrategy;
	}

	public void setClassificationStrategy(String classificationStrategy) 
	{
		this.classificationStrategy = classificationStrategy;
		wrapper.setClassificationStrategy(classificationStrategy);	
	}
}