package org.openml.rapidminer.utils;

import java.io.File;

import org.openml.apiconnector.algorithms.Conversion;
import org.openml.apiconnector.io.HttpConnector;
import org.openml.apiconnector.io.OpenmlConnector;
import org.openml.apiconnector.xml.Implementation;
import org.openml.apiconnector.xml.ImplementationExists;
import org.openml.apiconnector.xml.UploadImplementation;

public class ImplementationUtils {
	
	public static int getImplementationId( Implementation implementation, String processXml, OpenmlConnector apiconnector ) throws Exception {
		try {
			// First ask OpenML whether this implementation already exists
			ImplementationExists result = apiconnector.implementationExists( implementation.getName(), implementation.getExternal_version() );
			if(result.exists()) return result.getId();
		} catch( Exception e ) { /* Suppress Exception since it is totally OK. */ }
		// It does not exist. Create it. 
		String xml = HttpConnector.xstreamClient.toXML( implementation );
		//System.err.println(xml);
		File implementationFile = Conversion.stringToTempFile( xml, implementation.getName(), "xml");
		File source = Conversion.stringToTempFile(processXml, implementation.getName() + "_" + implementation.getExternal_version(), "xml");
		
		UploadImplementation ui = apiconnector.implementationUpload(implementationFile, null, source);
		return ui.getId();
	}
}
