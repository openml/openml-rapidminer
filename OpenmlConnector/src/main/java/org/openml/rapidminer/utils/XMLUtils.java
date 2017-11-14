package org.openml.rapidminer.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.openml.apiconnector.algorithms.Hashing;
import org.openml.apiconnector.xml.Flow;
import org.openml.apiconnector.xml.Flow.Component;
import org.openml.apiconnector.xml.Flow.Parameter;
import org.openml.apiconnector.xml.Run;
import org.openml.apiconnector.xml.Run.Parameter_setting;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.rapidminer.Process;
import com.rapidminer.RapidMinerVersion;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterType;

public class XMLUtils {

	private static String illegalClassPrefix = "openmlconnector:";

	public static Flow processXmlToImpentation(Process process) throws Exception {
		// also retrieve the document. Double work, but meh..
		Document processXml = RMProcessUtils.processToCanonicalXmlDocument(process);
		String processXmlString = RMProcessUtils.processXmlToString(processXml);

		String flowName = "rm." + processToOpenmlName(process, null);
		String flowClass = null;
		String flowVersion = Hashing.md5(processXmlString);
		RapidMinerVersion version = new RapidMinerVersion();
		String dependencyStr = "RapidMiner_" + version;
		Flow flow = new Flow(flowName, flowClass, flowVersion, "A RapidMiner Flow", "English", dependencyStr);
		for (Operator operator : process.getAllOperators()) {
			String className = getClassnameOfOperator(operator);
			
			String operatorName = operator.getName();
			if (className.startsWith(illegalClassPrefix) || operator.getParent() == null) {
				// we don't want to log hyperparameters for the openml
				// connectors (identified with
				// className.startsWith(illegalClassPrefix),
				// neither for the "root operator" (identified with
				// operator.getParent() == null)
				continue;
			} else {
				for (String parameterName : operator.getParameters()) {
					ParameterType type = operator.getParameterType(parameterName);
					String openmlParamName = RMProcessUtils.toOpenmlParameterName(operatorName, parameterName);
					flow.addParameter(openmlParamName, "", type.getDefaultValueAsString(), type.getDescription());
				}
			}
		}

		return flow;
	}

	private static String processToOpenmlName(Process process, String parentToMatch) throws Exception {
		StringBuilder result = new StringBuilder();
		for (Operator operator : process.getAllOperators()) {
			// Unfortunately, process.getAllOperators gives back all operators
			// we need to match against parent to match to see if it is the
			// correct one:
			if (parentToMatch == null && operator.getParent() == null) {
				// looking for root operator
				String subname = _operatorToOpenmlName(operator);
				if (subname != null) {
					result.append("," + subname);
				}

			} else if (operator.getParent() != null && operator.getParent().getName().equals(parentToMatch)) {
				// looking for casual operator
				String subname = _operatorToOpenmlName(operator);
				if (subname != null) {
					result.append("," + subname);
				}
			}
		}

		if (result.length() > 0) {
			return result.toString().substring(1);
		} else {
			// in case we didn't find any subprocesses
			return null;
		}

	}
	
	private static String getClassnameOfOperator(Operator operator) throws Exception {
		Element process = operator.getDOMRepresentation().getDocumentElement();
		int operatorsFound = 0;
		Node node = null;
		
		for(Node child = process.getFirstChild(); child != null; child = child.getNextSibling()) {
	        if(child instanceof Element && "operator".equals(child.getNodeName())) {
	        	operatorsFound += 1;
	        	node = child;
	        }
	    }
		if (operatorsFound != 1) {
			throw new Exception("Wrong number of operators: " + operatorsFound);
		}
		String name = node.getAttributes().getNamedItem("class").getNodeValue();
		if (name == null) {
			throw new Exception("Couldn't identify classname from operator: " + operator);
		}
		return name;
	}

	private static String _operatorToOpenmlName(Operator operator) throws Exception {
		// if we have subprocesses, get their names ...
		final String operatorClassName = getClassnameOfOperator(operator);
		String operatorName = new String(operatorClassName);
		
		String subnames = processToOpenmlName(operator.getProcess(), operator.getName());
		if(subnames == null && operatorClassName.startsWith(illegalClassPrefix) == false) {
			// stays the same
		} else if (subnames != null && operatorClassName.startsWith(illegalClassPrefix) == false) {
			operatorName += "(" + subnames + ")";
		} else  if(subnames == null && operatorClassName.startsWith(illegalClassPrefix)) {
			operatorName = null;
		} else  if(subnames != null && operatorClassName.startsWith(illegalClassPrefix)) {
			operatorName = subnames;
		}
		
		return operatorName;
	}
	

