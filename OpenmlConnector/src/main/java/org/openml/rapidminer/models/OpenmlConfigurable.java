package org.openml.rapidminer.models;

import org.openml.rapidminer.utils.OpenmlConfigurator;

import com.rapidminer.tools.config.AbstractConfigurable;

public class OpenmlConfigurable extends AbstractConfigurable {

	private static final String TYPE_ID = OpenmlConfigurator.TYPE_ID;
	
	public String getUsername() {
		return getParameter(OpenmlConfigurator.PARAMETER_USERNAME);
	}
	public String getPassword() {
		return getParameter(OpenmlConfigurator.PARAMETER_PASSWORD);
	}
	public String getUrl() {
		return getParameter(OpenmlConfigurator.PARAMETER_URL);
	}
	
	@Override
	public String getTypeId() {
		return TYPE_ID;
	}

	
}
