package org.openml.experiment.operators;

import java.util.HashMap;

import com.rapidminer.operator.Operator;
/*
 * Wrapper for the SVM with the epachnenikov kernel type
 */
public class EpachnenikovSVM extends SVM{
	
	public EpachnenikovSVM(HashMap<String, String> args, Operator operator) throws Exception{
		
		super(operator);
		if(args.size() != 10){
			throw new Exception("Not enough arguments for the EpachnenikovSVM");
		}
		super.setParameters(args);
	}
	
	public void setKernelSigma1(String kSigma1){
		
		this.getOperator().setParameter("kernel_sigma1", kSigma1);
	}
	
	public String getKernelSigma1(){
		
		try{
			return this.getOperator().getParameter("kernel_sigma1");
		}
		catch(Exception e){
			return null;
		}
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