package org.openml.rapidminer.utils;

import java.util.ArrayList;
import java.util.List;

import org.openml.apiconnector.settings.Settings;
import org.openml.rapidminer.models.OpenmlConfigurable;

import com.rapidminer.parameter.ParameterHandler;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.tools.config.AbstractConfigurator;

public class OpenmlConfigurator extends AbstractConfigurator<OpenmlConfigurable> {

	public static final String TYPE_ID = "OpenmlConfig";
	
	public static final String PARAMETER_URL = "Url";
	public static final String PARAMETER_APIKEY = "Api key";
	
	@Override
	public Class<OpenmlConfigurable> getConfigurableClass() {
		return OpenmlConfigurable.class;
	}

	@Override
	public List<ParameterType> getParameterTypes(ParameterHandler pamHandler) {
		List<ParameterType> types = new ArrayList<ParameterType>();
		types.add(new ParameterTypeString(PARAMETER_URL, "OpenML Url", Settings.BASE_URL));
		types.add(new ParameterTypeString(PARAMETER_APIKEY, "OpenML Api key", false));
		
		return types;
	}

	@Override
	public String getTypeId() {
		return TYPE_ID;
	}

	@Override
	public String getI18NBaseKey() {
		return TYPE_ID;
	}
}
