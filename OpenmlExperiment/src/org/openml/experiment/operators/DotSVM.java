package org.openml.experiment.operators;

import java.util.HashMap;

import com.rapidminer.operator.Operator;

/*
 * Wrapper for the SVM with the DotSVM kernel type
 */
public class DotSVM extends SVM{
	
	public DotSVM(HashMap<String, String> parameters, Operator operator) throws Exception{
		
		super(operator);
	}
}
