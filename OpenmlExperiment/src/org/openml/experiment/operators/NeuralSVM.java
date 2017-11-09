package org.openml.experiment.operators;

import java.util.HashMap;

import com.rapidminer.operator.Operator;

/*
 * Wrapper for the SVM with the neural kernel type
 */
public class NeuralSVM extends SVM{
	
	public NeuralSVM(HashMap<String, String> args, Operator operator) throws Exception{
		
		super(operator);
		if(args.size() != 10){
			throw new Exception("Not enough arguments for the NeuralSVM");
		}
		super.setParameters(args);
	}
	
	public void setKernelA(String kA){
		
		this.getOperator().setParameter("kernel_a", kA);
	}
	
	public String getKernelA(){
		
		try{
			return this.getOperator().getParameter("kernel_a");
		}
		catch(Exception e){
			return null;
		}
	}
	public void setKernelB(String kB){
		
		this.getOperator().setParameter("kernel_b", kB);
	}
	
	public String getKernelB(){
		
		try{
			return this.getOperator().getParameter("kernel_b");
		}
		catch(Exception e){
			return null;
		}
	}
}