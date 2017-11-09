package org.openml.rapidminer.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.openml.apiconnector.algorithms.Conversion;
import org.openml.apiconnector.algorithms.Hashing;
import org.openml.apiconnector.xml.Flow;
import org.openml.apiconnector.xml.Flow.Parameter;
import org.openml.apiconnector.xml.Flow.Component;
import org.openml.apiconnector.xml.Run;
import org.openml.apiconnector.xml.Run.Parameter_setting;
import org.w3c.dom.*;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.tools.OperatorService;

public class XMLUtils {
	private static String illegalClassPrefix = "openmlconnector:";
	private static final String VERSION_NR = "RapidMiner_7.5.0";
	
	public static String prepare(String xml)
			throws ParserConfigurationException, SAXException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, ClassCastException {
		final String[] removeAttributes = { "height", "width", "x", "y", "expanded"};
		final String[] removeNodes = {"parameter"};
		InputStream is = new ByteArrayInputStream(xml.getBytes());
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
		Document document = docBuilder.parse(is);
		
		// remove useless nodes
		for( String remove : removeNodes ) {
			Element element = (Element) document.getElementsByTagName(remove).item(0);
			
			while (element != null) {
				element.getParentNode().removeChild(element);
				element = (Element) document.getElementsByTagName(remove).item(0);
			}
		}
		// remove useless attributes
		NodeList nodeList = document.getElementsByTagName("*");
		for (int i = nodeList.getLength() - 1; i >= 0; --i) {
			Node node = nodeList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				NamedNodeMap attributes = node.getAttributes();
				for (int j = attributes.getLength()-1; j >= 0 ; --j) {
				    Node attr = attributes.item(j);
				    
				    if (Arrays.asList(removeAttributes).contains(attr.getNodeName())) {
				    	attributes.removeNamedItem(attr.getNodeName());
				    } 
				}
			}
		}

		// generate output
		DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
		
		DOMImplementationLS impl = 
		    (DOMImplementationLS)registry.getDOMImplementation("LS");
		
		
		LSSerializer writer = impl.createLSSerializer();
		writer.getDomConfig().setParameter("format-pretty-print", Boolean.TRUE);
		LSOutput output = impl.createLSOutput();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		output.setByteStream(baos);
		writer.write(document, output);
		
