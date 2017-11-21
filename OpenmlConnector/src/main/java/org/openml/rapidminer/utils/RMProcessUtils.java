package org.openml.rapidminer.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

import com.rapidminer.Process;
import com.rapidminer.operator.Operator;

public class RMProcessUtils {
	
	public static Process processToCanonicalProcess(Process process) throws Exception {
		// this function renames all nodes to comply to OpenML naming standard. 
		
		Collection<Operator> operators = process.getAllOperators();
		List<String> usedOperatorNames = new ArrayList<>();
		for (Operator operator : operators) {
			String renamed = operator.getName().replaceAll("[^A-Za-z0-9_\\-\\.]", "");
			if (usedOperatorNames.contains(renamed)) {
				// TODO: can be fixed/avoided, but requires effort.
				throw new Exception("Double operator name. Converting to canonical OpenML name resulted in error.");
			}
			for (String parameterName : operator.getParameters()) {
				if (!parameterName.matches("[a-zA-Z0-9_\\-\\.,\\(\\):=]+")) {
					throw new Exception("Illegal parameter name: " + parameterName + " (operator: " + renamed + ")");
				}
			}
			operator.rename(renamed);
			usedOperatorNames.add(renamed);
		}
		return process;
	}
	
	public static Document processToCanonicalXmlDocument(Process process) throws Exception {
		// This function removes all fields that obfuscate the XML document with stuff that is inserted elsewhere
		// TODO: we can make a better hash representation from a process that doing this, by getting the essential graph
		final String[] removeAttributes = {"height", "width", "x", "y", "expanded"};
		final String[] removeNodes = {"parameter"};
		
		process = processToCanonicalProcess(process);

		String xml = process.getRootOperator().getXML(true);
		InputStream is = new ByteArrayInputStream(xml.getBytes());
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
		Document document = docBuilder.parse(is);

		// remove unused nodes (these values will be inserted in setup)
		for (String remove : removeNodes) {
			Element element = (Element) document.getElementsByTagName(remove).item(0);

			while (element != null) {
				element.getParentNode().removeChild(element);
				element = (Element) document.getElementsByTagName(remove).item(0);
			}
		}

		// remove unused attributes (in order to get xml string consistent)
		NodeList nodeList = document.getElementsByTagName("*");
		for (int i = nodeList.getLength() - 1; i >= 0; --i) {
			Node node = nodeList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				NamedNodeMap attributes = node.getAttributes();
				for (int j = attributes.getLength() - 1; j >= 0; --j) {
					Node attr = attributes.item(j);

					if (Arrays.asList(removeAttributes).contains(attr.getNodeName())) {
						attributes.removeNamedItem(attr.getNodeName());
					}
				}
			}
		}
		
		return document; 
	}
	
	public static String processXmlToString(Document processXml) throws ClassNotFoundException, InstantiationException, IllegalAccessException, ClassCastException {
		// generate output
		DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();

		DOMImplementationLS impl = (DOMImplementationLS) registry.getDOMImplementation("LS");

		LSSerializer writer = impl.createLSSerializer();
		writer.getDomConfig().setParameter("format-pretty-print", Boolean.TRUE);
		LSOutput output = impl.createLSOutput();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		output.setByteStream(baos);
		writer.write(processXml, output);

		return baos.toString();
	}

	// some processes do not comply to the OpenML standard.
	// this function detects them.
	@Deprecated
	public static boolean validProcess(String xml) {
		try {
			Set<String> classifiers = new HashSet<String>();

			InputStream is = new ByteArrayInputStream(xml.getBytes());
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			Document document = docBuilder.parse(is);

			// iterate over all operators
			NodeList nodes = document.getElementsByTagName("operator");
			for (int i = 0; i < nodes.getLength(); i++) {
				Element element = (Element) nodes.item(i);
				String className = element.getAttribute("class");

				if (classifiers.contains(className)) {
					return false;
				}
				classifiers.add(className);
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}
/*
	public static String processXmlToProcessName(Document xmlDocument)
			throws ParserConfigurationException, SAXException, IOException {
		List<String> classifiers = new ArrayList<String>();

		// iterate over all operators
		NodeList nodes = xmlDocument.getElementsByTagName("operator");
		for (int i = 0; i < nodes.getLength(); i++) {
			Element element = (Element) nodes.item(i);
			try {
				String className = element.getAttribute("class");
				String packageName = OperatorService.createOperator(className).getClass().getPackage().toString();

				if (packageName.contains(".learner.")) {
					classifiers.add(XMLUtils.operatorNameToOpenmlName(className, false));
				}
			} catch (OperatorCreationException e) {
				Conversion.log("Warning", "ProcessXMLToName", "Could not create operator: " + e.getMessage());
			}
		}

		if (classifiers.size() > 0) {
			return "rm.process." + StringUtils.join(classifiers, "_");
		} else {
			return "rm.NamelessRapidMinerProcess";
		}
	}
*/
	public static String toOpenmlParameterName(String operatorName, String parameterName) {
		return operatorName + "__" + parameterName;
	}
}
