package org.openml.rapidminer;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openml.apiconnector.algorithms.Conversion;
import org.openml.apiconnector.algorithms.Hashing;
import org.openml.apiconnector.io.HttpConnector;
import org.openml.apiconnector.io.OpenmlConnector;
import org.openml.apiconnector.xml.Implementation;
import org.openml.apiconnector.xml.Run;
import org.openml.apiconnector.xstream.XstreamXmlMapping;
import org.openml.rapidminer.models.OpenmlConfigurable;
import org.openml.rapidminer.models.OpenmlExecutedTask;
import org.openml.rapidminer.utils.ImplementationUtils;
import org.openml.rapidminer.utils.OpenmlConfigurator;
import org.openml.rapidminer.utils.XMLUtils;

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
import com.rapidminer.tools.plugin.Plugin;
import com.thoughtworks.xstream.core.ClassLoaderReference;

public class UploadOpenmlTask extends Operator {

	private static String PARAMETER_CONFIG = "OpenML Connection";
	private static final String[] TAGS = {"RapidMiner"};
	
	private OpenmlConnector openmlConnector;

	
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
		
		try {
			config = (OpenmlConfigurable) ConfigurationManager.getInstance().lookup(
					OpenmlConfigurator.TYPE_ID, getParameterAsString(PARAMETER_CONFIG),
					getProcess().getRepositoryAccessor());
		} catch (ConfigurationException e) {
			throw new UserError(this, e, "openml.configuration_read");
		}
		
		String username = config.getUsername();
		String password = config.getPassword();
		String url = config.getUrl();
		
		openmlConnector = new OpenmlConnector(url,username,password);
		HttpConnector.xstreamClient = XstreamXmlMapping.getInstance(new ClassLoaderReference(Plugin.getMajorClassLoader()));
		
		try {
			// TODO: extract parameter settings from xml, and put them correctly in parameter string
			
			String processXml = XMLUtils.prepare( getProcess().getRootOperator().getXML(true) );
			String processName = XMLUtils.xmlToProcessName(processXml);
			// TODO: make the user enter his other meta-data!
			// TODO: workflow components in OpenMl representation.
			Implementation workflow = new Implementation(processName, Hashing.md5(processXml), "RapidMiner workflow", "English", "RapidMiner_6.4.0");
			int implementation_id = ImplementationUtils.getImplementationId(workflow, processXml, openmlConnector);
			
			// TODO: resolve parameter string
			Run run = new Run(oet.getTaskId(), null, implementation_id, "", null, TAGS);
			
			Map<String,File> files = new HashMap<String, File>();
			String taskName = "openml-task-" + oet.getTaskId() + "-predictions"; 
			// clustering task is safe as we don't need any class label anymore
			File predictionArff = Conversion.stringToTempFile(WekaTools.toWekaInstances(oet.getPredictions(), taskName , WekaInstancesAdaptor.CLUSTERING).toString(), taskName, "arff");
			files.put("predictions", predictionArff);
			
			openmlConnector.runUpload(Conversion.stringToTempFile(HttpConnector.xstreamClient.toXML(run), "RapidMinerExecutedRun", "xml"), files);
		} catch(Exception e) {
			e.printStackTrace();
			throw new OperatorException("Error uploading task to Openml: " + e.getMessage());
		}
	}
	
	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeConfigurable(PARAMETER_CONFIG, "Choose an OpenML Connection", OpenmlConfigurator.TYPE_ID));
		return types;
	}
}
