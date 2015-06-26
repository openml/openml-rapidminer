package org.openml.rapidminer.models;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.AbstractIOObject;
import com.rapidminer.operator.Annotations;

public class OpenmlExecutedTask extends AbstractIOObject {
	
	private static final long serialVersionUID = 758961L;
	
	protected final int task_id;
	protected final ExampleSet predictions;
	
	public OpenmlExecutedTask(int task_id, ExampleSet predictions) {
		this.task_id = task_id;
		this.predictions = predictions;
	}
	
	public int getTaskId() {
		return task_id;
	}
	
	public ExampleSet getPredictions() {
		return predictions;
	}
	
	@Override
	public Annotations getAnnotations() {
		// TODO Auto-generated method stub
		return null;
	}
}