	public static Run processXmlToRun(String processXml, OpenmlConnectorJson connector, int implementation_id,
			int task_id, String[] TAGS) throws Exception {

		InputStream is = new ByteArrayInputStream(processXml.getBytes());
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
		Document rapidMinerProcess = docBuilder.parse(is);

		List<Parameter_setting> params = new ArrayList<Parameter_setting>();
		NodeList nodes = rapidMinerProcess.getElementsByTagName("parameter");
		for (int i = 0; i < nodes.getLength(); i++) {
			Element parameter = (Element) nodes.item(i);
			Element operator = (Element) nodes.item(i).getParentNode();
			String operatorName = ((Element) operator).getAttribute("name");
			if (operator.getAttribute("class").startsWith(illegalClassPrefix) == false) {
				String paramKey = RMProcessUtils.toOpenmlParameterName(operatorName, parameter.getAttribute("key"));
				String paramValue = parameter.getAttribute("value");
				Parameter_setting ps = new Parameter_setting(implementation_id, paramKey, paramValue);
				params.add(ps);
			}
		}
		Run run = new Run(task_id, null, implementation_id, null, params.toArray(new Parameter_setting[params.size()]), TAGS);
		return run;
	}

	/**
	 * TODO: EVERYTHING BELOW SHOULD BE SEPARATED SOMEHOW FROM EVERYTHING ABOVE
	 **/

	public static Document flowToXml(Flow flowObject)
			throws ParserConfigurationException, IOException, TransformerException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document doc = docBuilder.newDocument();
		Element rootElement = subflowToElement(flowObject, doc);
		rootElement.setAttribute("xmlns:oml", "http://openml.org/openml");
		doc.appendChild(rootElement);

