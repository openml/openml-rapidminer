package org.openml.rapidminer;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openml.apiconnector.algorithms.Conversion;
import org.openml.apiconnector.xml.EvaluationScore;
import org.openml.apiconnector.xml.Flow;
import org.openml.apiconnector.xml.Run;
import org.openml.apiconnector.xml.UploadRun;
import org.openml.rapidminer.models.OpenmlConfigurable;
import org.openml.rapidminer.models.OpenmlConfigurator;
import org.openml.rapidminer.models.OpenmlExecutedTask;
import org.openml.rapidminer.models.OpenmlUploadRun;
import org.openml.rapidminer.utils.ImplementationUtils;
import org.openml.rapidminer.utils.OpenmlConnectorJson;
import org.openml.rapidminer.utils.RMProcessUtils;
import org.openml.rapidminer.utils.XMLUtils;
import org.w3c.dom.Document;

import com.rapidminer.Process;
import com.rapidminer.MacroHandler;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.tools.WekaInstancesAdaptor;
import com.rapidminer.tools.WekaTools;
import com.rapidminer.tools.config.ConfigurationException;
import com.rapidminer.tools.config.ConfigurationManager;
import com.rapidminer.tools.config.ParameterTypeConfigurable;

public class UploadOpenmlTask extends Operator {

	private static String PARAMETER_CONFIG = "OpenML_Connection";
	private static final String[] TAGS = {"RapidMiner"};
	
	private OpenmlConnectorJson openmlConnector;
	private InputPort predictionsInput = getInputPorts().createPort("predictions",OpenmlExecutedTask.class);
	private OutputPort uploadRunOutput = getOutputPorts().createPort("run id");
	
	public UploadOpenmlTask(OperatorDescription description) {
		super(description);
	}
	
	@Override
	public void doWork() throws OperatorException {
		// TODO: remove statement!
		if (RMProcessUtils.validProcess(getProcess().getRootOperator().getXML(true)) == false) {
			throw new OperatorException("Not a valid OpenML process. Please check the OpenML definition. Note that each operator can only be used once. ");
		}
		
		OpenmlExecutedTask oet = predictionsInput.getData(OpenmlExecutedTask.class);
		OpenmlConfigurable config;
		String apikey;
		String url;
		MacroHandler mHandler = this.getProcess().getMacroHandler();
		if (mHandler.getMacro("apikey") != null && mHandler.getMacro("url") != null) {
			apikey = mHandler.getMacro("apikey");
			url = mHandler.getMacro("url");
		} else if (this.isParameterSet("Url") && this.isParameterSet("Api key")) {
			url = this.getParameter("Url");
			apikey = this.getParameter("Api key");
		} else {
			try {
				config = (OpenmlConfigurable) ConfigurationManager.getInstance().lookup(OpenmlConfigurator.TYPE_ID,
						getParameterAsString(PARAMETER_CONFIG), getProcess().getRepositoryAccessor());
				apikey = config.getApiKey();
				url = config.getUrl();
			} catch (ConfigurationException e) {
				throw new UserError(this, e, "openml.configuration_read");
			}
		}
		openmlConnector = new OpenmlConnectorJson(url, apikey, true);
		openmlConnector.setVerboseLevel(1);
		try {
			// TODO: make the user enter his other meta-data!
			Process canonicalProcess = RMProcessUtils.processToCanonicalProcess(getProcess());
			Document processXml = RMProcessUtils.processToCanonicalXmlDocument(canonicalProcess);
			String processXmlString = RMProcessUtils.processXmlToString(processXml);
			
			Flow workflow = XMLUtils.processXmlToImpentation(canonicalProcess);
			
			int implementation_id = ImplementationUtils.getImplementationId(workflow, processXmlString, openmlConnector);

			// TODO: resolve parameter string
			Run run = XMLUtils.processXmlToRun(getProcess().getRootOperator().getXML(true), openmlConnector, implementation_id, oet.getTaskId(), TAGS); 
			for (EvaluationScore score : oet.getEvaluationMeasures()) {
				run.addOutputEvaluation(score);
			}
			
			//Conversion.log("OK","Upload Run", XMLUtils.runToXml(run));
			
			Map<String,File> files = new HashMap<String, File>();
			String taskName = "openml-task-" + oet.getTaskId() + "-predictions"; 
			// clustering task is safe as we don't need any class label anymore
			File predictionArff = Conversion.stringToTempFile(WekaTools.toWekaInstances(oet.getPredictions(), taskName , WekaInstancesAdaptor.CLUSTERING).toString(), taskName, "arff");
			files.put("predictions", predictionArff);
			
			Document runXml = XMLUtils.runToXml(run);
			
			UploadRun uploadRun = openmlConnector.runUpload(XMLUtils.DomDocumentToFile(runXml), files);
			uploadRunOutput.deliver(new OpenmlUploadRun(uploadRun));
			
		} catch(Exception e) {
			e.printStackTrace();
			throw new OperatorException("Error uploading task to Openml: " + e.getMessage() + " - " + stacktraceAsString(e));
		}
	}
	
	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeConfigurable(PARAMETER_CONFIG, "Choose an OpenML Connection", OpenmlConfigurator.TYPE_ID));
		return types;
	}
	
	private String stacktraceAsString(Exception e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		return sw.toString();
	}
}