		return baos.toString();
	}
	
	// some processes do not comply to the OpenML standard. 
	// this function detects them. 
	public static boolean validProcess(String xml) {
		try {
			Set<String> classifiers = new HashSet<String>();
			
			InputStream is = new ByteArrayInputStream(xml.getBytes());
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			Document document = docBuilder.parse(is);
			
			// iterate over all operators
			NodeList nodes = document.getElementsByTagName("operator");
		    for(int i = 0; i < nodes.getLength(); i++){
		        Element element = (Element)nodes.item(i);
				String className = element.getAttribute("class");
				
				if (classifiers.contains(className)) {
					return false;
				}
				classifiers.add(className);
		    }
		    return true;
	    } catch(Exception e) { return false; }
	}

	private static String xmlToProcessName(Document xmlDocument) throws ParserConfigurationException, SAXException, IOException {
		List<String> classifiers = new ArrayList<String>();
		
		// iterate over all operators
		NodeList nodes = xmlDocument.getElementsByTagName("operator");
	    for(int i = 0; i < nodes.getLength(); i++){
	        Element element = (Element)nodes.item(i);
			try {
				String className = element.getAttribute("class");
				String packageName = OperatorService.createOperator(className).getClass().getPackage().toString();
				
				if (packageName.contains(".learner.")) {
					classifiers.add(toOpenmlName(className, false));
				}
			} catch (OperatorCreationException e) {
				Conversion.log( "Warning", "ProcessXMLToName", "Could not create operator: " + e.getMessage() );
			}
		}
		
		if (classifiers.size() > 0) {
			return "rm.process." + join(classifiers, "_");
		} else {
			return "rm.NamelessRapidMinerProcess";
		}
	}
	
	private static String join(List<String> words, String delim) {
		StringBuilder sb = new StringBuilder();
		for ( String word : words ) {
			sb.append( delim + word );
		}
		return sb.toString().substring(1);
	}
	

	public static Flow xmlToImplementation(String xml)
			throws ParserConfigurationException, SAXException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, ClassCastException, NoSuchAlgorithmException, OperatorCreationException {
		InputStream is = new ByteArrayInputStream(xml.getBytes());
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
		Document document = docBuilder.parse(is);
		
		
		Flow workflow = processOperator(document.getElementsByTagName("operator").item(0), 0);
		
		workflow.setName(xmlToProcessName(document));
		workflow.setExternal_version(Hashing.md5(xml));
		workflow.setDescription("A RapidMiner Workflow. ");
		return workflow;
	}
	
	private static Flow processOperator(Node node, int depth) throws ParserConfigurationException, SAXException, IOException, OperatorCreationException {
		String operatorClass = ((Element) node).getAttribute("class");
		String operatorVersion = ((Element) node).getAttribute("compatibility");
		
		String operatorName = toOpenmlName(operatorClass, true);
		
		Flow current = new Flow(operatorName, operatorClass, operatorVersion, "A RapidMiner Operator", "English", VERSION_NR);
	    
	    NodeList nodeList = node.getChildNodes();
	    for (int i = 0; i < nodeList.getLength(); i++) {
	        Node currentNode = nodeList.item(i);
	        if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
	            //calls this method for all the children which is Element
	        	if (currentNode.getNodeName().equals("process")) {
	        		List<Flow> subworkflows = processProcess(currentNode, depth);
	        		
	        		for (Flow subworkflow : subworkflows) {
	        			current.addComponent("sub", subworkflow, false);
	        		}
	        	}
	        }
	    }
	    return current;
	}
	
	private static List<Flow> processProcess(Node node, int depth) throws ParserConfigurationException, SAXException, IOException, OperatorCreationException {
		List<Flow> subWorkflows = new ArrayList<Flow>();
		
	    NodeList nodeList = node.getChildNodes();
	    for (int i = 0; i < nodeList.getLength(); i++) {
	        Node currentNode = nodeList.item(i);
	        if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
	            //calls this method for all the children which is Element
	        	if (currentNode.getNodeName().equals("operator")) {
	        		Flow subflow = processOperator(currentNode, depth+1);
	        		
	        		// Don't add OpenML Package operators ... 
	        		String operatorClass = ((Element) currentNode).getAttribute("class");
	        		Operator operator = OperatorService.createOperator(operatorClass);
	        		if( operatorClass.startsWith(illegalClassPrefix) == false ) { 
		        		for (String parameterName : operator.getParameters()) {
		        			ParameterType type = operator.getParameterType(parameterName);
		        			
		        			subflow.addParameter(parameterName, "", type.getDefaultValueAsString(), type.getDescription());
		        		}
	        		
		        		subWorkflows.add(subflow);
	        		} else {
	        			if (subflow.getComponent() != null) {
	        				for (Component c : subflow.getComponent()) {
	        					subWorkflows.add(c.getImplementation());
	        				}
	        			}
	        		}
	        	}
	        }
	    }
	    return subWorkflows;
	}
	
	public static Run xmlToRun(String xml, OpenmlConnectorJson connector, int implementation_id, int task_id, String[] TAGS)
			throws Exception {
		
		InputStream is = new ByteArrayInputStream(xml.getBytes());
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
		Document rapidMinerProcess = docBuilder.parse(is);
		
		List<Parameter_setting> params = new ArrayList<Parameter_setting>();
		NodeList nodes = rapidMinerProcess.getElementsByTagName("parameter");
	    for(int i = 0; i < nodes.getLength(); i++){ 
	    	Element parameter = (Element) nodes.item(i); 
	    	Element operator = (Element) nodes.item(i).getParentNode();
	    	if( operator.getAttribute("class").startsWith(illegalClassPrefix) == false ) { 
		    	String flowName = toOpenmlName(operator.getAttribute("class"), true);
		    	String flowVersion = operator.getAttribute("compatibility");
		    	String paramKey = parameter.getAttribute("key");
		    	String paramValue = parameter.getAttribute("value");
		    	// TODO: we can do this in *a single* api call to implementation.get (and store all ids of sub workflows)
		    	Integer component = connector.flowExists( flowName, flowVersion ).getId(); 
		    	Parameter_setting ps = new Parameter_setting(component, paramKey, paramValue);
		    	params.add(ps);
	    	}
	    }
		Run run = new Run(task_id, null, implementation_id, null, params.toArray(new Parameter_setting[params.size()]), TAGS);
		return run;
	}
	
	private static String toOpenmlName(String rapidMinerName, boolean prefix) {
		String strPrefix = prefix ? "rm.operator." : "";
		
		String operatorName = strPrefix + Normalizer.normalize(rapidMinerName, Form.NFD);
		return operatorName.replaceAll("[^A-Za-z0-9_\\-\\.]", "");
	}
	
	// Construct the xml file from the Flow object
	public static Object flowToXml(Flow flowObject, Document document)
	{
		Document doc;
		Element rootElement = null;
		try 
		{
			// "initial call" to construct the xml file
			if(document == null)
			{
				DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
				doc = docBuilder.newDocument();
				rootElement = doc.createElement("oml:flow");
				rootElement.setAttribute("xmlns:oml", "http://openml.org/openml");
				doc.appendChild(rootElement);
			}
			// adding inner flow
			else
			{
				doc = document;
				rootElement = doc.createElement("oml:flow");
				
			}
			String flowName = flowObject.getName();
			if(checkExists(flowName))
			{
				Element name = doc.createElement("oml:name");
				name.appendChild(doc.createTextNode(flowName));
				rootElement.appendChild(name);
			}
			String flowCustomName = flowObject.getCustom_name();
			if(checkExists(flowCustomName))
			{
				Element customName = doc.createElement("oml:custom_name");
				customName.appendChild(doc.createTextNode(flowCustomName));
				rootElement.appendChild(customName);
			}
			String flowClassName = flowObject.getClass_name();
			if(checkExists(flowClassName))
			{
				Element className = doc.createElement("oml:class_name");
				className.appendChild(doc.createTextNode(flowClassName));
				rootElement.appendChild(className);
			}
			String flowExternalVersion = flowObject.getExternal_version();
			if(checkExists(flowExternalVersion))
			{
				Element externalVersion = doc.createElement("oml:external_version");
				externalVersion.appendChild(doc.createTextNode(flowExternalVersion));
				rootElement.appendChild(externalVersion);
			}
			String flowDescription = flowObject.getDescription();
			if(checkExists(flowDescription))
			{
				Element description = doc.createElement("oml:description");
				description.appendChild(doc.createTextNode(flowDescription));
				rootElement.appendChild(description);
			}
			String[] creators = flowObject.getCreator();
			if(creators != null)
			{
				for (int i=0; i< creators.length; i++)
				{
					Element creator = doc.createElement("oml:creator");
					creator.appendChild(doc.createTextNode(creators[i]));
					rootElement.appendChild(creator);
				}
			}
			String[] contributors = flowObject.getContributor();
			if(contributors != null)
			{
				for (int i=0; i< contributors.length; i++)
				{
					Element contributor = doc.createElement("oml:contributor");
					contributor.appendChild(doc.createTextNode(contributors[i]));
					rootElement.appendChild(contributor);				
				}
			}
			
			String flowLicense = flowObject.getLicence();
			if(checkExists(flowLicense))
			{
				Element license = doc.createElement("oml:licence");
				license.appendChild(doc.createTextNode(flowLicense));
				rootElement.appendChild(license);
			}
			String flowLanguage = flowObject.getLanguage();
			if(checkExists(flowLanguage))
			{
				Element language = doc.createElement("oml:language");
				language.appendChild(doc.createTextNode(flowLanguage));
				rootElement.appendChild(language);
			}
			String flowFullDescription = flowObject.getFull_description();
			if(checkExists(flowFullDescription))
			{
				Element full_description = doc.createElement("oml:full_description");
				full_description.appendChild(doc.createTextNode(flowFullDescription));
				rootElement.appendChild(full_description);
			}
			String flowInstallationNotes = flowObject.getInstallation_notes();
			if(checkExists(flowInstallationNotes))
			{
				Element installationNotes = doc.createElement("oml:installation_notes");
				installationNotes.appendChild(doc.createTextNode(flowInstallationNotes));
				rootElement.appendChild(installationNotes);
			}
			String flowDependencies = flowObject.getDependencies();
			if(checkExists(flowDependencies))
			{
				Element dependencies = doc.createElement("oml:dependencies");
				dependencies.appendChild(doc.createTextNode(flowDependencies));
				rootElement.appendChild(dependencies);
			}
			String flowImplement = flowObject.getImplement();
			if(checkExists(flowImplement))
			{
				Element implement = doc.createElement("oml:implements");
				implement.appendChild(doc.createTextNode(flowImplement));
				rootElement.appendChild(implement);
			}
			Parameter[] parameters = flowObject.getParameter();
			if(parameters != null)
			{
				for (int i=0; i< parameters.length; i++)
				{
					Parameter parameter = parameters[i];
					Element param = doc.createElement("oml:parameter");
					String pName = parameter.getName();
					if(checkExists(pName))
					{
						Element paramName = doc.createElement("oml:name");
						paramName.appendChild(doc.createTextNode(pName));
						param.appendChild(paramName);
					}
					String pDataType = parameter.getData_type();
					if(checkExists(pDataType))
					{
						Element dataType = doc.createElement("oml:data_type");
						dataType.appendChild(doc.createTextNode(pDataType));
						param.appendChild(dataType);
					}
					String pDefaultValue = parameter.getDefault_value();
					if(checkExists(pDefaultValue))
					{
						Element defaultValue = doc.createElement("oml:default_value");
						defaultValue.appendChild(doc.createTextNode(pDefaultValue));
						param.appendChild(defaultValue);
					}
					String pDescription = parameter.getDescription();
					if(checkExists(pDescription))
					{
						Element paramDescription = doc.createElement("oml:description");
						paramDescription.appendChild(doc.createTextNode(pDescription));
						param.appendChild(paramDescription);
					}
					rootElement.appendChild(param);
				}
			}
			Component[] components = flowObject.getComponent();
			if(components != null)
			{
				for (int i=0; i< components.length; i++)
				{
					Component component = components[i];
					Flow subFlow = component.getImplementation();
					Element comp = doc.createElement("oml:component");
					String cIdentifier = component.getIdentifier();
					if(checkExists(cIdentifier))
					{
						Element identifier = doc.createElement("oml:identifier");
						identifier.appendChild(doc.createTextNode(cIdentifier));
						comp.appendChild(identifier);
					}
					if(subFlow != null)
					{
						comp.appendChild((Element)flowToXml(subFlow, doc));
					}
					rootElement.appendChild(comp);
				}
			}
			String[] tags = flowObject.getTag();
			if(tags != null)
			{
				for(int i=0;i < tags.length; i++)
				{
					String tag = tags[i];
					Element tg = doc.createElement("oml:tag");
					tg.appendChild(doc.createTextNode(tag));
					rootElement.appendChild(tg);
				}
			}
			String fSourceFormat = flowObject.getSource_format();
			if(checkExists(fSourceFormat))
			{
				Element sourceFormat = doc.createElement("oml:source_format");
				sourceFormat.appendChild(doc.createTextNode(fSourceFormat));
				rootElement.appendChild(sourceFormat);
			}
			String fBinaryFormat = flowObject.getBinary_format();
			if(checkExists(fBinaryFormat))
			{
				Element binaryFormat = doc.createElement("oml:binary_format");
				binaryFormat.appendChild(doc.createTextNode(fBinaryFormat));
				rootElement.appendChild(binaryFormat);
			}
			String fSourceMd5 = flowObject.getSource_md5();
			if(checkExists(fSourceMd5))
			{
				Element sourceMd5 = doc.createElement("oml:source_md5");
				sourceMd5.appendChild(doc.createTextNode(fSourceMd5));
				rootElement.appendChild(sourceMd5);
			}
			String fBinaryMd5 = flowObject.getBinary_md5();
			if(checkExists(fBinaryMd5))
			{
				Element binaryMd5 = doc.createElement("oml:binary_md5");
				binaryMd5.appendChild(doc.createTextNode(fBinaryMd5));
				rootElement.appendChild(binaryMd5);
			}
			/*
			 *  Check if this was the first call and not a recursive one to create an inner flow.
			 *  If it is, create the document
			 */
			if(document == null)
			{
				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				Transformer transformer = transformerFactory.newTransformer();
				DOMSource source = new DOMSource(doc);
				File tmp = File.createTempFile(flowObject.getName(),".xml", null);
				StreamResult result = new StreamResult(tmp);
				transformer.transform(source, result);
				tmp.deleteOnExit();
				return tmp;
			}
		}
		catch (ParserConfigurationException e1) 
		{
			e1.printStackTrace();
		} 
		catch (TransformerConfigurationException e2) 
		{
			e2.printStackTrace();
		} 
		catch (TransformerException e3) 
		{
			e3.printStackTrace();
		} 
		catch (IOException e4) 
		{
			e4.printStackTrace();
		}
		return rootElement;
	}
	// check if a string exists and it is not empty
	private static boolean checkExists(String object)
	{
		if(object == null)
		{
			return false;
		}
		else
		{
			if(object.length() > 0)
			{
				return true;
			}
			else
			{
				return false;
			}
		}
	}
	// Construct the xml file from the Run object
	public static File runToXml(Run run) 
	{
		try
		{
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("oml:run");
			rootElement.setAttribute("xmlns:oml", "http://openml.org/openml");
			doc.appendChild(rootElement);
			String rTaskId = Integer.toString(run.getTask_id());
			if(checkExists(rTaskId))
			{
				Element runId = doc.createElement("oml:task_id");
				runId.appendChild(doc.createTextNode("" + rTaskId));
				rootElement.appendChild(runId);
			}
			String rFlowId = Integer.toString(run.getFlow_id());
			if(checkExists(rFlowId))
			{
				Element flowId = doc.createElement("oml:flow_id");
				flowId.appendChild(doc.createTextNode(rFlowId));
				rootElement.appendChild(flowId);
			}
			String rSetupString = run.getSetup_string();
			if(checkExists(rSetupString))
			{
				Element runSetupString = doc.createElement("oml:setup_string");
				runSetupString.appendChild(doc.createTextNode(rSetupString));
				rootElement.appendChild(runSetupString);
			}
			String rErrorMessage = run.getError_message();
			if(checkExists(rErrorMessage))
			{
				Element runErrorMessage = doc.createElement("oml:error_message");
				runErrorMessage.appendChild(doc.createTextNode(rErrorMessage));
				rootElement.appendChild(runErrorMessage);
			}
			Parameter_setting[] rSettingParameters = run.getParameter_settings();
			if(rSettingParameters != null)
			{
				for(int i=0; i< rSettingParameters.length; i++)
				{
					Parameter_setting parameter = rSettingParameters[i];
					Element parameterSetting = doc.createElement("oml:parameter_setting");
					String pName = parameter.getName();
					if(checkExists(pName))
					{
						Element name = doc.createElement("oml:name");
						name.appendChild(doc.createTextNode(pName));
						parameterSetting.appendChild(name);
					}
					String pValue = parameter.getValue();
					if(checkExists(pValue))
					{
						Element value = doc.createElement("oml:value");
						value.appendChild(doc.createTextNode(pValue));
						parameterSetting.appendChild(value);
					}
					String pComponent = Integer.toString(parameter.getComponent());
					if(checkExists(pComponent))
					{
						Element component = doc.createElement("oml:component");
						component.appendChild(doc.createTextNode(pComponent));
						parameterSetting.appendChild(component);
					}
					rootElement.appendChild(parameterSetting);
				}
			}
			String[] rTags = run.getTag();
			if(rTags != null)
			{
				for(int i=0; i< rTags.length; i++)
				{
					String rTag = rTags[i];
					if(checkExists(rTag))
					{
						Element tag = doc.createElement("oml:tag");
						tag.appendChild(doc.createTextNode(rTag));
						rootElement.appendChild(tag);
					}
				}
			}
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			DOMSource source = new DOMSource(doc);
			File tmp = File.createTempFile("RapidMinerExecutedRun", ".xml", null);
			StreamResult result = new StreamResult(tmp);
			transformer.transform(source, result);
			tmp.deleteOnExit();
			return tmp;
			
		}
		catch (ParserConfigurationException e1) 
		{
			e1.printStackTrace();
		}
		catch (TransformerConfigurationException e2) 
		{
			e2.printStackTrace();
		} 
		catch (TransformerException e3) 
		{
			e3.printStackTrace();
		} 
		catch (IOException e4) 
		{
			e4.printStackTrace();
		}
		return null;
	} 
}