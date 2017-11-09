package org.openml.experiment.operators;

import java.util.HashMap;

import com.rapidminer.operator.Operator;

/*
 * Base SVM class
 */
public class SVM{
	
	private Operator operator;
	
	public SVM(Operator operator) throws Exception{
		
		if(operator == null) {
			throw new Exception("The operator reference given to SVM is null");
		}
		this.operator = operator;
	}

	public String getKernelType(){
		
		try{
			return operator.getParameter("kernel_type");
		}
		catch(Exception e){
			return null;
		}
	}

	public void setKernelType(String kernelType) {
		
		operator.setParameter("kernel_type", kernelType);
	}

	public String getC(){
		
		try{
			return operator.getParameter("C");
		}
		catch(Exception e){
			return null;
		}
	}

	public void setC(String c){
		
		operator.setParameter("C", c);
	}

	public String getConvEps(){
		
		try{
			return operator.getParameter("convergence_epsilon");
		}
		catch(Exception e){
			return null;
		}
	}

	public void setConvEps(String convEps){
		
		operator.setParameter("convergence_epsilon", convEps);
	}

	public String getlPos(){
		
		try{
			return operator.getParameter("L_pos");
		}
		catch(Exception e){
			return null;
		}
	}

	public void setlPos(String lPos){
		
		operator.setParameter("L_pos", lPos);
	}

	public String getlNeg(){
		
		try{
			return operator.getParameter("L_neg");
		}
		catch(Exception e){
			return null;
		}
	}

	public void setlNeg(String lNeg){
		
		operator.setParameter("L_neg", lNeg);
	}

	public String getEps(){
		
		try{
			return operator.getParameter("epsilon");
		}
		catch(Exception e){
			return null;
		}
	}

	public void setEps(String eps){
		
		operator.setParameter("epsilon", eps);
	}

	public String getEpsPlus(){
		
		try{
			return operator.getParameter("epsilon_plus");
		}
		catch(Exception e){
			return null;
		}
	}

	public void setEpsPlus(String epsPlus){
		
		operator.setParameter("epsilon_plus", epsPlus);
	}

	public String getEpsMin(){
		
		try{
			return operator.getParameter("epsilon_minus");
		}
		catch(Exception e){
			return null;
		}
	}

	public void setEpsMin(String epsMin){
		
		operator.setParameter("epsilon_minus", epsMin);
	}

	public Operator getOperator(){
		
		return operator;
	}

	public void setOperator(Operator operator){
		
		this.operator = operator;
	}
	
	public void setParameters(HashMap<String, String> parameters){
		
		parameters.forEach((k,v)-> operator.setParameter(k, v));;
	}
}