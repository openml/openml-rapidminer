package org.openml.experiment.operators;

import java.util.HashMap;

import com.rapidminer.operator.Operator;
/*
 * Wrapper for the SVM with the gaussian combination kernel type
 */
public class GaussianCombinationSVM extends SVM{
	
	public GaussianCombinationSVM(HashMap<String, String> args, Operator operator) throws Exception{
		
		super(operator);
		if(args.size() != 11){
			throw new Exception("Not enough arguments for the GaussianCombinationSVM");
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
	
	public void setKernelSigma2(String kSigma2){
		
		this.getOperator().setParameter("kernel_sigma2", kSigma2);
	}
	
	public String getKernelSigma2(){
		
		try{
			return this.getOperator().getParameter("kernel_sigma2");
		}
		catch(Exception e){
			return null;
		}
	}
	
	public void setKernelSigma3(String kSigma3){
		
		this.getOperator().setParameter("kernel_sigma3", kSigma3);
	}
	
	public String getKernelSigma3(){
		
		try{
			return this.getOperator().getParameter("kernel_sigma3");
		}
		catch(Exception e){
			return null;
		}
	}

}