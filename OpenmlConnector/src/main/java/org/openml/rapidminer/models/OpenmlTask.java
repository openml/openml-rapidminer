package org.openml.rapidminer.models;

import org.openml.apiconnector.xml.DataSetDescription;
import org.openml.apiconnector.xml.Task;

import com.rapidminer.operator.AbstractIOObject;
import com.rapidminer.operator.Annotations;

public class OpenmlTask extends AbstractIOObject {
	
	private static final long serialVersionUID = 111L;
	protected final Task openmlTaskObject;
	protected final DataSetDescription dsd;
	
	public OpenmlTask(Task openmlTaskObject,DataSetDescription dsd) {
		this.openmlTaskObject = openmlTaskObject;
		this.dsd = dsd;
	}
	
	public Task getTask() {
		return openmlTaskObject;
	}
	
	public DataSetDescription getDatasetDescription() {
		return dsd;
	}
	
	@Override
	public Annotations getAnnotations() {
		// TODO Auto-generated method stub
		return null;
	}

}