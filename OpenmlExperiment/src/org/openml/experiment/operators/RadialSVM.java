package org.openml.experiment.operators;

import java.util.HashMap;

import com.rapidminer.operator.Operator;

/*
 * Wrapper for the SVM with the radial kernel type
 */
public class RadialSVM extends SVM{
	
	public RadialSVM(HashMap<String, String> args, Operator operator) throws Exception{
		
		super(operator);
		if(args.size() != 9){
			throw new Exception("Not enough arguments for the RadialSVM");
		}
		super.setParameters(args);
	}
	
	public void setKernelGamma(String kGamma){
		
		this.getOperator().setParameter("kernel_gamma", kGamma);
	}
	
	public String getKernelGamma(){
		
		try{
			return this.getOperator().getParameter("kernel_gamma");
		}
		catch(Exception e){
			return null;
		}
	}
}