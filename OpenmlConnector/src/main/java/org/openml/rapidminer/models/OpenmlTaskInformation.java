package org.openml.rapidminer.models;

import com.rapidminer.operator.AbstractIOObject;
import com.rapidminer.operator.Annotations;

public class OpenmlTaskInformation extends AbstractIOObject {

	private static final long serialVersionUID = -8248355308666055355L;
	String dataUrl = "";
	
	public OpenmlTaskInformation(String dataUrl)
	{
		this.dataUrl = dataUrl;
	}
	
	@Override
	public Annotations getAnnotations() {
		// TODO Auto-generated method stub
		return null;
	}

}
