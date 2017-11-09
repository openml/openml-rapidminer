package org.openml.experiment.operators;

import java.util.HashMap;

import com.rapidminer.operator.Operator;

/*
 * Wrapper for the SVM with the polynomial kernel type
 */
public class PolynomialSVM extends SVM{
	
	public PolynomialSVM(HashMap<String, String> args, Operator operator) throws Exception{
		
		super(operator);
		if(args.size() != 9){
			throw new Exception("Not enough arguments for the PolynomialSVM");
		}
		super.setParameters(args);
	}
	
	public void setKernelDegree(String kDegree){
		
		this.getOperator().setParameter("kernel_degree", kDegree);
	}
	
	public String getKernelDegree(){
		
		try{
			return this.getOperator().getParameter("kernel_degree");
		}
		catch(Exception e){
			return null;
		}
	}
}