package org.openml.rapidminer;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openml.apiconnector.algorithms.Conversion;
import org.openml.apiconnector.algorithms.Hashing;
import org.openml.apiconnector.io.HttpConnector;
import org.openml.apiconnector.io.OpenmlConnector;
import org.openml.apiconnector.settings.Settings;
import org.openml.apiconnector.xml.Implementation;
import org.openml.apiconnector.xml.Run;
import org.openml.apiconnector.xstream.XstreamXmlMapping;
import org.openml.rapidminer.models.OpenmlExecutedTask;
import org.openml.rapidminer.utils.ImplementationUtils;
import org.openml.rapidminer.utils.XMLUtils;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessRootOperator;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypePassword;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.WekaInstancesAdaptor;
import com.rapidminer.tools.WekaTools;
import com.rapidminer.tools.plugin.Plugin;
import com.thoughtworks.xstream.core.ClassLoaderReference;

public class UploadOpenmlTask extends Operator {

	private OpenmlConnector openmlConnector;

	private static final String PARAMETER_URL = "Url";
	private static final String PARAMETER_USERNAME = "Username";
	private static final String PARAMETER_PASSWORD = "Password";
	private static final String[] TAGS = {"RapidMiner"};
	
	private InputPort predictionsInput = getInputPorts().createPort("predictions",OpenmlExecutedTask.class);
	
	public UploadOpenmlTask(OperatorDescription description) {
		super(description);
	}
	
	@Override
	public void doWork() throws OperatorException {
		OpenmlExecutedTask oet = predictionsInput.getData(OpenmlExecutedTask.class);
		
		String username = "";
		String password = "";
		String url = Settings.BASE_URL;
		
		try { username = getParameterAsString(PARAMETER_USERNAME); } catch(UndefinedParameterError eupe ) { Conversion.log("Error","Parameter","Can't find parameter: " + PARAMETER_USERNAME ); }
		try { password = getParameterAsString(PARAMETER_PASSWORD); } catch(UndefinedParameterError eupe ) { Conversion.log("Error","Parameter","Can't find parameter: " + PARAMETER_PASSWORD );}
		try { url      = getParameterAsString(PARAMETER_URL);      } catch(UndefinedParameterError eupe ) { Conversion.log("Error","Parameter","Can't find parameter: " + PARAMETER_URL );}
		
		openmlConnector = new OpenmlConnector(url,username,password);
		HttpConnector.xstreamClient = XstreamXmlMapping.getInstance(new ClassLoaderReference(Plugin.getMajorClassLoader()));
		
		try {
			// TODO: do something smart with the XML, to prevent duplicates. 
			// TODO: wipe all parameter settings from xml!!! (and put them in parameter string?)
			
			//ProcessRootOperator pro = getProcess().getRootOperator();
			
			String processXml = XMLUtils.prepare( getProcess().getRootOperator().getXML(true) );
			// TODO: make the user enter his meta-data!
			Implementation workflow = new Implementation("RapidMinerWorkflow", Hashing.md5(processXml), "RapidMiner workflow", "English", "RapidMiner_6.4.0");
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
		types.add(new ParameterTypeString(PARAMETER_URL, "OpenML Url", "http://www.openml.org/"));
		types.add(new ParameterTypeString(PARAMETER_USERNAME, "OpenML username", false));
		types.add(new ParameterTypePassword(PARAMETER_PASSWORD, "OpenML password"));
		return types;
	}
}
