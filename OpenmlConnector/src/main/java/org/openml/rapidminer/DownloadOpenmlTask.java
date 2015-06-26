package org.openml.rapidminer;

import java.util.List;

import org.openml.apiconnector.algorithms.Conversion;
import org.openml.apiconnector.algorithms.TaskInformation;
import org.openml.apiconnector.io.HttpConnector;
import org.openml.apiconnector.io.OpenmlConnector;
import org.openml.apiconnector.settings.Settings;
import org.openml.apiconnector.xml.DataSetDescription;
import org.openml.apiconnector.xml.Task;
import org.openml.apiconnector.xml.Task.Input.Data_set;
import org.openml.apiconnector.xstream.XstreamXmlMapping;
import org.openml.rapidminer.models.OpenmlTask;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.ParameterTypePassword;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.plugin.Plugin;
import com.thoughtworks.xstream.core.ClassLoaderReference;

public class DownloadOpenmlTask extends Operator {

	private OpenmlConnector openmlConnector;

	private static String PARAMETER_URL = "Url";
	private static String PARAMETER_USERNAME = "Username";
	private static String PARAMETER_PASSWORD = "Password";
	private static String PARAMETER_TASKID = "Task id";
	
	private OutputPort taskOutput = getOutputPorts().createPort("task");
	
	public DownloadOpenmlTask(OperatorDescription description) {
		super(description);
		
		getTransformer().addGenerationRule(taskOutput, OpenmlTask.class);
	}
	
	@Override
	public void doWork() throws OperatorException {
		String username = "";
		String password = "";
		String url = Settings.BASE_URL;
		int task_id = -1;
		
		try { username = getParameterAsString(PARAMETER_USERNAME); } catch(UndefinedParameterError eupe ) { Conversion.log("Error","Parameter","Can't find parameter: " + PARAMETER_USERNAME ); }
		try { password = getParameterAsString(PARAMETER_PASSWORD); } catch(UndefinedParameterError eupe ) { Conversion.log("Error","Parameter","Can't find parameter: " + PARAMETER_PASSWORD );}
		try { url      = getParameterAsString(PARAMETER_URL);      } catch(UndefinedParameterError eupe ) { Conversion.log("Error","Parameter","Can't find parameter: " + PARAMETER_URL );}
		try { task_id  = getParameterAsInt(PARAMETER_TASKID);      } catch(UndefinedParameterError eupe ) { Conversion.log("Error","Parameter","Can't find parameter: " + PARAMETER_TASKID );}
		
		openmlConnector = new OpenmlConnector(url,username,password);
		HttpConnector.xstreamClient = XstreamXmlMapping.getInstance(new ClassLoaderReference(Plugin.getMajorClassLoader()));
		
		Task task = null;
		DataSetDescription dsd = null;
		
		try {
			task = openmlConnector.taskGet(task_id);
			Data_set sourceData = TaskInformation.getSourceData(task);
			dsd = openmlConnector.dataDescription(sourceData.getData_set_id());
		} catch (Exception e) {
			throw new OperatorException("Error retrieving task from Openml: " + e.getMessage());
		}

		OpenmlTask openmltask = new OpenmlTask(task,dsd);
		taskOutput.deliver(openmltask);
	}
	
	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeString(PARAMETER_URL, "OpenML Url", "http://www.openml.org/"));
		types.add(new ParameterTypeString(PARAMETER_USERNAME, "OpenML username", false));
		types.add(new ParameterTypePassword(PARAMETER_PASSWORD, "OpenML password"));
		types.add(new ParameterTypeInt(PARAMETER_TASKID, "The Openml Task that needs to be executed", 1, Integer.MAX_VALUE, false));
		return types;
	}
	
}
