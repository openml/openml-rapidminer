package org.openml.rapidminer.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.openml.apiconnector.algorithms.Conversion;
import org.w3c.dom.*;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;

import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.tools.OperatorService;

public class XMLUtils {

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
	
	public static String xmlToProcessName(String xml) throws ParserConfigurationException, SAXException, IOException {
		List<String> classifiers = new ArrayList<String>();
		
		InputStream is = new ByteArrayInputStream(xml.getBytes());
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
		Document document = docBuilder.parse(is);
		
		// iterate over all operators
		NodeList nodes = document.getElementsByTagName("operator");
	    for(int i = 0; i < nodes.getLength(); i++){
	        Element element = (Element)nodes.item(i);
			try {
				String className = element.getAttribute("class");
				String packageName = OperatorService.createOperator(className).getClass().getPackage().toString();
				
				if (packageName.contains(".learner.")) {
					classifiers.add(className.replace(" ", ""));
				}
			} catch (OperatorCreationException e) {
				Conversion.log( "Warning", "ProcessXMLToName", "Could not create operator: " + e.getMessage() );
			}
		}
		
		
		if (classifiers.size() > 0) {
			return "rm." + join(classifiers, "_");
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

}
