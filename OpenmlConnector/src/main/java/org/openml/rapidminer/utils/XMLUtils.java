package org.openml.rapidminer.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.*;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;

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

}
