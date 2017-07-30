package org.openml.rapidminer;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openml.apiconnector.algorithms.Conversion;
import org.openml.apiconnector.xml.EvaluationScore;
import org.openml.apiconnector.xml.Flow;
import org.openml.apiconnector.xml.Run;
import org.openml.rapidminer.models.OpenmlConfigurable;
import org.openml.rapidminer.models.OpenmlExecutedTask;
import org.openml.rapidminer.utils.ImplementationUtils;
import org.openml.rapidminer.utils.OpenmlConfigurator;
import org.openml.rapidminer.utils.OpenmlConnectorJson;
import org.openml.rapidminer.utils.XMLUtils;

import com.rapidminer.MacroHandler;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.tools.WekaInstancesAdaptor;
import com.rapidminer.tools.WekaTools;
import com.rapidminer.tools.config.ConfigurationException;
import com.rapidminer.tools.config.ConfigurationManager;
import com.rapidminer.tools.config.ParameterTypeConfigurable;

public class UploadOpenmlTask extends Operator {

	private static String PARAMETER_CONFIG = "OpenML Connection";
	private static final String[] TAGS = {"RapidMiner"};
	
	private OpenmlConnectorJson openmlConnector;
	private InputPort predictionsInput = getInputPorts().createPort("predictions",OpenmlExecutedTask.class);
	
	public UploadOpenmlTask(OperatorDescription description) {
		super(description);
	}
	
	@Override
	public void doWork() throws OperatorException {
		if (XMLUtils.validProcess(getProcess().getRootOperator().getXML(true)) == false) {
			throw new OperatorException("Not a valid OpenML process. Please check the OpenML definition. Note that each operator can only be used once. ");
		}
		
		OpenmlExecutedTask oet = predictionsInput.getData(OpenmlExecutedTask.class);
		OpenmlConfigurable config;
		String apikey;
		String url;
		MacroHandler mHandler = this.getProcess().getMacroHandler();
		if(mHandler.getMacro("apikey")!= null && mHandler.getMacro("url")!= null)
		{
			apikey = mHandler.getMacro("apikey");
			url = mHandler.getMacro("url");
		}
		else if(this.isParameterSet("Url") && this.isParameterSet("Api key"))
		{
			url = this.getParameter("Url");
			apikey = this.getParameter("Api key");
		}
		else
		{
			try 
			{
				config = (OpenmlConfigurable) ConfigurationManager.getInstance().lookup(
				OpenmlConfigurator.TYPE_ID, getParameterAsString(PARAMETER_CONFIG), 
				getProcess().getRepositoryAccessor());
				apikey = config.getApiKey();
				url = config.getUrl();
			} 
			catch (ConfigurationException e) 
			{
				throw new UserError(this, e, "openml.configuration_read");
			}
		}		openmlConnector = new OpenmlConnectorJson(url, apikey, true);
		
		try {
			// TODO: make the user enter his other meta-data!
			String processXml = XMLUtils.prepare(getProcess().getRootOperator().getXML(true));
			Flow workflow = XMLUtils.xmlToImplementation(processXml);
			
			//Conversion.log("OK","Upload Run", XMLUtils.flowToXml(workflow, null, null));
			
			int implementation_id = ImplementationUtils.getImplementationId(workflow, processXml, openmlConnector);

			// TODO: resolve parameter string
			Run run = XMLUtils.xmlToRun(getProcess().getRootOperator().getXML(true), openmlConnector, implementation_id, oet.getTaskId(), TAGS); //new Run(oet.getTaskId(), null, implementation_id, "", null, TAGS);
			for (EvaluationScore score : oet.getEvaluationMeasures()) {
				run.addOutputEvaluation(score);
			}
			
			
			//Conversion.log("OK","Upload Run", XMLUtils.runToXml(run));
			
			Map<String,File> files = new HashMap<String, File>();
			String taskName = "openml-task-" + oet.getTaskId() + "-predictions"; 
			// clustering task is safe as we don't need any class label anymore
			File predictionArff = Conversion.stringToTempFile(WekaTools.toWekaInstances(oet.getPredictions(), taskName , WekaInstancesAdaptor.CLUSTERING).toString(), taskName, "arff");
			files.put("predictions", predictionArff);
			
			openmlConnector.runUpload(XMLUtils.runToXml(run), files);
		} catch(Exception e) {
			e.printStackTrace();
			throw new OperatorException("Error uploading task to Openml: " + e.getMessage() + " " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(e));
		}
	}
	
	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeConfigurable(PARAMETER_CONFIG, "Choose an OpenML Connection", OpenmlConfigurator.TYPE_ID));
		return types;
	}
}
