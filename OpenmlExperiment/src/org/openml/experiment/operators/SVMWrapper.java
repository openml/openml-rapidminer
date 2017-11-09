package org.openml.experiment.operators;

import com.rapidminer.operator.Operator;

// A wrapper for the Polynominal by Binominal Classification
public class SVMWrapper {
	
	private Operator operator;
	
	public SVMWrapper(Operator operator, String strategy) throws Exception{
		
		if(operator == null){
			throw new Exception("The operator reference given to SVMWrapper is null");
		}
		this.operator = operator;
		operator.setParameter("classification_strategies", strategy);
	}
	
	public void setClassificationStrategy(String strategy){
		
		operator.setParameter("classification_strategies", strategy);
	}
	
	public String getClassificationStrategy(){
		
		try{
			return operator.getParameter("classification_strategies");
		}
		catch(Exception e){
			return null;
		}
	}
}