		return doc;
	}

	// Construct the xml file from the Flow object
	public static Element subflowToElement(Flow flowObject, Document doc)
			throws ParserConfigurationException, IOException, TransformerException {

		Element rootElement = doc.createElement("oml:flow");

		String flowName = flowObject.getName();
		if (checkExists(flowName)) {
			Element name = doc.createElement("oml:name");
			name.appendChild(doc.createTextNode(flowName));
			rootElement.appendChild(name);
		}
		String flowCustomName = flowObject.getCustom_name();
		if (checkExists(flowCustomName)) {
			Element customName = doc.createElement("oml:custom_name");
			customName.appendChild(doc.createTextNode(flowCustomName));
			rootElement.appendChild(customName);
		}
		String flowClassName = flowObject.getClass_name();
		if (checkExists(flowClassName)) {
			Element className = doc.createElement("oml:class_name");
			className.appendChild(doc.createTextNode(flowClassName));
			rootElement.appendChild(className);
		}
		String flowExternalVersion = flowObject.getExternal_version();
		if (checkExists(flowExternalVersion)) {
			Element externalVersion = doc.createElement("oml:external_version");
			externalVersion.appendChild(doc.createTextNode(flowExternalVersion));
			rootElement.appendChild(externalVersion);
		}
		String flowDescription = flowObject.getDescription();
		if (checkExists(flowDescription)) {
			Element description = doc.createElement("oml:description");
			description.appendChild(doc.createTextNode(flowDescription));
			rootElement.appendChild(description);
		}
		String[] creators = flowObject.getCreator();
		if (creators != null) {
			for (int i = 0; i < creators.length; i++) {
				Element creator = doc.createElement("oml:creator");
				creator.appendChild(doc.createTextNode(creators[i]));
				rootElement.appendChild(creator);
			}
		}
		String[] contributors = flowObject.getContributor();
		if (contributors != null) {
			for (int i = 0; i < contributors.length; i++) {
				Element contributor = doc.createElement("oml:contributor");
				contributor.appendChild(doc.createTextNode(contributors[i]));
				rootElement.appendChild(contributor);
			}
		}

		String flowLicense = flowObject.getLicence();
		if (checkExists(flowLicense)) {
			Element license = doc.createElement("oml:licence");
			license.appendChild(doc.createTextNode(flowLicense));
			rootElement.appendChild(license);
		}
		String flowLanguage = flowObject.getLanguage();
		if (checkExists(flowLanguage)) {
			Element language = doc.createElement("oml:language");
			language.appendChild(doc.createTextNode(flowLanguage));
			rootElement.appendChild(language);
		}
		String flowFullDescription = flowObject.getFull_description();
		if (checkExists(flowFullDescription)) {
			Element full_description = doc.createElement("oml:full_description");
			full_description.appendChild(doc.createTextNode(flowFullDescription));
			rootElement.appendChild(full_description);
		}
		String flowInstallationNotes = flowObject.getInstallation_notes();
		if (checkExists(flowInstallationNotes)) {
			Element installationNotes = doc.createElement("oml:installation_notes");
			installationNotes.appendChild(doc.createTextNode(flowInstallationNotes));
			rootElement.appendChild(installationNotes);
		}
		String flowDependencies = flowObject.getDependencies();
		if (checkExists(flowDependencies)) {
			Element dependencies = doc.createElement("oml:dependencies");
			dependencies.appendChild(doc.createTextNode(flowDependencies));
			rootElement.appendChild(dependencies);
		}
		String flowImplement = flowObject.getImplement();
		if (checkExists(flowImplement)) {
			Element implement = doc.createElement("oml:implements");
			implement.appendChild(doc.createTextNode(flowImplement));
			rootElement.appendChild(implement);
		}
		Parameter[] parameters = flowObject.getParameter();
		if (parameters != null) {
			for (int i = 0; i < parameters.length; i++) {
				Parameter parameter = parameters[i];
				Element param = doc.createElement("oml:parameter");
				String pName = parameter.getName();
				if (checkExists(pName)) {
					Element paramName = doc.createElement("oml:name");
					paramName.appendChild(doc.createTextNode(pName));
					param.appendChild(paramName);
				}
				String pDataType = parameter.getData_type();
				if (checkExists(pDataType)) {
					Element dataType = doc.createElement("oml:data_type");
					dataType.appendChild(doc.createTextNode(pDataType));
					param.appendChild(dataType);
				}
				String pDefaultValue = parameter.getDefault_value();
				if (checkExists(pDefaultValue)) {
					Element defaultValue = doc.createElement("oml:default_value");
					defaultValue.appendChild(doc.createTextNode(pDefaultValue));
					param.appendChild(defaultValue);
				}
				String pDescription = parameter.getDescription();
				if (checkExists(pDescription)) {
					Element paramDescription = doc.createElement("oml:description");
					paramDescription.appendChild(doc.createTextNode(pDescription));
					param.appendChild(paramDescription);
				}
				rootElement.appendChild(param);
			}
		}
		Component[] components = flowObject.getComponent();
		if (components != null) {
			for (int i = 0; i < components.length; i++) {
				Component component = components[i];
				Flow subFlow = component.getImplementation();
				Element comp = doc.createElement("oml:component");
				String cIdentifier = component.getIdentifier();
				if (checkExists(cIdentifier)) {
					Element identifier = doc.createElement("oml:identifier");
					identifier.appendChild(doc.createTextNode(cIdentifier));
					comp.appendChild(identifier);
				}
				if (subFlow != null) {
					comp.appendChild((Element) subflowToElement(subFlow, doc));
				}
				rootElement.appendChild(comp);
			}
		}
		String[] tags = flowObject.getTag();
		if (tags != null) {
			for (int i = 0; i < tags.length; i++) {
				String tag = tags[i];
				Element tg = doc.createElement("oml:tag");
				tg.appendChild(doc.createTextNode(tag));
				rootElement.appendChild(tg);
			}
		}
		String fSourceFormat = flowObject.getSource_format();
		if (checkExists(fSourceFormat)) {
			Element sourceFormat = doc.createElement("oml:source_format");
			sourceFormat.appendChild(doc.createTextNode(fSourceFormat));
			rootElement.appendChild(sourceFormat);
		}
		String fBinaryFormat = flowObject.getBinary_format();
		if (checkExists(fBinaryFormat)) {
			Element binaryFormat = doc.createElement("oml:binary_format");
			binaryFormat.appendChild(doc.createTextNode(fBinaryFormat));
			rootElement.appendChild(binaryFormat);
		}
		String fSourceMd5 = flowObject.getSource_md5();
		if (checkExists(fSourceMd5)) {
			Element sourceMd5 = doc.createElement("oml:source_md5");
			sourceMd5.appendChild(doc.createTextNode(fSourceMd5));
			rootElement.appendChild(sourceMd5);
		}
		String fBinaryMd5 = flowObject.getBinary_md5();
		if (checkExists(fBinaryMd5)) {
			Element binaryMd5 = doc.createElement("oml:binary_md5");
			binaryMd5.appendChild(doc.createTextNode(fBinaryMd5));
			rootElement.appendChild(binaryMd5);
		}

		return rootElement;
	}

	// Construct the xml file from the Run object
	public static Document runToXml(Run run) throws ParserConfigurationException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		Document doc = docBuilder.newDocument();
		Element rootElement = doc.createElement("oml:run");
		rootElement.setAttribute("xmlns:oml", "http://openml.org/openml");
		doc.appendChild(rootElement);
		String rTaskId = Integer.toString(run.getTask_id());
		if (checkExists(rTaskId)) {
			Element runId = doc.createElement("oml:task_id");
			runId.appendChild(doc.createTextNode("" + rTaskId));
			rootElement.appendChild(runId);
		}
		String rFlowId = Integer.toString(run.getFlow_id());
		if (checkExists(rFlowId)) {
			Element flowId = doc.createElement("oml:flow_id");
			flowId.appendChild(doc.createTextNode(rFlowId));
			rootElement.appendChild(flowId);
		}
		String rSetupString = run.getSetup_string();
		if (checkExists(rSetupString)) {
			Element runSetupString = doc.createElement("oml:setup_string");
			runSetupString.appendChild(doc.createTextNode(rSetupString));
			rootElement.appendChild(runSetupString);
		}
		String rErrorMessage = run.getError_message();
		if (checkExists(rErrorMessage)) {
			Element runErrorMessage = doc.createElement("oml:error_message");
			runErrorMessage.appendChild(doc.createTextNode(rErrorMessage));
			rootElement.appendChild(runErrorMessage);
		}
		Parameter_setting[] rSettingParameters = run.getParameter_settings();
		if (rSettingParameters != null) {
			for (int i = 0; i < rSettingParameters.length; i++) {
				Parameter_setting parameter = rSettingParameters[i];
				Element parameterSetting = doc.createElement("oml:parameter_setting");
				String pName = parameter.getName();
				if (checkExists(pName)) {
					Element name = doc.createElement("oml:name");
					name.appendChild(doc.createTextNode(pName));
					parameterSetting.appendChild(name);
				}
				String pValue = parameter.getValue();
				if (checkExists(pValue)) {
					Element value = doc.createElement("oml:value");
					value.appendChild(doc.createTextNode(pValue));
					parameterSetting.appendChild(value);
				}
				String pComponent = Integer.toString(parameter.getComponent());
				if (checkExists(pComponent)) {
					Element component = doc.createElement("oml:component");
					component.appendChild(doc.createTextNode(pComponent));
					parameterSetting.appendChild(component);
				}
				rootElement.appendChild(parameterSetting);
			}
		}
		String[] rTags = run.getTag();
		if (rTags != null) {
			for (int i = 0; i < rTags.length; i++) {
				String rTag = rTags[i];
				if (checkExists(rTag)) {
					Element tag = doc.createElement("oml:tag");
					tag.appendChild(doc.createTextNode(rTag));
					rootElement.appendChild(tag);
				}
			}
		}
		return doc;
	}

	public static File DomDocumentToFile(Document doc) throws IOException, TransformerException {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
		File tmp = File.createTempFile("RapidMinerExecutedRun", ".xml", null);
		StreamResult result = new StreamResult(tmp);
		transformer.transform(source, result);
		tmp.deleteOnExit();
		return tmp;
	}

	public static String DomDocumentToString(Document doc) throws TransformerException {
		DOMSource domSource = new DOMSource(doc);
		StringWriter writer = new StringWriter();
		StreamResult result = new StreamResult(writer);
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		transformer.transform(domSource, result);
		return writer.toString();
	}

	// check if a string exists and it is not empty
	private static boolean checkExists(String object) {
		if (object == null) {
			return false;
		} else {
			if (object.length() > 0) {
				return true;
			} else {
				return false;
			}
		}
	}
}