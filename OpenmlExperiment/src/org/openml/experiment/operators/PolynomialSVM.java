package org.openml.experiment.operators;

import java.util.List;

import org.openml.experiment.Logger;

import com.rapidminer.operator.Operator;

/*
 * Wrapper for the SVM with the polynomial kernel type
 */
public class PolynomialSVM extends DotSVM
{
	public PolynomialSVM(List<String> args, Operator operator)
	{
		super(args, operator);
		if(args.size() != 9)
		{
			Logger.getInstance().logToFile("Not enough arguments for the PolynomialSVM");
			System.exit(-1);
		}
		setKernelDegree(args.get(8));
	}
	
	public void setKernelDegree(String kDegree)
	{
		this.getOperator().setParameter("kernel_degree", kDegree);
	}
	
	public String getKernelDegree()
	{
		try
		{
			return this.getOperator().getParameter("kernel_degree");
		}
		catch(Exception e)
		{
			return null;
		}
	}
}