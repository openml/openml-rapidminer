package org.openml.rapidminer.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

import org.openml.apiconnector.algorithms.Conversion;
import org.openml.apiconnector.algorithms.Hashing;
import org.openml.apiconnector.io.OpenmlConnector;
import org.openml.apiconnector.xml.Implementation;
import org.openml.apiconnector.xml.Implementation.Component;
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
	

	public static Implementation xmlToImplementation(String xml)
			throws ParserConfigurationException, SAXException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, ClassCastException, NoSuchAlgorithmException, OperatorCreationException {
		InputStream is = new ByteArrayInputStream(xml.getBytes());
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
		Document document = docBuilder.parse(is);
		
		
		Implementation workflow = processOperator(document.getElementsByTagName("operator").item(0), 0);
		
		workflow.setName(xmlToProcessName(document));
		workflow.setExternal_version(Hashing.md5(xml));
		workflow.setDescription("A RapidMiner Workflow. ");
		return workflow;
	}
	
	private static Implementation processOperator(Node node, int depth) throws ParserConfigurationException, SAXException, IOException, OperatorCreationException {
		String operatorClass = ((Element) node).getAttribute("class");
		String operatorVersion = ((Element) node).getAttribute("compatibility");
		
		String operatorName = toOpenmlName(operatorClass, true);
		
		Implementation current = new Implementation(operatorName, operatorVersion, "A RapidMiner Operator", "English", "RapidMiner_6.4.0");
	    
	    NodeList nodeList = node.getChildNodes();
	    for (int i = 0; i < nodeList.getLength(); i++) {
	        Node currentNode = nodeList.item(i);
	        if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
	            //calls this method for all the children which is Element
	        	if (currentNode.getNodeName().equals("process")) {
	        		List<Implementation> subworkflows = processProcess(currentNode, depth);
	        		
	        		for (Implementation subworkflow : subworkflows) {
	        			current.addComponent("sub", subworkflow, false);
	        		}
	        	}
	        }
	    }
	    return current;
	}
	
	private static List<Implementation> processProcess(Node node, int depth) throws ParserConfigurationException, SAXException, IOException, OperatorCreationException {
		List<Implementation> subWorkflows = new ArrayList<Implementation>();
		
	    NodeList nodeList = node.getChildNodes();
	    for (int i = 0; i < nodeList.getLength(); i++) {
	        Node currentNode = nodeList.item(i);
	        if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
	            //calls this method for all the children which is Element
	        	if (currentNode.getNodeName().equals("operator")) {
	        		Implementation subflow = processOperator(currentNode, depth+1);
	        		
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
	
	public static Run xmlToRun(String xml, OpenmlConnector connector, int implementation_id, int task_id, String[] TAGS)
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
		    	Integer component = connector.implementationExists( flowName, flowVersion ).getId(); 
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
	
}
