package org.openml.rapidminer.models;

import java.util.List;

import org.openml.apiconnector.xml.EvaluationScore;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.AbstractIOObject;
import com.rapidminer.operator.Annotations;

public class OpenmlExecutedTask extends AbstractIOObject {
	
	private static final long serialVersionUID = 758961L;
	
	protected final int task_id;
	protected final ExampleSet predictions;
	protected final List<EvaluationScore> evaluationMeasures;
	
	public OpenmlExecutedTask(int task_id, ExampleSet predictions, List<EvaluationScore> evaluationMeasures) {
		this.task_id = task_id;
		this.predictions = predictions;
		this.evaluationMeasures = evaluationMeasures;
	}
	
	public int getTaskId() {
		return task_id;
	}
	
	public ExampleSet getPredictions() {
		return predictions;
	}
	
	public List<EvaluationScore> getEvaluationMeasures() {
		return evaluationMeasures;
	}
	
	@Override
	public Annotations getAnnotations() {
		// TODO Auto-generated method stub
		return null;
	}
}
