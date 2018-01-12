package org.openml.experiment.utils;

import java.util.HashMap;

import org.openml.apiconnector.xml.SetupParameters;
import org.openml.apiconnector.xml.SetupParameters.Parameter;

public final class DataUtils {
	
	/**
	 * Groups the parameter names and values on a HashMap. The HashMaps are then grouped on operators in another HashMap.
	 * @param paramFile 
	 * @return
	 */
	public static HashMap<String, HashMap<String, String>> groupParameters(SetupParameters paramFile) {
		
		HashMap<String, HashMap<String, String>> parameters = new HashMap<String, HashMap<String, String>>();
		
		for(Parameter param: paramFile.getParameters()) {
			String[] mapping = param.getParameter_name().split("__");
			String operator = mapping[0];
			String paramName = mapping[1];
			String value = param.getValue();
			if(parameters.containsKey(operator)) {
				HashMap<String, String> temp = parameters.get(operator);
				temp.put(paramName, value);
				parameters.put(operator, temp);
			} else {
				HashMap<String, String> temp = new HashMap<String, String>();
				temp.put(paramName, value);
				parameters.put(operator, temp);			
			}
		}
		return parameters;
	}
}