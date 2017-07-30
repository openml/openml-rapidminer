package org.openml.experiment.operators;

import java.util.List;

import org.openml.experiment.Logger;

import com.rapidminer.operator.Operator;

/*
 * Wrapper for the SVM with an anova kernel type
 */
public class AnovaSVM extends DotSVM
{
	public AnovaSVM(List<String> args, Operator operator)
	{
		super(args, operator);
		if(args.size() != 10)
		{
			Logger.getInstance().logToFile("Not enough arguments for the RadialSVM");
			System.exit(-1);
		}
		setKernelGamma(args.get(8));
		setKernelDegree(args.get(9));
	}
	
	public void setKernelDegree(String kDegree)
	{
		getOperator().setParameter("kernel_degree", kDegree);
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

	public void setKernelGamma(String kGamma)
	{
		this.getOperator().setParameter("kernel_gamma", kGamma);
	}
	
	public String getKernelGamma()
	{
		try
		{
			return this.getOperator().getParameter("kernel_gamma");
		}
		catch(Exception e)
		{
			return null;
		}
	}
}